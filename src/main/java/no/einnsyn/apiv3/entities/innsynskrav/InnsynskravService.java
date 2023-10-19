package no.einnsyn.apiv3.entities.innsynskrav;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.Getter;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravJSON;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelService;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDel;
import no.einnsyn.apiv3.utils.IdGenerator;

@Service
public class InnsynskravService extends EinnsynObjectService<Innsynskrav, InnsynskravJSON> {

  @Getter
  private InnsynskravRepository repository;
  private InnsynskravDelService innsynskravDelService;
  private InnsynskravSenderService innsynskravSenderService;
  private JavaMailSender mailSender;

  @Value("${email.from}")
  private String emailFrom;

  @Value("${email.baseUrl}")
  private String emailBaseUrl;

  MustacheFactory mustacheFactory = new DefaultMustacheFactory();
  Mustache confirmAnonymousOrderTemplateHTML =
      mustacheFactory.compile("mailtemplates/confirmAnonymousOrder.html.mustache");
  Mustache confirmAnonymousOrderTemplateTXT =
      mustacheFactory.compile("mailtemplates/confirmAnonymousOrder.txt.mustache");


  public InnsynskravService(InnsynskravRepository repository,
      InnsynskravDelService innsynskravDelService,
      InnsynskravSenderService innsynskravSenderService, JavaMailSender mailSender) {
    super();
    this.repository = repository;
    this.innsynskravDelService = innsynskravDelService;
    this.innsynskravSenderService = innsynskravSenderService;
    this.mailSender = mailSender;
  }


  public Innsynskrav newObject() {
    return new Innsynskrav();
  }


  public InnsynskravJSON newJSON() {
    return new InnsynskravJSON();
  }


  /**
   * Update or insert a InnsynskravBestilling from a JSON object. If no ID is given, a new
   * InnsynskravBestilling will be created, and a verification e-mail will be sent unless the user
   * is logged in.
   * 
   * @param id
   * @param json
   * @return
   */
  public InnsynskravJSON update(String id, InnsynskravJSON json) {

    // If user is logged in
    boolean isLoggedIn = false;

    if (id == null) {
      if (isLoggedIn) {
        // json.setBruker(bruker); // TODO: implement Bruker
        json.setVerified(true);
      } else {
        String secret = IdGenerator.generate("issec");
        json.setVerificationSecret(secret);
      }
    }

    // Run regular update/insert procedure
    json = super.update(id, json);

    if (id == null) {
      Innsynskrav innsynskrav = repository.findById(json.getId());
      if (isLoggedIn) {
        innsynskravSenderService.sendInnsynskrav(innsynskrav);
      } else {
        // Send verification email
        try {
          sendAnonymousConfirmationEmail(innsynskrav);
        } catch (Exception e) {
          // TODO: We couldn't send the verification email, log / report this
          System.out.println(e);
        }
      }
    }

    return json;
  }


  public Innsynskrav fromJSON(InnsynskravJSON json, Innsynskrav innsynskrav, Set<String> paths,
      String currentPath) {
    super.fromJSON(json, innsynskrav, paths, currentPath);

    if (json.getEpost() != null) {
      innsynskrav.setEpost(json.getEpost());
    }

    // This should never pass through the controller, and is only set internally
    if (json.getVerified() != null) {
      innsynskrav.setVerified(json.getVerified());
    }

    // This should never pass through the controller, and is only set internally
    if (json.getVerificationSecret() != null) {
      innsynskrav.setVerificationSecret(json.getVerificationSecret());
    }

    // Add InnsynskravDel list
    var innsynskravDelListField = json.getInnsynskravDel();
    if (innsynskravDelListField != null) {
      innsynskravDelListField.forEach(innsynskravDelField -> {

        // We don't accept existing InnsynskravDel objects
        var innsynskravDelJSON = innsynskravDelField.getExpandedObject();
        if (innsynskravDelJSON == null) {
          return;
        }

        // Set reference to innsynskrav if it's not already set
        if (innsynskravDelJSON.getInnsynskrav() == null) {
          innsynskravDelJSON
              .setInnsynskrav(new ExpandableField<InnsynskravJSON>(innsynskrav.getId()));
        }

        String path = currentPath.equals("") ? "krav" : currentPath + ".krav";
        paths.add(path);
        InnsynskravDel innsynskravDel =
            innsynskravDelService.fromJSON(innsynskravDelField.getExpandedObject(), paths, path);
        innsynskrav.getInnsynskravDel().add(innsynskravDel);
        innsynskravDel.setInnsynskrav(innsynskrav);
      });
    }

    return innsynskrav;

  }


  public InnsynskravJSON toJSON(Innsynskrav innsynskrav, InnsynskravJSON json,
      Set<String> expandPaths, String currentPath) {
    super.toJSON(innsynskrav, json, expandPaths, currentPath);

    json.setEpost(innsynskrav.getEpost());
    json.setVerified(innsynskrav.getVerified());

    // Add InnsynskravDel list
    var innsynskravDelList = innsynskrav.getInnsynskravDel();
    var innsynskravDelJSONList = json.getInnsynskravDel();
    if (innsynskravDelList != null) {
      innsynskravDelList.forEach(innsynskravDel -> {
        var expandableField =
            innsynskravDelService.maybeExpand(innsynskravDel, "krav", expandPaths, currentPath);
        innsynskravDelJSONList.add(expandableField);
      });
    }

    return json;
  }


  public void sendAnonymousConfirmationEmail(Innsynskrav innsynskrav) throws MessagingException {
    var language = innsynskrav.getLanguage();
    var locale = Locale.forLanguageTag(language);
    var languageBundle = ResourceBundle.getBundle("mailtemplates/confirmAnonymousOrder", locale);

    var mimeMessage = mailSender.createMimeMessage();
    var message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
    message.setSubject(languageBundle.getString("subject"));
    message.setFrom(emailFrom);
    message.setTo(innsynskrav.getEpost());

    var context = new HashMap<String, String>();
    context.put("title", languageBundle.getString("title"));
    context.put("intro", languageBundle.getString("intro"));
    context.put("actionbutton", languageBundle.getString("actionbutton"));
    context.put("actionurl", emailBaseUrl + "/innsynskrav/" + innsynskrav.getId() + "/verify/"
        + innsynskrav.getVerificationSecret()); // TODO: Final URL will be different
    context.put("outro", languageBundle.getString("outro"));

    StringWriter htmlWriter = new StringWriter();
    confirmAnonymousOrderTemplateHTML.execute(htmlWriter, context);
    StringWriter plainWriter = new StringWriter();
    confirmAnonymousOrderTemplateTXT.execute(plainWriter, context);

    message.setText(plainWriter.toString(), htmlWriter.toString());
    mailSender.send(mimeMessage);
  }


  @Transactional
  public InnsynskravJSON verify(Innsynskrav innsynskrav, String verificationSecret) {
    if (innsynskrav.getVerificationSecret().equals(verificationSecret)) {
      innsynskrav.setVerified(true);
      repository.saveAndFlush(innsynskrav);
      innsynskravSenderService.sendInnsynskrav(innsynskrav);
    }

    return toJSON(innsynskrav);
  }


  @Transactional
  public InnsynskravJSON delete(String id) {
    return delete(repository.findById(id));
  }

  /**
   * Delete innsynskrav. This will cascade to InnsynskravDel and InnsynskravDelStatus.
   */
  @Transactional
  public InnsynskravJSON delete(Innsynskrav innsynskrav) {
    InnsynskravJSON json = newJSON();
    repository.delete(innsynskrav);
    json.setDeleted(true);
    return json;
  }

}
