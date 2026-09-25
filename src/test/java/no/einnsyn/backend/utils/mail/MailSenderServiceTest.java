package no.einnsyn.backend.utils.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

class MailSenderServiceTest {

  private static final String NB_CONFIRM_SUBJECT =
      ResourceBundle.getBundle("mailtemplates/mailtemplates", Locale.forLanguageTag("nb"))
          .getString("confirmAnonymousOrderSubject");

  @Test
  void testCustomMessageIdIsSet() throws Exception {
    var javaMailSender = mock(JavaMailSenderImpl.class);
    var meterRegistry = new SimpleMeterRegistry();
    var service = newService(javaMailSender, meterRegistry);

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

  @Test
  void orderConfirmationToBrukerHasRequiredTranslationsForAllSupportedLanguages() {
    var requiredKeys =
        List.of(
            "orderConfirmationToBrukerSubject",
            "orderConfirmationToBrukerTitle",
            "orderConfirmationToBrukerIntro",
            "orderConfirmationToBrukerCaseTitleLabel",
            "orderConfirmationToBrukerJournalTitleLabel",
            "orderConfirmationToBrukerDocumentNumberLabel",
            "orderConfirmationToBrukerCaseNumberLabel",
            "orderConfirmationToBrukerOrganizationLabel",
            "orderConfirmationToBrukerActionLabel",
            "orderConfirmationToBrukerEmpty");

    for (var language : List.of("nb", "nn", "en", "se")) {
      var bundle =
          ResourceBundle.getBundle("mailtemplates/mailtemplates", Locale.forLanguageTag(language));
      for (var key : requiredKeys) {
        assertTrue(bundle.containsKey(key), "Missing key " + key + " for language " + language);
      }
    }
  }

  private MailSenderService newService(
      JavaMailSenderImpl javaMailSender, SimpleMeterRegistry meterRegistry) {
    return newService(javaMailSender, meterRegistry, "test");
  }

  private MailSenderService newService(
      JavaMailSenderImpl javaMailSender, SimpleMeterRegistry meterRegistry, String environment) {
    when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    var renderer = mock(MailRendererService.class);
    when(renderer.renderFile(anyString(), any())).thenReturn("content");

    var service = new MailSenderService(javaMailSender, renderer, meterRegistry);
    ReflectionTestUtils.setField(service, "fromFqdn", "test.einnsyn.no");
    ReflectionTestUtils.setField(service, "baseUrl", "https://test.einnsyn.no");
    ReflectionTestUtils.setField(service, "environment", environment);
    return service;
  }

  private String sentSubject(JavaMailSenderImpl javaMailSender) throws Exception {
    var mimeMessageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(javaMailSender, times(1)).send(mimeMessageCaptor.capture());
    return mimeMessageCaptor.getValue().getSubject();
  }

  @Test
  void testSubjectIsPrefixedWithEnvironmentOutsideProduction() throws Exception {
    var javaMailSender = mock(JavaMailSenderImpl.class);
    var service = newService(javaMailSender, new SimpleMeterRegistry(), "test");
    var context = new HashMap<String, Object>();

    service.send("from@example.com", "to@example.com", "confirmAnonymousOrder", "nb", context);

    assertEquals("[TEST] " + NB_CONFIRM_SUBJECT, sentSubject(javaMailSender));
  }

  @Test
  void testSubjectIsNotPrefixedInProduction() throws Exception {
    var javaMailSender = mock(JavaMailSenderImpl.class);
    var service = newService(javaMailSender, new SimpleMeterRegistry(), "prod");
    var context = new HashMap<String, Object>();

    service.send("from@example.com", "to@example.com", "confirmAnonymousOrder", "nb", context);

    assertEquals(NB_CONFIRM_SUBJECT, sentSubject(javaMailSender));
  }

  /** Emails with an attachment are sent as multipart/mixed with the attachment included. */
  @Test
  void testSendWithAttachment() throws Exception {
    var javaMailSender = mock(JavaMailSenderImpl.class);
    var meterRegistry = new SimpleMeterRegistry();
    var service = newService(javaMailSender, meterRegistry);

    var attachment = new ByteArrayResource("attachment content".getBytes(StandardCharsets.UTF_8));
    service.send(
        "from@example.com",
        "to@example.com",
        "confirmAnonymousOrder",
        "nb",
        new HashMap<>(),
        attachment,
        "order.pdf",
        "application/pdf");

    var mimeMessageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(javaMailSender, times(1)).send(mimeMessageCaptor.capture());
    var mimeMessage = mimeMessageCaptor.getValue();
    mimeMessage.saveChanges();

    var multipart = (MimeMultipart) mimeMessage.getContent();
    var attachmentPart = multipart.getBodyPart(multipart.getCount() - 1);
    assertEquals("order.pdf", attachmentPart.getFileName());
    assertEquals(1.0, meterRegistry.counter("ein_email", "status", "success").count());
  }

  /** A failed send increments the failure counter and rethrows the MailException. */
  @Test
  void testSendFailureIncrementsFailedCounter() throws Exception {
    var javaMailSender = mock(JavaMailSenderImpl.class);
    var meterRegistry = new SimpleMeterRegistry();
    var service = newService(javaMailSender, meterRegistry);
    doThrow(new MailSendException("SMTP unavailable"))
        .when(javaMailSender)
        .send(any(MimeMessage.class));

    assertThrows(
        MailSendException.class,
        () ->
            service.send(
                "from@example.com",
                "to@example.com",
                "confirmAnonymousOrder",
                "nb",
                new HashMap<>()));

    assertEquals(1.0, meterRegistry.counter("ein_email", "status", "failed").count());
    assertEquals(0.0, meterRegistry.counter("ein_email", "status", "success").count());
  }
}
