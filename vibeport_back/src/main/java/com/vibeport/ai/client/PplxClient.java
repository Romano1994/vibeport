package com.vibeport.ai.client;

import com.vibeport.ai.vo.ConcertInfoVo;
import com.vibeport.ai.vo.ArtistMsgVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Component
public class PplxClient {
    private final OpenAiChatModel chatModel;

    public List<ConcertInfoVo> getConcertInfos(int year, int month) throws Exception{
        List<ConcertInfoVo> resultList = new ArrayList<>();

        log.info(year + "=============" + month);

        String systemPrompt = """
            ### 역할: SYSTEM
            당신은 내한 공연 전문 데이터 분석가입니다.\n
            아래 규칙을 절대적으로 준수하여 결과를 제공합니다.\n
            당신은 추론은 사용하지 않고 검색 내용을 정리하기만 합니다.\n
            \n
            참고 사이트: 나무위키의 2026년 내한\n
           \n
            규칙:\n
            0) **반드시 정확한 정보만 답변**\n
            1) 답변 형식은 반드시 다음 순서만 사용:
               아티스트 / 공연일자 및 시간 / 공연장소 / 예매처 / 예매시간 / 인기도 점수\n
            2) 아직 정해지지 않은 정보는 '미정'으로 표기\n
            3) 다른 설명, 해석, 불필요한 문장은 절대 포함하지 않기\n
            4) 응답의 key는 '아티스트', '공연 일자' 두 개만 사용\n
            5) 여러 결과가 있을 경우 줄바꿈으로 구분\n
            6) 아티스트의 인기 순으로 정렬\n
            7) 발표된 일정이 없으면 '-' 만 출력\n
            8) 유명하지 않은 가수의 내한 정보도 출력\n
            9) 가수명은 가급적 한글, 영문 병행 표기\n
            10) 공연장소, 예매처 등의 나머지 항목들을 한글 표기\n
            11) 응답 이외의 문자([1][2][3], ** 등)는 답변에 포함하지 않음\n
            12) 인기도를 0 ~ 100으로 정해줘\n
       \n
            규칙:\n
            - 출력은 반드시 텍스트만 사용하며 어떤 추가 문장도 포함하지 않는다.\n
            - 출력 예시는 다음과 같다:
              가수명 / 2026-01-10 19:00 / 장소 / 예매처 / 예매시간 / 인기도 점수
       \s""";
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemPrompt);
        Message systemMessage = systemPromptTemplate.createMessage();

        String userPrompt = "### 역할 : USER\n" +
                year + "년 " + month + "월에 내한이 확정된 가수와 공연 정보를 알려줘.";

        PromptTemplate userPromptTamplate = new PromptTemplate(userPrompt);
        Message userMessage = userPromptTamplate.createMessage();

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        Generation result = chatModel.call(prompt).getResult();
        String answer = result.getOutput().getText();

        log.info(answer);
        if(answer != null && answer.equals("-")) {
            return resultList;
        } else {
            answer = java.util.Arrays.stream(answer.split("\\R"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(java.util.stream.Collectors.joining("\n"));
        }

        // AI 응답 재가공
        resultList = this.concertInfoReprocess(answer, year, month);
        return resultList;
    }

    private List<ConcertInfoVo> concertInfoReprocess(String answer, int year, int month) throws Exception {
        List<ConcertInfoVo> resultList = new ArrayList<>();

        if (answer == null || answer.isEmpty()) {
            return resultList;
        }

        String[] arrResult = answer.split("\n");

        for (String data : arrResult) {
            if (data == null) continue;
            data = data.trim();
            if (data.isEmpty()) continue;

            String[] tmp = data.split("/");

            // 최소한 아티스트와 날짜/시간은 있어야 함
            if (tmp.length < 2) continue;

            ConcertInfoVo vo = new ConcertInfoVo();

            // 0: 아티스트명
            vo.setArtistNmKor(tmp[0].trim());

            // 1: 공연일시 yyyy-MM-dd HH:mm
            String dateTimeStr = tmp[1].trim();
            try {
                java.time.LocalDateTime dt = java.time.LocalDateTime.parse(
                        dateTimeStr,
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                );
                vo.setConcertYear(dt.getYear());
                vo.setConcertMonth(dt.getMonthValue());
                vo.setConcertDate(dt.getDayOfMonth());
                vo.setConcertTime(String.format("%02d%02d", dt.getHour(), dt.getMinute()));
            } catch (Exception e) {
                // 파싱 실패 시 호출 시점의 year/month만 백업값으로 사용
                vo.setConcertYear(year);
                vo.setConcertMonth(month);
            }

            // 2: 공연장소
            if (tmp.length > 2) {
                vo.setVenue(tmp[2].trim());
            }

            // 3: 예매처
            if (tmp.length > 3) {
                vo.setTctSite(tmp[3].trim());
            }

            // 4: 예매시간
            if (tmp.length > 4) {
                vo.setTctOpenAt(tmp[4].trim());
            }

            // 5: 인기도 점수 (0~100 사이 INT 예상)
            if (tmp.length > 5) {
                try {
                    int score = Integer.parseInt(tmp[5].trim());
                    vo.setPopScore(score);
                } catch (NumberFormatException ignore) {
                    // 파싱 실패 시 기본값(0.0) 유지
                }
            }

            vo.setEmailYn("N");

            resultList.add(vo);
        }

        return resultList;
    }

    public ArtistMsgVo getArtistInfo(String artistNm) {
        ArtistMsgVo letterVo = new ArtistMsgVo();

        StringBuffer sysSb = new StringBuffer();
        sysSb.append("너는 재치있는 20대 음악 지식에 해박한 음악 평론가야. 그리고 대중들이 알기 쉽게 아티스트와 공연에 대한 설명을 뉴스레터로 전달할거야.");
        sysSb.append("뉴스레터의 제목을 뽑고 'subject-'라고 붙여줘");
        sysSb.append("마침표를 찍을 때 마다 행을 바꿔주되 마지막에 이모티콘을 넣을 땐 마침표를 생략해.");
        sysSb.append("사진, 영상, 이모티콘이 포함되어 있지 않은 글자로만 응답해.");
        sysSb.append("출처 표시 [1][2]...는 하지마.");
        sysSb.append("이모티콘을 적절하게 사용해.");
        sysSb.append("답변은 존댓말로 해.");
        sysSb.append("최종적으로 답변이 몇 자인지는 안 알려줘도 돼.");
        SystemPromptTemplate promptTemplate = new SystemPromptTemplate(sysSb.toString());
        Message sysMessage = promptTemplate.createMessage();

        StringBuffer userSb = new StringBuffer();
        userSb.append("가수 " + artistNm + "와 새로 예정된 공연에 대해서 1,000글자 이내로 소개하고 3개의 대표곡, 뽑은 대표곡들에 대한 설명도 덧 붙여줘.");
        PromptTemplate userPromptTamplate = new PromptTemplate(userSb.toString());
        Message userMessage = userPromptTamplate.createMessage();

        Prompt prompt = new Prompt(List.of(sysMessage, userMessage));
        Generation answer = chatModel.call(prompt).getResult();
        String text = answer.getOutput().getText();

        if (text == null || text.isEmpty()) {
            return letterVo;
        }

        // 답변을 제목과 본문으로 재가공
        letterVo = this.artistMsgProcess(text);

        return letterVo;
    }

    private ArtistMsgVo artistMsgProcess(String answer) {
        ArtistMsgVo letterVo = new ArtistMsgVo();

        String subject = "";
        String content = "";
        int subjectIdx = answer.indexOf("subject-");
        if (subjectIdx != -1) {
            int lineEndIdx = answer.indexOf('\n', subjectIdx);
            if (lineEndIdx == -1) {
                lineEndIdx = answer.length();
            }

            // 'subject-' 이후부터 줄 끝까지를 제목으로 사용 (접두어는 제거)
            int titleStart = subjectIdx + "subject-".length();
            if (titleStart < lineEndIdx) {
                subject = answer.substring(titleStart, lineEndIdx).trim();
            }

            // 줄바꿈 뒤부터 끝까지를 본문으로 사용
            if (lineEndIdx < answer.length()) {
                content = answer.substring(lineEndIdx + 1).trim();
            }

            letterVo.setSubject(subject);
            letterVo.setContent(content);
        }

        return letterVo;
    }
}
