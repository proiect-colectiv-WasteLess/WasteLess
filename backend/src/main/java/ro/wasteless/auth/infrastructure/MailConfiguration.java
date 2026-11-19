package ro.wasteless.auth.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import ro.wasteless.common.port.MailerPort;

@Configuration
public class MailConfiguration {
  @Bean
  @Profile({"dev", "test"})
  MailerPort developmentMailer() {
    return (recipient, subject, text) ->
        org.slf4j.LoggerFactory.getLogger("dev.mail")
            .info(
                "Development mail delivery skipped; configure SMTP with the prod profile to deliver recovery messages");
  }

  @Bean
  @Profile("prod")
  MailerPort smtpMailer(JavaMailSender sender, @Value("${app.mail-from}") String from) {
    return (recipient, subject, text) -> {
      var message = new SimpleMailMessage();
      message.setFrom(from);
      message.setTo(recipient);
      message.setSubject(subject);
      message.setText(text);
      sender.send(message);
    };
  }
}
