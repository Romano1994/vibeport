package com.vibeport.ai.service;

import com.vibeport.ai.client.GeminiClient;
import com.vibeport.ai.mapper.AiMapper;
import com.vibeport.ai.vo.ConcertInfoVo;
import com.vibeport.ai.vo.ArtistMsgVo;
import com.vibeport.mail.service.ArtistMsgMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private final GeminiClient geminiClient;
    private final AiMapper aiMapper;
    private final ArtistMsgMailService emailService;

    public void fetchAndNotifyNewConcerts() throws Exception {

        // 오늘의 년, 월
        LocalDate today = LocalDate.now();
        int beginYear = today.plusMonths(1).getYear();
        int beginMonth = today.plusMonths(1).getMonthValue();
        int endYear = today.plusMonths(3).getYear();
        int endMonth = today.plusMonths(3).getMonthValue();
        int year = 0;
        int month = 0;
        List<ConcertInfoVo> resultList = new ArrayList<>();

        Map<String, Object> paramMap = Map.of("frYear", beginYear, "frMonth", beginMonth,
                "toYear", endYear, "toMonth", endMonth);

        // 이번 달 이후의 3개월의 콘서트 정보를 가져 옴
        for(int i = 0; i < 3; i++) {
//            today = today.plusMonths(1);
//            year = today.getYear();
//            month = today.getMonthValue();
//
//            // Gemini의 API를 통해서 업데이트 된 콘서트 정보를 가져온다.
//            resultList.addAll(this.geminiClient.getConcertInfos(year, month));
        }

        // 저장된 해당 년월 콘서트 정보 가져옴
        List<ConcertInfoVo> savedConcertList = this.aiMapper.selectSavedList(paramMap);

        // 저장되지 않은 새로운 콘서트 정보
        List<ConcertInfoVo> newConcertList = this.getNewConcertList(resultList, savedConcertList);

        ConcertInfoVo firstConcert;
        if(!newConcertList.isEmpty()) {
            System.out.println("========================new===========");
            // 새로 추가된 콘서트 정보들 DB에 저장
            this.saveConcertInfos(newConcertList);

            firstConcert = newConcertList.getFirst();
        } else { // 새로 추가된 콘서트 정보가 없을 경우
            System.out.println("========================saved===========");
            // pop score의 역순으로 정렬 (이미 발송된 건 제외하고, 널 체크 포함)
            firstConcert = savedConcertList.stream()
                    .filter(data -> data.getSendYn() == null || data.getSendYn().isEmpty() || "N".equals(data.getSendYn()))
                    .sorted(Comparator.comparing(ConcertInfoVo::getPopScore).reversed())
                    .findFirst().get();
        }

//        // 이미 발송된 콘서트 정보 무작위 한 건만 - 테스트용
//        ConcertInfoVo firstConcert = this.aiMapper.selectRandomSendedInfo();

        // 공연의 아티스트 설명
        ArtistMsgVo artistMsgVo = this.geminiClient.getArtistInfo(firstConcert);

        //가수 사진 검색 (실패해도 메일 발송은 계속 진행)
        try {
            this.geminiClient.getArtistPicture(artistMsgVo);
            log.info("아티스트 이미지 설정 완료 - artist={}, url={}",
                    artistMsgVo.getArtistNmFor(), artistMsgVo.getArtistImageUrl());
        } catch (Exception e) {
            log.warn("아티스트 이미지 취득 실패 (무시하고 계속) - artist={}",
                    artistMsgVo.getArtistNmFor(), e);
        }

        // 아티스트 설명 DB저장 (artistImageUrl 컬럼 포함 - 미리보기에서 사용)
        this.aiMapper.insertArtistMsg(artistMsgVo);
        if (artistMsgVo.getArtistImageUrl() == null || artistMsgVo.getArtistImageUrl().isBlank()) {
            log.warn("아티스트 이미지 URL이 비어있어 NULL로 저장됨 - artist={}", artistMsgVo.getArtistNmFor());
        }

        // 아티스트 정보 메일 발송
        this.sendArtistInfoMail(artistMsgVo);

        // ArtistMsg 메일 전송 여부 업데이트
        this.aiMapper.updateArtistMsgSendYn(artistMsgVo);

        // ConcertInfo 메일 전송 여부 업데이트
        this.aiMapper.updateConcertInfoSendYn(artistMsgVo);
    }

    private List<ConcertInfoVo> getNewConcertList(List<ConcertInfoVo> resultList, List<ConcertInfoVo> savedConcertList) {
        List<ConcertInfoVo> newConcertList = new ArrayList<>();

        if (!resultList.isEmpty()) {
            // 한글 아티스트명 기준 키 셋
            Set<String> savedKeySetKor = new HashSet<>();
            // 영문 아티스트명 기준 키 셋
            Set<String> savedKeySetFor = new HashSet<>();

            for (ConcertInfoVo savedVo : savedConcertList) {
                savedKeySetKor.add(buildConcertKeyByKor(savedVo));
                savedKeySetFor.add(buildConcertKeyByFor(savedVo));
            }

            List<ConcertInfoVo> filteredList = new ArrayList<>();
            for (ConcertInfoVo vo : resultList) {
                // 한글명 또는 영문명 중 하나라도 매칭되면 같은 콘서트로 간주
                boolean isDuplicate = savedKeySetKor.contains(buildConcertKeyByKor(vo))
                        || savedKeySetFor.contains(buildConcertKeyByFor(vo));

                if (!isDuplicate) {
                    filteredList.add(vo);
                }
            }

            newConcertList = filteredList.stream()
                    .sorted(Comparator.comparing(ConcertInfoVo::getPopScore).reversed())
                    .toList();
        }

        return newConcertList;
    }

    private String buildConcertKeyByKor(ConcertInfoVo vo) {
        if (vo == null) {
            return "";
        }

        String artistKor = vo.getArtistNmKor() == null ? "" : vo.getArtistNmKor().trim();
        String year = Objects.toString(vo.getConcertYear(), "0");
        String month = Objects.toString(vo.getConcertMonth(), "0");
        String date = Objects.toString(vo.getConcertDate(), "0");

        return String.join("|", artistKor, year, month, date);
    }

    private String buildConcertKeyByFor(ConcertInfoVo vo) {
        if (vo == null) {
            return "";
        }

        String artistFor = vo.getArtistNmFor() == null ? "" : vo.getArtistNmFor().trim();
        String year = Objects.toString(vo.getConcertYear(), "0");
        String month = Objects.toString(vo.getConcertMonth(), "0");
        String date = Objects.toString(vo.getConcertDate(), "0");

        return String.join("|", artistFor, year, month, date);
    }

    @Transactional
    private void saveConcertInfos(List<ConcertInfoVo> concertVoList) {
        concertVoList.stream()
                .filter(data -> data.getArtistNmKor() != null && !data.getArtistNmKor().isEmpty())
                .filter(data -> data.getArtistNmFor() != null && !data.getArtistNmFor().isEmpty())
                .forEach(this.aiMapper::mergeConcertInfo);
    }

    @Transactional
    private void sendArtistInfoMail(ArtistMsgVo artistMsgVo) {
        // 이메일 발송 목록 조회
//        List<String> emailList = this.aiMapper.selectEmailList();
        List<String> emailList = List.of("sala9423@naver.com");

        // 이메일 발송
        this.emailService.artistMsgEmailSend(emailList, artistMsgVo);

        // 메일 로그 저장
        emailList.forEach(data -> {
            Map<String, Object> paramMap = Map.of("email", data
            , "subject", artistMsgVo.getSubject());

            this.aiMapper.insertMailLog(paramMap);
        });
    }
}
