package com.vibeport.mail.service;

import com.vibeport.mail.vo.EmailVo;
import com.vibeport.user.vo.VerifCodeVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import software.amazon.awssdk.services.ses.SesClient;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationMailService {

    @Value("${aws.ses.send-mail-from}")
    private String sender;
    private final SesClient sesClient;
    private final TemplateEngine templateEngine;

    public void verifyEmailSend(VerifCodeVo codeVo) {
        Context context = new Context();
        context.setVariable("verifyCode", codeVo.getCode());

        String content = templateEngine.process("verificationMail", context);

        EmailVo emailVo = EmailVo.builder()
                .from(sender)
                .to(codeVo.getEmailList())
                .subject("VIBEPORT - 인증 이메일입니다.")
                .content(content)
                .build();

        // 이메일 발송
        sesClient.sendEmail(emailVo.toSendEmailRequest());
    }
}
