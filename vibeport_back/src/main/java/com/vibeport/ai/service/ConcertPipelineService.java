package com.vibeport.ai.service;

import com.vibeport.ai.client.GeminiClient;
import com.vibeport.ai.client.OpenAiClient;
import com.vibeport.ai.mapper.AiMapper;
import com.vibeport.ai.vo.ArtistMsgVo;
import com.vibeport.ai.vo.ConcertInfoVo;
import com.vibeport.ai.vo.NewsletterTestRequestVo;
import com.vibeport.mail.service.ArtistMsgMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcertPipelineService {

    private static final String UNKNOWN = new String(new char[] {(char) 48120, (char) 51221});
    private static final int MAX_PARALLEL_TASKS = 5;

    private final GeminiClient geminiClient;
    private final OpenAiClient openAiClient;
    private final AiMapper aiMapper;
    private final NewsletterService newsletterService;
    private final ArtistMsgMailService artistMsgMailService;
    private final TransactionTemplate transactionTemplate;

    // -------------------------------------------------------------------------
    // 진입점
    // -------------------------------------------------------------------------

    /**
     * 일별 전체 파이프라인을 실행합니다.
     * Step 1: 전날 새로 발표된 내한 공연 수집
     * Step 2: 다음달 공연 수집
     * Step 3: 뉴스레터 생성 및 발송
     */
    public void runDailyPipeline() {
        log.info("[ConcertPipelineService] 일별 파이프라인 시작");

        boolean newsletterSentDuringCollection = collectDailyConcertsInternal();
        if (!newsletterSentDuringCollection) {
            sendDailyNewsletter();
        }

        log.info("[ConcertPipelineService] 일별 파이프라인 완료");
    }

    public void collectDailyConcerts() {
        collectDailyConcertsInternal();
    }

    public void sendDailyNewsletter() {
        generateAndSendNewsletter();
    }

    private boolean collectDailyConcertsInternal() {
        return collectNextMonthConcerts();
    }

    // -------------------------------------------------------------------------
    // 테스트: 뉴스레터 생성 및 단일 이메일 발송 (DB 업데이트 없음)
    // -------------------------------------------------------------------------

    /**
     * 테스트용 뉴스레터 생성 및 발송.
     * - req에 아티스트 정보가 있으면 해당 데이터로 생성
     * - 없으면 DB에서 미발송 공연 1건을 조회해서 사용
     * - DB 상태(sendYn 등)는 변경하지 않음
     */
    public Map<String, Object> testGenerateAndSend(NewsletterTestRequestVo req) throws Exception {
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            throw new IllegalArgumentException("email은 필수입니다.");
        }

        ConcertInfoVo concert;
        if (req.getArtistNmFor() != null && !req.getArtistNmFor().isBlank()) {
            concert = buildTestConcert(req);
            log.info("[테스트] 직접 입력 데이터로 뉴스레터 생성 - 아티스트: {}({})",
                    concert.getArtistNmKor(), concert.getArtistNmFor());
        } else {
            concert = aiMapper.selectTopPopUnsentConcert();
            if (concert == null) {
                concert = aiMapper.selectRandomSendedInfo();
            }
            if (concert == null) {
                throw new RuntimeException("테스트할 공연 데이터가 없습니다. 아티스트 정보를 직접 입력하거나 DB에 공연 데이터를 추가해 주세요.");
            }
            log.info("[테스트] DB 공연 데이터로 뉴스레터 생성 - 아티스트: {}({})",
                    concert.getArtistNmKor(), concert.getArtistNmFor());
        }

        ensureArtistImage(concert, false);

        ArtistMsgVo artistMsgVo = newsletterService.generate(concert);

        artistMsgMailService.artistMsgEmailSend(List.of(req.getEmail()), artistMsgVo);
        log.info("[테스트] 뉴스레터 발송 완료 → {}", req.getEmail());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sentTo", req.getEmail());
        result.put("artistNmKor", artistMsgVo.getArtistNmKor());
        result.put("artistNmFor", artistMsgVo.getArtistNmFor());
        result.put("subject", artistMsgVo.getSubject());
        result.put("content", artistMsgVo.getContent());
        result.put("artistImageUrl", artistMsgVo.getArtistImageUrl());
        return result;
    }

    private ConcertInfoVo buildTestConcert(NewsletterTestRequestVo req) {
        ConcertInfoVo concert = new ConcertInfoVo();
        concert.setArtistNmKor(req.getArtistNmKor() != null ? req.getArtistNmKor() : req.getArtistNmFor());
        concert.setArtistNmFor(req.getArtistNmFor());
        concert.setConcertYear(req.getConcertYear() != null ? req.getConcertYear() : 0);
        concert.setConcertMonth(req.getConcertMonth() != null ? req.getConcertMonth() : 0);
        concert.setConcertDate(req.getConcertDate() != null ? req.getConcertDate() : 0);
        concert.setConcertTime(req.getConcertTime());
        concert.setVenue(req.getVenue());
        concert.setTctSite(req.getTctSite());
        concert.setTctOpenAt(req.getTctOpenAt());
        concert.setGenre(req.getGenre());
        return concert;
    }

    // -------------------------------------------------------------------------
    // Step 1: 향후 3개월 공연 수집 (비동기 병렬)
    // -------------------------------------------------------------------------

    private boolean collectNextMonthConcerts() {
        LocalDate now = LocalDate.now();
        List<LocalDate> targets = List.of(
                now.plusMonths(1),
                now.plusMonths(2),
                now.plusMonths(3)
        );

        log.info("[ConcertPipelineService] Step2 - 향후 3개월 공연 수집 시작: {}~{}",
                targets.get(0).getYear() + "년 " + targets.get(0).getMonthValue() + "월",
                targets.get(2).getYear() + "년 " + targets.get(2).getMonthValue() + "월");

        ExecutorService executor = Executors.newFixedThreadPool(parallelPoolSize(targets.size()));
        try {
            List<CompletableFuture<List<ConcertInfoVo>>> futures = targets.stream()
                    .map(date -> CompletableFuture.supplyAsync(() -> {
                        int year = date.getYear();
                        int month = date.getMonthValue();
                        log.info("[ConcertPipelineService] Step2 - {}년 {}월 수집 시작 (thread={})",
                                year, month, Thread.currentThread().getName());
                        List<ConcertInfoVo> result = fetchNextMonthConcertsWithGemini(year, month);
                        log.info("[ConcertPipelineService] Step2 - {}년 {}월 응답 {}건", year, month, result.size());
                        return result;
                    }, executor))
                    .toList();

            List<ConcertInfoVo> allResults = futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .toList();

            log.info("[ConcertPipelineService] Step2 - 3개월 합계 {}건 수집 완료", allResults.size());


            log.info(allResults.toString());

            if (allResults.isEmpty()) {
                log.info("[ConcertPipelineService] Step2 - 수집된 공연 없음. DB 미발송 공연 뉴스레터 발송 시도.");
                return generateAndSendNewsletter();
            }

            List<ConcertInfoVo> savedList = loadSavedConcerts();
            List<ConcertInfoVo> newList = filterNew(allResults, savedList);
            log.info("[ConcertPipelineService] Step2 - 중복 제거 후 신규 {}건", newList.size());

            // '미정' 항목 보강
            newList = enrichMissingConcertFields(newList);
            newList = enrichArtistImages(newList);
            saveConcerts(newList);
            return false;
        } finally {
            executor.shutdown();
        }
    }

    private List<ConcertInfoVo> fetchNextMonthConcertsWithGemini(int year, int month) {
        try {
            return geminiClient.getConcertInfos(year, month);
        } catch (Exception e) {
            log.error("[ConcertPipelineService] Step2 - Gemini concert collection failed - year={}, month={}: {}",
                    year, month, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // -------------------------------------------------------------------------
    // Step 2: 뉴스레터 생성 및 발송
    // -------------------------------------------------------------------------

    private boolean generateAndSendNewsletter() {
        log.info("[ConcertPipelineService] Step3 - 뉴스레터 생성 및 발송 시작");

        ConcertInfoVo concert = aiMapper.selectTopPopUnsentConcert();
        if (concert == null) {
            log.info("[ConcertPipelineService] Step3 - 발송 대상 공연 없음. 스킵.");
            return false;
        }
        log.info("[ConcertPipelineService] Step3 - 대상 공연: {}({}), popScore={}",
                concert.getArtistNmKor(), concert.getArtistNmFor(), concert.getPopScore());

        ensureArtistImage(concert, true);

        ArtistMsgVo artistMsgVo;
        try {
            artistMsgVo = newsletterService.generate(concert);
        } catch (Exception e) {
            log.error("[ConcertPipelineService] Step3 - 뉴스레터 생성 실패: {}", e.getMessage(), e);
            return false;
        }

        // DB 저장 (artistImageUrl 컬럼 포함 - 미리보기에서 사용)
        aiMapper.insertArtistMsg(artistMsgVo);
        log.info("[ConcertPipelineService] Step3 - 뉴스레터 DB 저장 완료 (imageUrl={})",
                artistMsgVo.getArtistImageUrl());
        if (artistMsgVo.getArtistImageUrl() == null || artistMsgVo.getArtistImageUrl().isBlank()) {
            log.warn("[ConcertPipelineService] Step3 - 아티스트 이미지 URL 비어있음, NULL로 저장 - artist={}",
                    artistMsgVo.getArtistNmFor());
        }

        // 수신자 목록 조회
        List<String> recipients = aiMapper.selectEmailList();
//        List<String> recipients = List.of("sala9423@naver.com");
        log.info("[ConcertPipelineService] Step3 - 수신자 {}명에게 발송", recipients.size());

        if (!recipients.isEmpty()) {
            artistMsgMailService.artistMsgEmailSend(recipients, artistMsgVo);
            log.info("[ConcertPipelineService] Step3 - 이메일 발송 완료");
        }

        // 발송 여부 업데이트
        aiMapper.updateArtistMsgSendYn(artistMsgVo);
        aiMapper.updateConcertInfoSendYn(artistMsgVo);
        log.info("[ConcertPipelineService] Step3 - 발송 상태 업데이트 완료");
        return true;
    }

    // -------------------------------------------------------------------------
    // 공통 유틸
    // -------------------------------------------------------------------------

    /**
     * 향후 3개월치 저장된 공연 목록을 DB에서 조회합니다.
     */
    private List<ConcertInfoVo> loadSavedConcerts() {
        LocalDate today = LocalDate.now();
        LocalDate frDate = today.plusMonths(0);
        LocalDate toDate = today.plusMonths(3);

        Map<String, Object> paramMap = Map.of(
                "frYear", frDate.getYear(),
                "frMonth", frDate.getMonthValue(),
                "toYear", toDate.getYear(),
                "toMonth", toDate.getMonthValue()
        );
        return aiMapper.selectSavedList(paramMap);
    }

    /**
     * incoming 목록 중 saved에 없는 신규 항목만 반환합니다.
     * 키: artistNmKor + concertYear + concertMonth + concertDate (한글명 기준)
     *      OR artistNmFor + concertYear + concertMonth + concertDate (외국명 기준)
     * 둘 중 하나라도 매칭되면 중복으로 간주합니다.
     */
    private List<ConcertInfoVo> filterNew(List<ConcertInfoVo> incoming, List<ConcertInfoVo> saved) {
        if (incoming.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> savedKeySetKor = new HashSet<>();
        Set<String> savedKeySetFor = new HashSet<>();

        for (ConcertInfoVo savedVo : saved) {
            savedKeySetKor.add(buildKeyByKor(savedVo));
            savedKeySetFor.add(buildKeyByFor(savedVo));
        }

        List<ConcertInfoVo> newList = new ArrayList<>();
        for (ConcertInfoVo vo : incoming) {
            boolean isDuplicate = savedKeySetKor.contains(buildKeyByKor(vo))
                    || savedKeySetFor.contains(buildKeyByFor(vo));
            if (!isDuplicate) {
                newList.add(vo);
            }
        }

        newList.sort(Comparator.comparingInt(ConcertInfoVo::getPopScore).reversed());
        return newList;
    }

    private String buildKeyByKor(ConcertInfoVo vo) {
        if (vo == null) return "";
        String kor = vo.getArtistNmKor() == null ? "" : vo.getArtistNmKor().trim();
        return String.join("|", kor,
                Objects.toString(vo.getConcertYear(), "0"),
                Objects.toString(vo.getConcertMonth(), "0"),
                Objects.toString(vo.getConcertDate(), "0"));
    }

    private String buildKeyByFor(ConcertInfoVo vo) {
        if (vo == null) return "";
        String eng = vo.getArtistNmFor() == null ? "" : vo.getArtistNmFor().trim();
        return String.join("|", eng,
                Objects.toString(vo.getConcertYear(), "0"),
                Objects.toString(vo.getConcertMonth(), "0"),
                Objects.toString(vo.getConcertDate(), "0"));
    }

    public List<ConcertInfoVo> enrichMissingConcertFields(List<ConcertInfoVo> concerts) {
        if (concerts == null || concerts.isEmpty()) {
            return concerts == null ? new ArrayList<>() : concerts;
        }

        ExecutorService executor = Executors.newFixedThreadPool(parallelPoolSize(concerts.size()));
        try {
            List<CompletableFuture<ConcertInfoVo>> futures = concerts.stream()
                    .map(concert -> CompletableFuture.supplyAsync(() -> enrichMissingConcertFieldsWithOpenAi(concert), executor))
                    .toList();

            futures.forEach(CompletableFuture::join);
            return concerts;
        } finally {
            executor.shutdown();
        }
    }

    public Map<String, Object> enrichFutureMissingConcertFieldsWithOpenAi() {
        List<ConcertInfoVo> concerts = aiMapper.selectFutureMissingConcertInfos();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("targetCount", concerts.size());

        if (concerts.isEmpty()) {
            result.put("updatedCount", 0);
            result.put("skippedCount", 0);
            result.put("updatedConcertNos", new ArrayList<Integer>());
            result.put("skippedConcertNos", new ArrayList<Integer>());
            return result;
        }

        ExecutorService executor = Executors.newFixedThreadPool(parallelPoolSize(concerts.size()));
        List<MissingConcertEnrichmentResult> enrichmentResults;
        try {
            List<CompletableFuture<MissingConcertEnrichmentResult>> futures = concerts.stream()
                    .map(concert -> CompletableFuture.supplyAsync(() -> enrichFutureMissingConcertFieldsWithOpenAi(concert), executor))
                    .toList();

            enrichmentResults = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
        } finally {
            executor.shutdown();
        }

        int updatedCount = 0;
        int skippedCount = 0;
        List<Integer> updatedConcertNos = new ArrayList<>();
        List<Integer> skippedConcertNos = new ArrayList<>();

        for (MissingConcertEnrichmentResult enrichmentResult : enrichmentResults) {
            if (enrichmentResult.updated()) {
                updatedCount++;
                updatedConcertNos.add(enrichmentResult.concertNo());
            } else {
                skippedCount++;
                skippedConcertNos.add(enrichmentResult.concertNo());
            }
        }

        result.put("updatedCount", updatedCount);
        result.put("skippedCount", skippedCount);
        result.put("updatedConcertNos", updatedConcertNos);
        result.put("skippedConcertNos", skippedConcertNos);
        return result;
    }

    private List<ConcertInfoVo> enrichArtistImages(List<ConcertInfoVo> concerts) {
        if (concerts == null || concerts.isEmpty()) {
            return concerts == null ? new ArrayList<>() : concerts;
        }

        ExecutorService executor = Executors.newFixedThreadPool(parallelPoolSize(concerts.size()));
        try {
            List<CompletableFuture<ConcertInfoVo>> futures = concerts.stream()
                    .map(concert -> CompletableFuture.supplyAsync(() -> enrichArtistImage(concert), executor))
                    .toList();

            futures.forEach(CompletableFuture::join);
            return concerts;
        } finally {
            executor.shutdown();
        }
    }

    private ConcertInfoVo enrichArtistImage(ConcertInfoVo concert) {
        if (concert == null || concert.getArtistImageUrl() != null && !concert.getArtistImageUrl().isBlank()) {
            return concert;
        }

        try {
            String artistImageUrl = geminiClient.getArtistPictureUrl(concert.getArtistNmFor());
            concert.setArtistImageUrl(artistImageUrl);
            if (artistImageUrl == null || artistImageUrl.isBlank()) {
                log.warn("[ConcertPipelineService] Artist image not found - artist={}", concert.getArtistNmFor());
            }
        } catch (Exception e) {
            log.warn("[ConcertPipelineService] Artist image fetch failed - artist={}: {}",
                    concert.getArtistNmFor(), e.getMessage(), e);
        }
        return concert;
    }

    private void ensureArtistImage(ConcertInfoVo concert, boolean persistConcert) {
        if (concert == null || concert.getArtistImageUrl() != null && !concert.getArtistImageUrl().isBlank()) {
            return;
        }
        if (concert.getArtistNmFor() == null || concert.getArtistNmFor().isBlank()) {
            log.warn("[ConcertPipelineService] Artist image fetch skipped - artistNmFor is blank, artistNmKor={}",
                    concert.getArtistNmKor());
            return;
        }

        try {
            String artistImageUrl = geminiClient.getArtistPictureUrl(concert.getArtistNmFor());
            if (artistImageUrl == null || artistImageUrl.isBlank()) {
                log.warn("[ConcertPipelineService] Artist image not found - artist={}", concert.getArtistNmFor());
                return;
            }

            concert.setArtistImageUrl(artistImageUrl);
            if (persistConcert) {
                aiMapper.mergeConcertInfo(concert);
            }
        } catch (Exception e) {
            log.warn("[ConcertPipelineService] Artist image fetch failed - artist={}: {}",
                    concert.getArtistNmFor(), e.getMessage(), e);
        }
    }

    private ConcertInfoVo enrichMissingConcertFieldsWithOpenAi(ConcertInfoVo concert) {
        Set<String> missingFields = findMissingConcertFields(concert);
        if (missingFields.isEmpty()) {
            return concert;
        }

        normalizeMissingFields(concert, missingFields);

        try {
            ConcertInfoVo enriched = openAiClient.fetchMissingConcertFields(concert, missingFields);
            applyMissingConcertFields(concert, enriched, missingFields);
        } catch (Exception e) {
            log.error("[ConcertPipelineService] Missing concert field enrichment failed - artistNmKor={}, date={}-{}-{}, fields={}: {}",
                    concert.getArtistNmKor(),
                    concert.getConcertYear(), concert.getConcertMonth(), concert.getConcertDate(),
                    missingFields, e.getMessage(), e);
        }
        return concert;
    }

    private MissingConcertEnrichmentResult enrichFutureMissingConcertFieldsWithOpenAi(ConcertInfoVo concert) {
        Set<String> missingFields = findMissingConcertFields(concert);
        if (missingFields.isEmpty()) {
            return MissingConcertEnrichmentResult.skipped(concert.getConcertNo());
        }

        normalizeMissingFields(concert, missingFields);

        try {
            ConcertInfoVo enriched = openAiClient.fetchMissingConcertFields(concert, missingFields);
            Set<String> stillMissingBeforeApply = findMissingConcertFields(concert);
            applyMissingConcertFields(concert, enriched, missingFields);

            if (findMissingConcertFields(concert).size() < stillMissingBeforeApply.size()) {
                int affectedRows = aiMapper.updateMissingConcertFields(concert);
                if (affectedRows > 0) {
                    return MissingConcertEnrichmentResult.updated(concert.getConcertNo());
                }
            }
        } catch (Exception e) {
            log.error("[ConcertPipelineService] OpenAI missing concert field enrichment failed - concertNo={}, artistNmKor={}, date={}-{}-{}, fields={}: {}",
                    concert.getConcertNo(),
                    concert.getArtistNmKor(),
                    concert.getConcertYear(), concert.getConcertMonth(), concert.getConcertDate(),
                    missingFields, e.getMessage(), e);
        }
        return MissingConcertEnrichmentResult.skipped(concert.getConcertNo());
    }

    private record MissingConcertEnrichmentResult(int concertNo, boolean updated) {
        private static MissingConcertEnrichmentResult updated(int concertNo) {
            return new MissingConcertEnrichmentResult(concertNo, true);
        }

        private static MissingConcertEnrichmentResult skipped(int concertNo) {
            return new MissingConcertEnrichmentResult(concertNo, false);
        }
    }

    private int parallelPoolSize(int taskCount) {
        return Math.min(taskCount, MAX_PARALLEL_TASKS);
    }

    Set<String> findMissingConcertFields(ConcertInfoVo concert) {
        Set<String> missingFields = new LinkedHashSet<>();
        if (concert == null) {
            return missingFields;
        }

        if (isMissingConcertTime(concert.getConcertTime())) {
            missingFields.add("concertTime");
        }
        if (isMissingText(concert.getVenue())) {
            missingFields.add("venue");
        }
        if (isMissingText(concert.getTctSite())) {
            missingFields.add("tctSite");
        }
        if (isMissingText(concert.getTctOpenAt())) {
            missingFields.add("tctOpenAt");
        }
        return missingFields;
    }

    private void normalizeMissingFields(ConcertInfoVo concert, Set<String> missingFields) {
        if (missingFields.contains("concertTime")) {
            concert.setConcertTime(UNKNOWN);
        }
        if (missingFields.contains("venue")) {
            concert.setVenue(UNKNOWN);
        }
        if (missingFields.contains("tctSite")) {
            concert.setTctSite(UNKNOWN);
        }
        if (missingFields.contains("tctOpenAt")) {
            concert.setTctOpenAt(UNKNOWN);
        }
    }

    private void applyMissingConcertFields(ConcertInfoVo target, ConcertInfoVo enriched, Set<String> missingFields) {
        if (target == null || enriched == null || missingFields == null || missingFields.isEmpty()) {
            return;
        }

        if (missingFields.contains("concertTime") && !isMissingConcertTime(enriched.getConcertTime())) {
            target.setConcertTime(enriched.getConcertTime().trim());
        }
        if (missingFields.contains("venue") && !isMissingText(enriched.getVenue())) {
            target.setVenue(enriched.getVenue().trim());
        }
        if (missingFields.contains("tctSite") && !isMissingText(enriched.getTctSite())) {
            target.setTctSite(enriched.getTctSite().trim());
        }
        if (missingFields.contains("tctOpenAt") && !isMissingText(enriched.getTctOpenAt())) {
            target.setTctOpenAt(enriched.getTctOpenAt().trim());
        }
    }

    private boolean isMissingConcertTime(String value) {
        if (isMissingText(value)) {
            return true;
        }
        return "0000".equals(value.trim());
    }

    private boolean isMissingText(String value) {
        return value == null || value.isBlank() || UNKNOWN.equals(value.trim());
    }

    @Transactional
    public void saveConcerts(List<ConcertInfoVo> concertList) {
        if (concertList == null || concertList.isEmpty()) {
            return;
        }
        if (transactionTemplate == null) {
            doSaveConcerts(concertList);
            return;
        }
        transactionTemplate.executeWithoutResult(status -> doSaveConcerts(concertList));
    }

    private void doSaveConcerts(List<ConcertInfoVo> concertList) {
        concertList.forEach(concert -> {
            try {
                aiMapper.mergeConcertInfo(concert);
            } catch (Exception e) {
                log.error("[ConcertPipelineService] 공연 저장 실패 - artistNmKor={}, date={}-{}-{}: {}",
                        concert.getArtistNmKor(),
                        concert.getConcertYear(), concert.getConcertMonth(), concert.getConcertDate(),
                        e.getMessage(), e);
            }
        });
        log.info("[ConcertPipelineService] 공연 {}건 저장 완료", concertList.size());
    }
}
