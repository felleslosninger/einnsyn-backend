package no.einnsyn.backend.utils.mail;

import com.google.gson.Gson;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import no.einnsyn.backend.utils.id.IdGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailSenderService {

  private final JavaMailSender javaMailSender;
  private final MailRendererService mailRenderer;
  private final MeterRegistry meterRegistry;
  private final Gson gson;

  @Value("${application.email.from_host:example.com}")
  private String fromFqdn;

  @Value("${application.baseUrl}")
  private String baseUrl;

  private final Pattern variablePattern = Pattern.compile("\\{([\\w\\.]+)\\}");

  public MailSenderService(
      JavaMailSender javaMailSender,
      MailRendererService mailRenderer,
      MeterRegistry meterRegistry,
      @Qualifier("pretty") Gson gson) {
    this.javaMailSender = javaMailSender;
    this.mailRenderer = mailRenderer;
    this.meterRegistry = meterRegistry;
    this.gson = gson;
  }

  public void send(
      String from, String to, String templateName, String language, Map<String, Object> context)
      throws MessagingException {
    send(from, to, templateName, language, context, null, null, null);
  }

  /**
   * Send email
   *
   * @param from
   * @param to
   * @param templateName
   * @param language
   * @param context
   * @param attachment
   * @param attachmentName
   * @param attachmentContentType
   * @return
   * @throws MessagingException
   * @throws MailException
   * @throws Exception
   */
  @SuppressWarnings("java:S107") // Allow 8 parameters
  public void send(
      String from,
      String to,
      String templateName,
      String language,
      Map<String, Object> context,
      ByteArrayResource attachment,
      String attachmentName,
      String attachmentContentType)
      throws MessagingException {

    context.put("baseUrl", baseUrl);

    // Read translated template strings
    var locale = Locale.forLanguageTag(language);
    var languageBundle = ResourceBundle.getBundle("mailtemplates/mailtemplates", locale);
    var labels = new HashMap<String, String>();
    for (var key : languageBundle.keySet()) {
      var value = languageBundle.getString(key);

      // Replace {string} with values from context
      var variableMatcher = variablePattern.matcher(value);
      while (variableMatcher.find()) {
        var variable = variableMatcher.group(1);
        if (context.containsKey(variable)) {
          value = value.replace("{" + variable + "}", context.get(variable).toString());
        }
      }

      labels.put(key, value);
    }
    context.put("labels", labels);

    // Create message
    // If we set the Message-ID header manually on the MimeMessage, it will be overridden by
    // MimeMessage.saveChanges(). Therefore, we create a subclass that overrides updateMessageID()
    // to do nothing.
    var messageId = "<" + IdGenerator.generateId("email") + "@" + fromFqdn + ">";
    // The JavaMailSender bean is always created as JavaMailSenderImpl
    var session = ((JavaMailSenderImpl) javaMailSender).getSession();
    var mimeMessage = new MimeMessageWithFixedId(session, messageId);

    // Render email-content (HTML and TXT)
    var html = mailRenderer.renderFile("mailtemplates/" + templateName + ".html.mustache", context);
    var txt = mailRenderer.renderFile("mailtemplates/" + templateName + ".txt.mustache", context);

    if (attachment != null) {
      // With attachment: use MimeMessageHelper with multipart/mixed
      var message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
      message.setSubject(labels.get(templateName + "Subject"));
      message.setFrom(from);
      message.setTo(to);
      message.setText(txt, html);

      if (attachmentName == null) {
        attachmentName = "attachment";
      }
      if (attachmentContentType == null) {
        attachmentContentType = "application/octet-stream";
      }
      message.addAttachment(attachmentName, attachment, attachmentContentType);
    } else {
      // Without attachment: manually create multipart/alternative structure
      var message = new MimeMessageHelper(mimeMessage, false, "UTF-8");
      message.setSubject(labels.get(templateName + "Subject"));
      message.setFrom(from);
      message.setTo(to);

      // Create multipart/alternative content manually
      var multipart = new MimeMultipart("alternative");

      // Add text part
      var textPart = new MimeBodyPart();
      textPart.setText(txt, "UTF-8");
      multipart.addBodyPart(textPart);

      // Add HTML part
      var htmlPart = new MimeBodyPart();
      htmlPart.setContent(html, "text/html; charset=UTF-8");
      multipart.addBodyPart(htmlPart);

      mimeMessage.setContent(multipart);
    }

    try {
      if (log.isDebugEnabled()) {
        var mimeMessageContent = getRawMimeMessageContent(mimeMessage);
        log.debug(
            "Sending email to {} with subject '{}' and template '{}'. Has attachment: {}",
            to,
            labels.get(templateName + "Subject"),
            templateName,
            attachment != null,
            StructuredArguments.raw("messageBody", gson.toJson(mimeMessageContent)));
      }
      javaMailSender.send(mimeMessage);
      meterRegistry.counter("ein_email", "status", "success").increment();
    } catch (MailException e) {
      meterRegistry.counter("ein_email", "status", "failed").increment();
      log.error("Could not send email to {}", to, e);
      throw e;
    }
  }

  /**
   * Converts a MimeMessage into its raw string representation.
   *
   * @param mimeMessage The message to convert.
   * @return The raw MIME content as a String, or an error message if conversion fails.
   */
  private String getRawMimeMessageContent(MimeMessage mimeMessage) {
    try {
      var outputStream = new ByteArrayOutputStream();
      mimeMessage.saveChanges();
      mimeMessage.writeTo(outputStream);
      return outputStream.toString(StandardCharsets.UTF_8.name());
    } catch (IOException | MessagingException e) {
      log.error("Error converting MimeMessage to raw string for logging.", e);
      return "[ERROR: Could not get raw MIME content: " + e.getMessage() + "]";
    }
  }
}
