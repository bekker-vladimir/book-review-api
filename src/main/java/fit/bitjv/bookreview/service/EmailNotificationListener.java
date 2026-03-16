package fit.bitjv.bookreview.service;

import fit.bitjv.bookreview.config.RabbitMqConfig;
import fit.bitjv.bookreview.model.dto.EmailMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationListener {

    @Value("${spring.mail.username}")
    private String fromEmail;
    private final JavaMailSender javaMailSender;

    @RabbitListener(queues = RabbitMqConfig.EMAIL_QUEUE)
    public void processEmailMessage(EmailMessageDto emailMessageDto) {
        log.info("Received email message for: {}", emailMessageDto.to());
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();

            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(emailMessageDto.to());
            mailMessage.setSubject(emailMessageDto.subject());
            mailMessage.setText(emailMessageDto.body());

            // Note: from field is often overwritten by SMTP server depending on config, but it's good practice
            // to set it or rely on Spring boot properties spring.mail.username.

            javaMailSender.send(mailMessage);
            log.info("Email sent successfully to: {}", emailMessageDto.to());
        } catch (Exception e) {
            log.error("Failed to send email to: {}", emailMessageDto.to(), e);
        }
    }
}
