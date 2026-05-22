package com.vibeport.ai.service;

import com.vibeport.ai.client.GeminiClient;
import com.vibeport.ai.client.OpenAiClient;
import com.vibeport.ai.vo.ArtistMsgVo;
import com.vibeport.ai.vo.ConcertInfoVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsletterService {

    private final OpenAiClient openAiClient;
    private final GeminiClient geminiClient;

    /**
     * ConcertInfoVo를 기반으로 GPT-4o 뉴스레터를 생성합니다.
     *
     * @param concert 콘서트 정보 VO
     * @return 생성된 뉴스레터 ArtistMsgVo (이미지 포함)
     */
    public ArtistMsgVo generate(ConcertInfoVo concert) throws Exception {
        log.info("뉴스레터 생성 시작 - 아티스트: {}({}), 공연일: {}-{}-{}",
                concert.getArtistNmKor(), concert.getArtistNmFor(),
                concert.getConcertYear(), concert.getConcertMonth(), concert.getConcertDate());

        // 1. 시스템 프롬프트 로드
        String systemPrompt = loadPrompt("prompts/openai-newsletter-system.txt");

        // 2. 사용자 프롬프트 구성
        String userPrompt = buildUserPrompt(concert);
        log.debug("뉴스레터 user prompt:\n{}", userPrompt);

        // 3. OpenAI로 뉴스레터 생성
        Map<String, String> result = openAiClient.generateNewsletter(systemPrompt, userPrompt);

        // 4. ArtistMsgVo 구성
        ArtistMsgVo artistMsgVo = new ArtistMsgVo();
        artistMsgVo.setArtistNmKor(concert.getArtistNmKor());
        artistMsgVo.setArtistNmFor(concert.getArtistNmFor());
        artistMsgVo.setConcertYear(concert.getConcertYear());
        artistMsgVo.setConcertMonth(concert.getConcertMonth());
        artistMsgVo.setConcertDate(concert.getConcertDate());
        artistMsgVo.setSubject(result.get("subject"));
        artistMsgVo.setContent(result.get("content"));
        artistMsgVo.setSendYn("N");

        // 5. 아티스트 이미지 검색 및 저장
        try {
            geminiClient.getArtistPicture(artistMsgVo);
            log.info("아티스트 이미지 설정 완료 - url: {}", artistMsgVo.getArtistImageUrl());
        } catch (Exception e) {
            log.warn("아티스트 이미지 취득 실패 (무시하고 계속 진행) - 아티스트: {}", concert.getArtistNmFor(), e);
        }

        log.info("뉴스레터 생성 완료 - subject: {}", artistMsgVo.getSubject());
        return artistMsgVo;
    }

    /**
     * 콘서트 정보를 자연어 user prompt 문자열로 변환합니다.
     */
    private String buildUserPrompt(ConcertInfoVo concert) {
        return "아티스트: " + concert.getArtistNmKor() + "(" + concert.getArtistNmFor() + ")\n" +
                "공연일: " + concert.getConcertYear() + "년 " + concert.getConcertMonth() + "월 " +
                concert.getConcertDate() + "일 " + concert.getConcertTime() + "\n" +
                "공연장: " + nullSafe(concert.getVenue()) + "\n" +
                "티켓 사이트: " + nullSafe(concert.getTctSite()) + "\n" +
                "티켓 오픈: " + nullSafe(concert.getTctOpenAt()) + "\n" +
                "장르: " + nullSafe(concert.getGenre()) + "\n\n" +
                "[글자수 요구사항 — 반드시 준수]\n" +
                "아티스트 소개: 250자 이상 350자 이하\n" +
                "공연 소개: 250자 이상 350자 이하\n" +
                "추천곡 감상 포인트 (각 곡): 80자 이상 120자 이하\n" +
                "끝인사: 40자 이상 60자 이하";
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    /**
     * ClassPath 리소스에서 프롬프트 파일을 읽어 String으로 반환합니다.
     */
    private String loadPrompt(String path) {
        try {
            return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("프롬프트 파일 로드 실패: {}", path, e);
            throw new RuntimeException("프롬프트 파일 로드 실패: " + path, e);
        }
    }
}
