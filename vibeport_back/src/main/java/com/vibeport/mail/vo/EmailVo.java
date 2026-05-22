package com.vibeport.mail.vo;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import java.util.Properties;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVo {

    private String from;
    private List<String> to;
    private String subject;
    private String content;
    private String inlineImagePath;
    private String inlineImageContentId;

    @Builder
    public EmailVo(String from, List<String> to, String subject, String content, String inlineImagePath, String inlineImageContentId) {
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.content = content;
        this.inlineImagePath = inlineImagePath;
        this.inlineImageContentId = inlineImageContentId;
    }

    public SendEmailRequest toSendEmailRequest() {
        Destination destination = Destination.builder()
                .toAddresses(this.to)
                .build();

        software.amazon.awssdk.services.ses.model.Message message = software.amazon.awssdk.services.ses.model.Message.builder()
                .subject(this.createContent(this.subject))
                .body(Body.builder().html(this.createContent(this.content)).build())
                .build();

        return SendEmailRequest.builder()
                .source(this.from)
                .destination(destination)
                .message(message)
                .build();
    }

    public SendRawEmailRequest toSendRawEmailRequest() {
        try {
            Session session = Session.getInstance(new Properties());
            MimeMessage mimeMessage = new MimeMessage(session);

            mimeMessage.setFrom(new InternetAddress(this.from));
            mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(String.join(",", this.to)));
            mimeMessage.setSubject(this.subject, "UTF-8");

            MimeMultipart multipart = new MimeMultipart("related");

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(this.content, "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);

            if (this.inlineImagePath != null && !this.inlineImagePath.isBlank()
                    && this.inlineImageContentId != null && !this.inlineImageContentId.isBlank()) {
                File inlineImage = new File(this.inlineImagePath);
                if (inlineImage.exists() && inlineImage.isFile()) {
                    MimeBodyPart imagePart = new MimeBodyPart();
                    imagePart.setDataHandler(new DataHandler(new FileDataSource(inlineImage)));
                    imagePart.setHeader("Content-ID", "<" + this.inlineImageContentId + ">");
                    imagePart.setDisposition(MimeBodyPart.INLINE);
                    imagePart.setFileName(inlineImage.getName());
                    multipart.addBodyPart(imagePart);
                }
            }

            mimeMessage.setContent(multipart);
            mimeMessage.saveChanges();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            mimeMessage.writeTo(outputStream);

            RawMessage rawMessage = RawMessage.builder()
                    .data(SdkBytes.fromByteArray(outputStream.toByteArray()))
                    .build();

            return SendRawEmailRequest.builder()
                    .rawMessage(rawMessage)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create raw email request", e);
        }
    }

    private Content createContent(String content) {
        return Content.builder()
                .charset("UTF-8")
                .data(content)
                .build();
    }
}
