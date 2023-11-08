package no.einnsyn.apiv3.entities.innsynskrav;

import java.util.HashMap;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import lombok.Getter;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravJSON;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelService;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDel;
import no.einnsyn.apiv3.utils.IdGenerator;
import no.einnsyn.apiv3.utils.MailRenderer;
import no.einnsyn.apiv3.utils.MailSender;

@Service
public class InnsynskravService extends EinnsynObjectService<Innsynskrav, InnsynskravJSON> {

  @Getter
  private InnsynskravRepository repository;
  private InnsynskravDelService innsynskravDelService;
  private InnsynskravSenderService innsynskravSenderService;
  private MailSender mailSender;

  @Value("${email.from}")
  private String emailFrom;

  @Value("${email.baseUrl}")
  private String emailBaseUrl;

  MailRenderer mailRenderer;


  public InnsynskravService(InnsynskravRepository repository,
      InnsynskravDelService innsynskravDelService,
      InnsynskravSenderService innsynskravSenderService, MailSender mailSender,
      MailRenderer mailRenderer) {
    super();
    this.repository = repository;
    this.innsynskravDelService = innsynskravDelService;
    this.innsynskravSenderService = innsynskravSenderService;
    this.mailSender = mailSender;
    this.mailRenderer = mailRenderer;
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

    if (json.getLanguage() != null) {
      innsynskrav.setLanguage(json.getLanguage());
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
    json.setVerified(innsynskrav.isVerified());

    // Add InnsynskravDel list
    var innsynskravDelList = innsynskrav.getInnsynskravDel();
    var innsynskravDelJSONList = json.getInnsynskravDel();
    if (innsynskravDelList != null) {
      innsynskravDelList.forEach(innsynskravDel -> {
        var expandableField = innsynskravDelService.maybeExpand(innsynskravDel, "innsynskravDel",
            expandPaths, currentPath);
        innsynskravDelJSONList.add(expandableField);
      });
    }

    return json;
  }


  /**
   * Send e-mail to user, asking to verify the Innsynskrav
   * 
   * @param innsynskrav
   * @throws Exception
   */
  public void sendAnonymousConfirmationEmail(Innsynskrav innsynskrav) throws Exception {
    var language = innsynskrav.getLanguage();
    var context = new HashMap<String, Object>();
    context.put("actionUrl", emailBaseUrl + "/innsynskrav/" + innsynskrav.getId() + "/verify/"
        + innsynskrav.getVerificationSecret()); // TODO: Final URL will be different

    mailSender.send(emailFrom, innsynskrav.getEpost(), "confirmAnonymousOrder", language, context);
  }


  /**
   * Send order confirmation e-mail to bruker
   * 
   * @param innsynskrav
   * @throws Exception
   */
  public void sendOrderConfirmationToBruker(Innsynskrav innsynskrav) throws Exception {
    var language = innsynskrav.getLanguage();
    var context = new HashMap<String, Object>();
    context.put("innsynskrav", innsynskrav);
    context.put("innsynskravDelList", innsynskrav.getInnsynskravDel());

    mailSender.send(emailFrom, innsynskrav.getEpost(), "orderConfirmationToBruker", language,
        context);
  }


  /**
   * Verify an anonymous innsynskrav
   * 
   * @param innsynskrav
   * @param verificationSecret
   * @return
   */
  @Transactional
  public InnsynskravJSON verify(Innsynskrav innsynskrav, String verificationSecret,
      Set<String> expandPaths) {
    if (!innsynskrav.isVerified()
        && innsynskrav.getVerificationSecret().equals(verificationSecret)) {
      innsynskrav.setVerified(true);
      repository.saveAndFlush(innsynskrav);
      innsynskravSenderService.sendInnsynskrav(innsynskrav);
      try {
        sendOrderConfirmationToBruker(innsynskrav);
      } catch (Exception e) {
        // TODO: Proper error handling
        System.out.println(e);
      }
    }

    return toJSON(innsynskrav, expandPaths);
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
