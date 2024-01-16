package no.einnsyn.apiv3.utils;

import jakarta.mail.MessagingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailSender {

  private JavaMailSender javaMailSender;

  private MailRenderer mailRenderer;

  public MailSender(JavaMailSender javaMailSender, MailRenderer mailRenderer) {
    this.javaMailSender = javaMailSender;
    this.mailRenderer = mailRenderer;
  }

  public boolean send(
      String from, String to, String templateName, String language, Map<String, Object> context)
      throws MessagingException {
    return send(from, to, templateName, language, context, null, null, null);
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
   * @throws Exception
   */
  @SuppressWarnings("java:S107") // Allow 8 parameters
  public boolean send(
      String from,
      String to,
      String templateName,
      String language,
      Map<String, Object> context,
      ByteArrayResource attachment,
      String attachmentName,
      String attachmentContentType)
      throws MessagingException {

    // Read translated template strings
    var locale = Locale.forLanguageTag(language);
    var languageBundle = ResourceBundle.getBundle("mailtemplates/mailtemplates", locale);
    Map<String, String> labels = new HashMap<>();
    for (var key : languageBundle.keySet()) {
      labels.put(key, languageBundle.getString(key));
    }
    context.put("labels", labels);

    // Create message
    var mimeMessage = javaMailSender.createMimeMessage();
    var message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
    message.setSubject(languageBundle.getString(templateName + "Subject"));
    message.setFrom(from);
    message.setTo(to);

    // Render email-content (HTML and TXT)
    var html = mailRenderer.render("mailtemplates/" + templateName + ".html.mustache", context);
    var txt = mailRenderer.render("mailtemplates/" + templateName + ".txt.mustache", context);
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

    // Send message in a separate thread
    new Thread(() -> javaMailSender.send(mimeMessage)).start();

    return true;
  }
}
