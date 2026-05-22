package com.vibeport.ai.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibeport.ai.vo.ConcertInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class OpenAiClient {

    private static final Duration MISSING_FIELD_TIMEOUT = Duration.ofMinutes(3);
    private static final int MISSING_FIELD_MAX_OUTPUT_TOKENS = 4096;

    private final WebClient webClient;

    @Value("${openai.chat.model:gpt-4o}")
    private String model;

    @Value("${openai.responses.model:gpt-4.1}")
    private String responsesModel;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public OpenAiClient(@Qualifier("openAiWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * OpenAI Chat Completions API를 호출하여 JSON 응답을 Map으로 반환합니다.
     *
     * @param systemPrompt 시스템 프롬프트
     * @param userPrompt   사용자 프롬프트
     * @return 파싱된 JSON 응답 Map (key: subject, content 등)
     */
    public Map<String, String> generateNewsletter(String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.7,
                "max_tokens", 2000
        );

        log.info("OpenAI 뉴스레터 생성 요청 - model: {}", model);

        try {
            String responseBody = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("OpenAI 응답 수신 완료");
            log.debug("OpenAI raw response: {}", responseBody);

            Map<String, Object> response = objectMapper.readValue(responseBody,
                    new TypeReference<Map<String, Object>>() {});

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("OpenAI 응답에 choices가 없습니다.");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) {
                throw new RuntimeException("OpenAI 응답에 message가 없습니다.");
            }

            String content = (String) message.get("content");
            if (content == null || content.isBlank()) {
                throw new RuntimeException("OpenAI 응답 content가 비어있습니다.");
            }

            Map<String, String> result = objectMapper.readValue(content,
                    new TypeReference<Map<String, String>>() {});

            log.info("OpenAI 뉴스레터 생성 성공 - subject: {}", result.get("subject"));
            return result;

        } catch (Exception e) {
            log.error("OpenAI 뉴스레터 생성 실패", e);
            throw new RuntimeException("OpenAI 뉴스레터 생성 실패: " + e.getMessage(), e);
        }
    }

    public ConcertInfoVo fetchMissingConcertFields(ConcertInfoVo concert, Set<String> missingFields) {
        if (concert == null || missingFields == null || missingFields.isEmpty()) {
            return null;
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", responsesModel);
        requestBody.put("tools", List.of(Map.of("type", "web_search")));
        requestBody.put("tool_choice", "auto");
        requestBody.put("temperature", 0);
        requestBody.put("max_output_tokens", MISSING_FIELD_MAX_OUTPUT_TOKENS);
        requestBody.put("instructions", buildMissingFieldsInstructions());
        requestBody.put("input", buildMissingFieldsPrompt(concert, missingFields));

        log.info("OpenAI missing-field search request - model: {}, ArtistNmKor: {}, fields: {}",
                responsesModel, concert.getArtistNmKor(), missingFields);

        try {
            String responseBody = webClient.post()
                    .uri("/responses")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(MISSING_FIELD_TIMEOUT)
                    .block();

            log.debug("OpenAI missing-field raw response: {}", responseBody);

            Map<String, Object> response = objectMapper.readValue(responseBody,
                    new TypeReference<Map<String, Object>>() {});
            String outputText = extractOutputText(response);
            if (outputText == null || outputText.isBlank()) {
                throw new RuntimeException("OpenAI missing-field response content is empty.");
            }
            return parseConcertInfo(outputText);
        } catch (Exception e) {
            log.error("OpenAI missing-field search failed - concertNo: {}, fields: {}",
                    concert.getConcertNo(), missingFields, e);
            return null;
        }
    }

    private String buildMissingFieldsInstructions() {
        return """
                You are a Korean concert information researcher.
                Search the web and fill only requested missing fields for one concert.
                Return a single JSON object only. Do not include markdown or commentary.
                JSON keys: concertTime, venue, tctSite, tctOpenAt.
                concertTime must be HHmm, for example 1900.
                tctOpenAt should be a concise Korean or ISO-like date/time string when available.
                If a requested field cannot be verified from reliable web results, return "미정" for that field.
                For fields not requested, return the original value from the prompt.
                """;
    }

    private String buildMissingFieldsPrompt(ConcertInfoVo concert, Set<String> missingFields) {
        return """
                Missing fields: %s

                Existing concert:
                - artistNmKor: %s
                - artistNmFor: %s
                - concertYear: %d
                - concertMonth: %d
                - concertDate: %d
                - concertTime: %s
                - venue: %s
                - tctSite: %s
                - tctOpenAt: %s
                - genre: %s
                """.formatted(
                missingFields,
                valueOrUnknown(concert.getArtistNmKor()),
                valueOrUnknown(concert.getArtistNmFor()),
                concert.getConcertYear(),
                concert.getConcertMonth(),
                concert.getConcertDate(),
                valueOrUnknown(concert.getConcertTime()),
                valueOrUnknown(concert.getVenue()),
                valueOrUnknown(concert.getTctSite()),
                valueOrUnknown(concert.getTctOpenAt()),
                valueOrUnknown(concert.getGenre())
        );
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "미정" : value.trim();
    }

    @SuppressWarnings("unchecked")
    private String extractOutputText(Map<String, Object> response) {
        Object outputText = response.get("output_text");
        if (outputText instanceof String text && !text.isBlank()) {
            return text;
        }

        Object output = response.get("output");
        if (!(output instanceof List<?> outputItems)) {
            return null;
        }

        List<String> texts = new ArrayList<>();
        for (Object outputItem : outputItems) {
            if (!(outputItem instanceof Map<?, ?> item)) {
                continue;
            }
            Object content = item.get("content");
            if (!(content instanceof List<?> contentItems)) {
                continue;
            }
            for (Object contentItem : contentItems) {
                if (contentItem instanceof Map<?, ?> contentMap) {
                    Object text = contentMap.get("text");
                    if (text instanceof String value && !value.isBlank()) {
                        texts.add(value);
                    }
                }
            }
        }
        return String.join("\n", texts);
    }

    private ConcertInfoVo parseConcertInfo(String answer) throws Exception {
        String json = extractJsonPayload(answer);
        if (json.equals("{}") || json.equals("[]") || json.equals("-")) {
            return null;
        }
        return objectMapper.readValue(json, ConcertInfoVo.class);
    }

    private String extractJsonPayload(String answer) {
        String json = answer.trim();
        if (json.startsWith("```")) {
            int firstNewline = json.indexOf('\n');
            int lastBacktick = json.lastIndexOf("```");
            if (firstNewline != -1 && lastBacktick > firstNewline) {
                json = json.substring(firstNewline + 1, lastBacktick).trim();
            }
        }
        return json;
    }
}
