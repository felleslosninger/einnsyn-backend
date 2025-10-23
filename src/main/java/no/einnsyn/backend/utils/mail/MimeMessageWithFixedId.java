package no.einnsyn.backend.utils.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

// If we set the Message-ID header manually on the MimeMessage, it will be overridden by
// MimeMessage.saveChanges(). Therefore, we create a subclass that overrides updateMessageID()
// to do nothing.
public class MimeMessageWithFixedId extends MimeMessage {

  public MimeMessageWithFixedId(Session session, String messageId) throws MessagingException {
    super(session);
    setHeader("Message-ID", messageId);
  }

  @Override
  protected void updateMessageID() throws MessagingException {
    // Do nothing, we already set a fixed Message-ID
  }
}
