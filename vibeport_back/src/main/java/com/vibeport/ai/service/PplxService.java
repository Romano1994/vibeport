package com.vibeport.ai.service;

import com.vibeport.ai.client.GeminiClient;
import com.vibeport.ai.client.PplxClient;
import com.vibeport.ai.mapper.AiMapper;
import com.vibeport.ai.vo.ConcertInfoVo;
import com.vibeport.ai.vo.ArtistMsgVo;
import com.vibeport.mail.service.TestEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PplxService {

    private final GeminiClient geminiClient;
    private final PplxClient pplxClient;
    private final AiMapper aiMapper;
    private final TestEmailService emailService;

    public void fetchAndNotifyNewConcerts() throws Exception{
        // 오늘의 년, 월
        LocalDate today = LocalDate.now();
        int beginYear = today.plusMonths(1).getYear();
        int beginMonth = today.plusMonths(1).getMonthValue();
        int year = 0;
        int month = 0;
        List<ConcertInfoVo> resultList = new ArrayList<>();

        for(int i = 0; i < 3; i++) {
            today = today.plusMonths(1);
            year = today.getYear();
            month = today.getMonthValue();

            // 퍼플렉시티 API를 통해서 해당 년월의 콘서트 정보를 가져온다.
            resultList.addAll(this.pplxClient.getConcertInfos(year, month));
        }
        Map<String, Object> paramMap = Map.of("frYear", beginYear, "frMonth", beginMonth,
                "toYear", year, "toMonth", month);

        // Gemini로 다시 한번 검증
        if(!resultList.isEmpty()) {
//            resultList = this.geminiClient.VerifySearchResult(resultList, paramMap);
        }

        log.info("final result======================");
        resultList.forEach(System.out::println);

//        // 저장된 해당 년월 콘서트 정보 가져옴
//        List<ConcertInfoVo> savedConcertList = this.aiMapper.selectSavedList(paramMap);
//
//        // 저장되지 않은 새로운 콘서트 정보
//        List<ConcertInfoVo> newConcertList = this.getNewConcertList(resultList, savedConcertList);
//        newConcertList.forEach(System.out::println);
//
//        if(!newConcertList.isEmpty()) {
//            // 새로 추가된 콘서트 정보 DB에 저장
//            this.saveConcertInfos(newConcertList);
//
//            // 새로 추가된 공연의 아티스트 설명
//            NewsLetterVo letterVo = this.pplxClient.getArtistInfo(newConcertList.getFirst().getArtistNm());
//
//            // DB에 아티스트 정보 저장
//            this.aiMapper.insertArtistMsg(letterVo);
//
//                log.info(letterVo);
//
//            // 아티스트 정보 메일 발송
//            this.sendArtistInfoMail(letterVo);
//        }
    }

    private List<ConcertInfoVo> getNewConcertList(List<ConcertInfoVo> resultList, List<ConcertInfoVo> savedConcertList) {
        List<ConcertInfoVo> newConcertList = new ArrayList<>();

        if (!resultList.isEmpty()) {
            Set<String> savedKeySet = new HashSet<>();
            for (ConcertInfoVo savedVo : savedConcertList) {
                savedKeySet.add(buildConcertKey(savedVo));
            }

            List<ConcertInfoVo> filteredList = new ArrayList<>();
            for (ConcertInfoVo vo : resultList) {
                if (!savedKeySet.contains(buildConcertKey(vo))) {
                    filteredList.add(vo);
                }
            }

            newConcertList = filteredList.stream()
                    .sorted(Comparator.comparing(ConcertInfoVo::getPopScore).reversed())
                    .toList();
        }

        return newConcertList;
    }

    private String buildConcertKey(ConcertInfoVo vo) {
        if (vo == null) {
            return "";
        }

        String artistKor = vo.getArtistNmKor() == null ? "" : vo.getArtistNmKor().trim();
        String artistFor = vo.getArtistNmFor() == null ? "" : vo.getArtistNmFor().trim();
        String year = Objects.toString(vo.getConcertYear(), "0");
        String month = Objects.toString(vo.getConcertMonth(), "0");
        String date = Objects.toString(vo.getConcertDate(), "0");

        return String.join("|", artistKor, artistFor, year, month, date);
    }

    private void saveConcertInfos(List<ConcertInfoVo> concertVoList) {
        concertVoList.stream()
                .filter(data -> data.getArtistNmKor() != null && !data.getArtistNmKor().isEmpty())
                .filter(data -> data.getArtistNmFor() != null && !data.getArtistNmFor().isEmpty())
                .forEach(this.aiMapper::mergeConcertInfo);
    }

    private void sendArtistInfoMail(ArtistMsgVo letterVo) {
        this.emailService.emailVerifSend(Arrays.asList("sala9423@naver.com"), letterVo);

        // 메일 로그 저장
//        this.aiMapper.insertMailLog(letterVo);
    }
}
