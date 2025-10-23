package no.einnsyn.backend.utils.mail;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.GsonBuilder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class MailSenderTest {

  // Capturing sender that provides a Session (needed for MailSenderService) and captures the
  // message
  static class CapturingMailSender extends JavaMailSenderImpl {
    MimeMessage captured;

    @Override
    public void send(MimeMessage mimeMessage) throws MailException {
      this.captured = mimeMessage; // prevent real sending
    }
  }

  @Test
  void testCustomMessageIdIsSet() throws Exception {
    var capturingSender = new CapturingMailSender();
    var renderer = mock(MailRendererService.class);
    when(renderer.renderFile(anyString(), any())).thenReturn("content");

    var meterRegistry = new SimpleMeterRegistry();
    var gson = new GsonBuilder().create();
    var service = new MailSenderService(capturingSender, renderer, meterRegistry, gson);

    // Inject @Value fields
    var fqdnField = MailSenderService.class.getDeclaredField("fromFqdn");
    fqdnField.setAccessible(true);
    fqdnField.set(service, "test.einnsyn.no");
    var baseUrlField = MailSenderService.class.getDeclaredField("baseUrl");
    baseUrlField.setAccessible(true);
    baseUrlField.set(service, "https://test.einnsyn.no");

    var context = new HashMap<String, Object>();
    service.send("from@example.com", "to@example.com", "confirmAnonymousOrder", "nb", context);

    assertNotNull(capturingSender.captured, "MimeMessage not captured");
    var messageId = capturingSender.captured.getMessageID();
    assertNotNull(messageId, "Message-ID missing");
    assertTrue(messageId.startsWith("<"), "Message-ID should start with <");
    assertTrue(
        messageId.endsWith("@test.einnsyn.no>"), "Message-ID should end with @custom.example>");
    assertEquals(
        messageId,
        capturingSender.captured.getHeader("Message-ID", null),
        "Message-ID header mismatch");
  }
}
