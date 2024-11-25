package no.einnsyn.apiv3.utils;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.mail.MessagingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.utils.idgenerator.IdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailSender {

  private final JavaMailSender javaMailSender;
  private final MailRenderer mailRenderer;
  private final MeterRegistry meterRegistry;

  @Value("${application.email.from_host:example.com}")
  private String fromFqdn;

  @Value("${application.baseUrl}")
  private String baseUrl;

  private final Pattern variablePattern = Pattern.compile("\\{([\\w\\.]+)\\}");

  public MailSender(
      JavaMailSender javaMailSender, MailRenderer mailRenderer, MeterRegistry meterRegistry) {
    this.javaMailSender = javaMailSender;
    this.mailRenderer = mailRenderer;
    this.meterRegistry = meterRegistry;
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
    var mimeMessage = javaMailSender.createMimeMessage();
    var message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
    message.setSubject(labels.get(templateName + "Subject"));
    message.setFrom(from);
    message.setTo(to);

    // Render email-content (HTML and TXT)
    var html = mailRenderer.renderFile("mailtemplates/" + templateName + ".html.mustache", context);
    var txt = mailRenderer.renderFile("mailtemplates/" + templateName + ".txt.mustache", context);
    message.setText(txt, html);

    // Add attachment
    if (attachment != null) {
      if (attachmentName == null) {
        attachmentName = "attachment";
      }
      if (attachmentContentType == null) {
        attachmentContentType = "application/octet-stream";
      }
      message.addAttachment(attachmentName, attachment, attachmentContentType);
    }

    // Set message id
    mimeMessage.setHeader(
        "Message-ID", "<" + IdGenerator.generateId("email") + "@" + fromFqdn + ">");

    try {
      javaMailSender.send(mimeMessage);
      meterRegistry.counter("ein_email", "status", "success").increment();
    } catch (MailException e) {
      meterRegistry.counter("ein_email", "status", "failed").increment();
      log.error("Could not send email to {}", to, e);
      throw e;
    }
  }
}
