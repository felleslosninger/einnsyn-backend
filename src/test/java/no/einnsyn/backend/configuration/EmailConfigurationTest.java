package no.einnsyn.backend.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

class EmailConfigurationTest {

  /** The mail properties must be mapped onto the JavaMailSender bean. */
  @Test
  void testJavaMailSenderConfiguration() {
    var configuration = new EmailConfiguration();
    ReflectionTestUtils.setField(configuration, "mailServerHost", "smtp.example.com");
    ReflectionTestUtils.setField(configuration, "mailServerPort", 587);
    ReflectionTestUtils.setField(configuration, "mailServerUsername", "user");
    ReflectionTestUtils.setField(configuration, "mailServerPassword", "password");
    ReflectionTestUtils.setField(configuration, "mailServerProtocol", "smtp");
    ReflectionTestUtils.setField(configuration, "mailServerAuth", "true");
    ReflectionTestUtils.setField(configuration, "mailServerStartTls", "true");
    ReflectionTestUtils.setField(configuration, "debug", "false");

    var mailSender = (JavaMailSenderImpl) configuration.getJavaMailSender();

    assertEquals("smtp.example.com", mailSender.getHost());
    assertEquals(587, mailSender.getPort());
    assertEquals("user", mailSender.getUsername());
    assertEquals("password", mailSender.getPassword());

    var properties = mailSender.getJavaMailProperties();
    assertEquals("smtp", properties.getProperty("mail.transport.protocol"));
    assertEquals("true", properties.getProperty("mail.smtp.auth"));
    assertEquals("true", properties.getProperty("mail.smtp.starttls.enable"));
    assertEquals("false", properties.getProperty("mail.debug"));
  }
}
