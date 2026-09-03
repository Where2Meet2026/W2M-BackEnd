package com.w2m.backend.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine; // Thymeleaf 템플릿 엔진 주입


    public void sendVerificationCode(String to, String code, int minutes) {
        if (to == null || to.isBlank()) return;

        try {
            // MimeMessage 객체 생성 (HTML 메일을 보내기 위해 필요)
            MimeMessage message = mailSender.createMimeMessage();
            // MimeMessageHelper를 사용하면 메일의 제목, 수신자, 내용 등을 쉽게 설정할 수 있습니다.
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 템플릿에 전달할 변수 설정
            Context context = new Context();
            context.setVariable("to", to);
            context.setVariable("verificationCode", code);
            context.setVariable("minutes", minutes);

            // Thymeleaf 템플릿 엔진으로 HTML 렌더링
            String html = templateEngine.process("MailForm", context);

            // 메일 내용 설정 (두 번째 인자를 true로 하여 HTML 형식임을 명시)
            helper.setTo(to);
            helper.setSubject("[Where2Meet] 이메일 인증코드");
            helper.setText(html, true);

            // 메일 전송
            mailSender.send(message);

        } catch (MessagingException e) {
            // 이메일 전송 실패 시 예외 처리
            // 예시: throw new EmailSendException("이메일 전송에 실패했습니다.", e);
        }
    }
}
