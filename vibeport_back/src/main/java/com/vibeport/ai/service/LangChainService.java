package com.vibeport.ai.service;

import com.vibeport.ai.mapper.AiMapper;
import com.vibeport.ai.vo.ConcertInfoVo;
import com.vibeport.mail.MailSMTP;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class LangChainService {

    private final String apiKey;
    private final AiMapper aiMapper;
    private final MailSMTP mailSMTP;


    public LangChainService(@Value("${spring.ai.gemini.api-key}") String apiKey, AiMapper aiMapper, MailSMTP mailSMTP) {
        this.apiKey = apiKey;
        this.aiMapper = aiMapper;
        this.mailSMTP = mailSMTP;
    }

    public void fetchAndNotifyNewConcerts() {
        List<ConcertInfoVo> resultList = this.getConcerInfos();

        if(!resultList.isEmpty()) {
            // 새로 추가된 콘서트 정보 DB에 저장
            this.saveConcertInfos(resultList);

            // 새로 추가된 공연의 아티스트 설명
            String artistInfo = this.getArtistInfo(resultList.getFirst().getArtistNmKor());

            // 아티스트 정보 메일 발송
            this.sendArtistInfoMail(artistInfo);
        }
    }

    private List<ConcertInfoVo> getConcerInfos() {
        List<ConcertInfoVo> resultList = new ArrayList<>();

        // -----------------------
        // 1. System 메시지 구성
        // -----------------------
        String systemPrompt = """
            당신은 내한 공연 전문 데이터 분석가입니다.
            아래 규칙을 절대적으로 준수하여 결과를 제공합니다.
           \s
            규칙:
            1) 답변 형식은 반드시 다음 순서만 사용:
               가수 / 공연일자 및 시간 / 공연장소 / 예매처 / 예매시간
            2) 아직 정해지지 않은 정보는 '미정'으로 표기
            3) 다른 설명, 해석, 불필요한 문장은 절대 포함하지 않기
            4) 응답의 key는 '가수', '공연 일자' 두 개만 사용
            5) 여러 결과가 있을 경우 줄바꿈으로 구분
            6) 가수의 인기 순으로 정렬
            7) 아무 내한 일정이 없으면 '-' 만 출력
       \s
            규칙:
            - 출력은 반드시 텍스트만 사용하며 어떤 추가 문장도 포함하지 않는다.
            - 출력 예시는 다음과 같다:
              가수명 / 2026-01-10 19:00 / 장소 / 예매처 / 예매시간
             \s
              예시:
              Weeknd / 2025-12-20 18:00 / Tokyo Dome / Ticketmaster / 2025-10-01 12:00
       \s""";
        SystemMessage systemMessage = new SystemMessage(systemPrompt);

        // -----------------------
        // 2. User 메시지 구성
        // -----------------------
        String userPrompt = """
            2026년 1월에 내한이 확정된 국적이 한국 이외의 가수 목록과 
            공연 일자, 장소, 예매처를 알려줘.
        """;
        UserMessage userMessage = new UserMessage(userPrompt);

        ChatLanguageModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-1.5-pro")
                .temperature(0.0)
                .build();

        List<ChatMessage> messages = Arrays.asList(systemMessage, userMessage);

        String response = model.generate(messages).content().text();

        System.out.println(response);


        return resultList;
    }

    private void saveConcertInfos(List<ConcertInfoVo> concertVoList) {
        concertVoList.stream().filter(data -> {
            return !data.getArtistNmKor().isEmpty();
        }).forEach(this.aiMapper::mergeConcertInfo);
    }

    private String getArtistInfo(String artistNm) {
        String systemPrompt = "너는 음악 지식에 해박한 음악 평론가야. 그리고 대중들이 알기 쉽게 아티스트에 대해서 설명해주면서 긍정적인 이야기만 해.";
        SystemMessage systemMessage = new SystemMessage(systemPrompt);

        String userPrompt = "가수 " + artistNm + "에 대해서 1,000글자 이내로 소개하고 3개의 대표곡, 뽑은 대표곡들에 대한 설명도 덧 붙여줘."
                        + "사진, 영상, 이모티콘이 포함되어 있지 않은 글자로만 응답해.";
        UserMessage userMessage = new UserMessage(userPrompt);

        List<ChatMessage> messages = Arrays.asList(systemMessage, userMessage);

        ChatLanguageModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-pro")
                .temperature(0.0)
                .build();

        return model.generate(messages).content().text();
    }

    private void sendArtistInfoMail(String artistInfo) {
        this.mailSMTP.sendArtistInfoMail(artistInfo);
    }

}
