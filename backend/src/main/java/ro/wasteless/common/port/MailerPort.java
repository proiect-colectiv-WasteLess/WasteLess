package ro.wasteless.common.port;

public interface MailerPort {
  void send(String recipient, String subject, String text);
}
