package fit.bitjv.bookreview;

import fit.bitjv.bookreview.service.EmailNotificationListener;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest
class BookReviewWebsiteApplicationTests {

    @MockBean
    RabbitTemplate rabbitTemplate;

    @MockBean
    EmailNotificationListener emailNotificationListener;

    @MockBean
    JavaMailSender javaMailSender;

    @Test
    void contextLoads() {
    }
}
