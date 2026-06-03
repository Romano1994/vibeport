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

@Service
@RequiredArgsConstructor
public class TestEmailService {
    private static final String ARTIST_IMAGE_CID = "artist-image";
    private static final DateTimeFormatter SEND_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    @Value("${aws.ses.send-mail-from}")
    private String sender;
    @Value("${newsletter.launch-date}")
    private String launchDate;
    @Value("${newsletter.send-time-text}")
    private String sendTimeText;
    private final SesClient sesClient;
    private final TemplateEngine templateEngine;
    private final ArtistImagePathResolver artistImagePathResolver;

    public void emailVerifSend(List<String> toList, ArtistMsgVo letterVo) {
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

        EmailVo emailVo = EmailVo.builder()
                .from(sender)
                .to(toList)
                .subject(letterVo.getSubject())
                .content(content)
                .inlineImagePath(inlineImagePath)
                .inlineImageContentId(ARTIST_IMAGE_CID)
                .build();

        sesClient.sendRawEmail(emailVo.toSendRawEmailRequest());
    }
}
