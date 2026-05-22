package com.vibeport.ai.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibeport.ai.vo.ConcertInfoVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PerplexityClient {

    private static final String UNKNOWN = new String(new char[] {(char) 48120, (char) 51221});

    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public List<ConcertInfoVo> fetchNextMonthConcerts(int year, int month) {
        String systemPromptText;
        try {
            systemPromptText = loadPrompt("prompts/pplx-next-month-concerts-system.txt")
                    .replace("{year}", String.valueOf(year))
                    .replace("{month}", String.valueOf(month));
        } catch (IOException e) {
            log.error("[PerplexityClient] Failed to load prompt: pplx-next-month-concerts-system.txt", e);
            return new ArrayList<>();
        }

        String userPromptText = "Find confirmed Korean concerts in " + year + "-" + month + ".";

        log.info("[PerplexityClient] fetchNextMonthConcerts request - year={}, month={}", year, month);
        return callAndParseList(systemPromptText, userPromptText);
    }

    public ConcertInfoVo fetchMissingConcertFields(ConcertInfoVo concert, Set<String> missingFields) {
        if (concert == null || missingFields == null || missingFields.isEmpty()) {
            return null;
        }

        String systemPromptText;
        try {
            systemPromptText = loadPrompt("prompts/pplx-fill-missing-concert-fields-system.txt");
        } catch (IOException e) {
            log.error("[PerplexityClient] Failed to load prompt: pplx-fill-missing-concert-fields-system.txt", e);
            return null;
        }

        String userPromptText = buildMissingFieldsPrompt(concert, missingFields);

        log.info("[PerplexityClient] fetchMissingConcertFields request - artistNmKor={}, date={}-{}-{}, fields={}",
                concert.getArtistNmKor(),
                concert.getConcertYear(), concert.getConcertMonth(), concert.getConcertDate(),
                missingFields);
        return callAndParseObject(systemPromptText, userPromptText);
    }

    private String loadPrompt(String classpath) throws IOException {
        ClassPathResource resource = new ClassPathResource(classpath);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    private String buildMissingFieldsPrompt(ConcertInfoVo concert, Set<String> missingFields) {
        return """
                Fill only the requested missing fields for this concert.

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
        return value == null || value.isBlank() ? UNKNOWN : value.trim();
    }

    private List<ConcertInfoVo> callAndParseList(String systemPromptText, String userPromptText) {
        try {
            Message systemMessage = new SystemMessage(systemPromptText);
            Message userMessage = new UserMessage(userPromptText);

            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
            String answer = chatModel.call(prompt).getResult().getOutput().getText();

            log.info("[PerplexityClient] AI response:\n{}", answer);

            return parseJsonListResponse(answer);
        } catch (Exception e) {
            log.error("[PerplexityClient] AI call failed: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private ConcertInfoVo callAndParseObject(String systemPromptText, String userPromptText) {
        try {
            Message systemMessage = new SystemMessage(systemPromptText);
            Message userMessage = new UserMessage(userPromptText);

            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
            String answer = chatModel.call(prompt).getResult().getOutput().getText();

            log.info("[PerplexityClient] AI missing-field response:\n{}", answer);

            return parseJsonObjectResponse(answer);
        } catch (Exception e) {
            log.error("[PerplexityClient] AI missing-field call failed: {}", e.getMessage(), e);
            return null;
        }
    }

    private List<ConcertInfoVo> parseJsonListResponse(String answer) {
        if (answer == null || answer.isBlank()) {
            return new ArrayList<>();
        }

        String json = extractJsonPayload(answer);
        if (json.equals("[]") || json.equals("-")) {
            return new ArrayList<>();
        }

        try {
            List<ConcertInfoVo> list = objectMapper.readValue(json, new TypeReference<>() {});
            list.forEach(vo -> {
                if (vo.getEmailYn() == null) {
                    vo.setEmailYn("N");
                }
            });
            log.info("[PerplexityClient] Parsed {} concerts", list.size());
            return list;
        } catch (Exception e) {
            log.error("[PerplexityClient] JSON list parsing failed. Raw response:\n{}", answer, e);
            return new ArrayList<>();
        }
    }

    private ConcertInfoVo parseJsonObjectResponse(String answer) {
        if (answer == null || answer.isBlank()) {
            return null;
        }

        String json = extractJsonPayload(answer);
        if (json.equals("{}") || json.equals("[]") || json.equals("-")) {
            return null;
        }

        try {
            return objectMapper.readValue(json, ConcertInfoVo.class);
        } catch (Exception e) {
            log.error("[PerplexityClient] JSON object parsing failed. Raw response:\n{}", answer, e);
            return null;
        }
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
