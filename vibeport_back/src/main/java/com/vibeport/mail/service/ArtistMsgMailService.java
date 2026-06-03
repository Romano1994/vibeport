package com.vibeport.mail.service;

import com.vibeport.ai.vo.ArtistMsgVo;
import com.vibeport.mail.vo.EmailVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import software.amazon.awssdk.services.ses.SesClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class ArtistMsgMailService {
    private static final String ARTIST_IMAGE_CID = "artist-image";
    private static final DateTimeFormatter SEND_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final int MAX_PARALLEL_TASKS = 5;

    @Value("${aws.ses.send-mail-from}")
    private String sender;
    @Value("${newsletter.launch-date}")
    private String launchDate;
    @Value("${newsletter.send-time-text}")
    private String sendTimeText;
    private final SesClient sesClient;
    private final TemplateEngine templateEngine;
    private final ArtistImagePathResolver artistImagePathResolver;

    public void artistMsgEmailSend(List<String> toList, ArtistMsgVo letterVo) {
        String inlineImagePath = artistImagePathResolver.resolveInlineImagePath(letterVo);
        Context context = new Context();
        String concertDateText = "";
        if (letterVo.getConcertYear() > 0 && letterVo.getConcertMonth() > 0 && letterVo.getConcertDate() > 0) {
            concertDateText = String.format("%d.%02d.%02d",
                    letterVo.getConcertYear(),
                    letterVo.getConcertMonth(),
                    letterVo.getConcertDate());
        }

        LocalDate today = LocalDate.now();
        String sendDateText = today.format(SEND_DATE_FORMATTER);
        long volNumber = ChronoUnit.DAYS.between(LocalDate.parse(launchDate), today) + 1;
        if (volNumber < 1) volNumber = 1;

        context.setVariable("artistName", letterVo.getArtistNmFor() != null && !letterVo.getArtistNmFor().isBlank()
                ? letterVo.getArtistNmFor()
                : letterVo.getArtistNmKor());
        context.setVariable("artistNameKor", letterVo.getArtistNmFor() != null && !letterVo.getArtistNmFor().isBlank()
                ? letterVo.getArtistNmKor()
                : null);
        context.setVariable("concertDateText", concertDateText);
        context.setVariable("sendDateText", sendDateText);
        context.setVariable("sendTimeText", sendTimeText);
        context.setVariable("volNumber", volNumber);
        context.setVariable("artistInfo", letterVo.getContent());
        context.setVariable("artistImageCid", inlineImagePath == null ? null : ARTIST_IMAGE_CID);

        String content = templateEngine.process("vibeportNewsletter", context);

        if (toList == null || toList.isEmpty()) {
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(toList.size(), MAX_PARALLEL_TASKS));
        try {
            List<CompletableFuture<Void>> futures = toList.stream()
                    .map(to -> CompletableFuture.runAsync(() -> sendArtistMsgEmail(to, letterVo, content, inlineImagePath), executor))
                    .toList();

            futures.forEach(CompletableFuture::join);
        } finally {
            executor.shutdown();
        }
    }

    private void sendArtistMsgEmail(String to, ArtistMsgVo letterVo, String content, String inlineImagePath) {
        EmailVo emailVo = EmailVo.builder()
                .from(sender)
                .to(List.of(to))
                .subject(letterVo.getSubject())
                .content(content)
                .inlineImagePath(inlineImagePath)
                .inlineImageContentId(ARTIST_IMAGE_CID)
                .build();

        sesClient.sendRawEmail(emailVo.toSendRawEmailRequest());
    }

    public void sendOnboardingEmail(Map<String, Object> param) {
        Context context = new Context();
        context.setVariable("message",
                "구독해 주셔서 감사합니다! <br/>" +
                        "매일 아침, 당신을 위한 음악 소식이 발송됩니다. <br/>" +
                        "VibePort가 엄선한 내한 예정 아티스트의 소식과 콘서트 정보를 놓치지 마세요.");

        String content = templateEngine.process("onboardingMail", context);
        String recipientEmail = (String) param.get("email");

        EmailVo emailVo = EmailVo.builder()
                .from(sender)
                .to(List.of(recipientEmail))
                .subject("환영합니다! VibePort 뉴스레터 구독이 시작되었습니다.")
                .content(content)
                .build();

        sesClient.sendEmail(emailVo.toSendEmailRequest());
    }
}
