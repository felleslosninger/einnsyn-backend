package no.einnsyn.backend.utils.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.GsonBuilder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class MailSenderTest {

  @Test
  void testCustomMessageIdIsSet() throws Exception {
    var javaMailSender = mock(JavaMailSenderImpl.class);
    var renderer = mock(MailRendererService.class);
    when(renderer.renderFile(anyString(), any())).thenReturn("content");

    var meterRegistry = new SimpleMeterRegistry();
    var gson = new GsonBuilder().create();
    System.err.println(javaMailSender);
    var service = new MailSenderService(javaMailSender, renderer, meterRegistry, gson);

    // Inject @Value fields
    var fqdnField = MailSenderService.class.getDeclaredField("fromFqdn");
    fqdnField.setAccessible(true);
    fqdnField.set(service, "test.einnsyn.no");
    var baseUrlField = MailSenderService.class.getDeclaredField("baseUrl");
    baseUrlField.setAccessible(true);
    baseUrlField.set(service, "https://test.einnsyn.no");

    var context = new HashMap<String, Object>();
    service.send("from@example.com", "to@example.com", "confirmAnonymousOrder", "nb", context);

    var mimeMessageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(javaMailSender, times(1)).send(mimeMessageCaptor.capture());
    var mimeMessage = mimeMessageCaptor.getValue();

    assertNotNull(mimeMessage, "MimeMessage not captured");
    var messageId = mimeMessage.getMessageID();
    assertNotNull(messageId, "Message-ID missing");
    assertTrue(messageId.startsWith("<"), "Message-ID should start with <");
    assertTrue(
        messageId.endsWith("@test.einnsyn.no>"), "Message-ID should end with @test.einnsyn.no>");
    assertEquals(
        messageId, mimeMessage.getHeader("Message-ID", null), "Message-ID header mismatch");
  }
}
