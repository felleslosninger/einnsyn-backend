package no.einnsyn.apiv3.entities.innsynskrav;

import java.util.HashMap;
import java.util.Set;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.bruker.BrukerService;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravJSON;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelService;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDel;
import no.einnsyn.apiv3.exceptions.UnauthorizedException;
import no.einnsyn.apiv3.utils.IdGenerator;
import no.einnsyn.apiv3.utils.MailSender;

@Slf4j
@Service
public class InnsynskravService extends EinnsynObjectService<Innsynskrav, InnsynskravJSON> {

  @Getter
  private InnsynskravRepository repository;
  private InnsynskravDelService innsynskravDelService;
  private InnsynskravSenderService innsynskravSenderService;
  private MailSender mailSender;

  @Resource
  private BrukerService brukerService;

  @Value("${application.email.from}")
  private String emailFrom;

  @URL
  @Value("${application.baseUrl}")
  private String emailBaseUrl;

  @Getter
  private InnsynskravService service = this;


  public InnsynskravService(InnsynskravRepository repository,
      InnsynskravDelService innsynskravDelService,
      InnsynskravSenderService innsynskravSenderService, MailSender mailSender) {
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
  @Override
  public InnsynskravJSON update(String id, InnsynskravJSON json) {

    var bruker = brukerService.getBrukerFromAuthentication();

    if (id == null) {
      if (bruker != null) {
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
      if (bruker != null) {
        innsynskravSenderService.sendInnsynskrav(innsynskrav);
      } else {
        // Send verification email
        try {
          sendAnonymousConfirmationEmail(innsynskrav);
        } catch (Exception e) {
          log.error("Could not send verification email", e);
        }
      }
    }

    return json;
  }


  @Override
  public Innsynskrav fromJSON(InnsynskravJSON json, Innsynskrav innsynskrav, Set<String> paths,
      String currentPath) {
    super.fromJSON(json, innsynskrav, paths, currentPath);

    if (json.getEmail() != null) {
      innsynskrav.setEpost(json.getEmail());
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

    var brukerField = json.getBruker();
    if (json.getBruker() != null) {
      var bruker = brukerService.findById(brukerField.getId());
      innsynskrav.setBruker(bruker);
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
          innsynskravDelJSON.setInnsynskrav(new ExpandableField<>(innsynskrav.getId()));
        }

        String path = currentPath.isEmpty() ? "krav" : currentPath + ".krav";
        paths.add(path);
        InnsynskravDel innsynskravDel =
            innsynskravDelService.fromJSON(innsynskravDelField.getExpandedObject(), paths, path);
        innsynskrav.getInnsynskravDel().add(innsynskravDel);
        innsynskravDel.setInnsynskrav(innsynskrav);
      });
    }

    return innsynskrav;

  }


  @Override
  public InnsynskravJSON toJSON(Innsynskrav innsynskrav, InnsynskravJSON json,
      Set<String> expandPaths, String currentPath) {
    super.toJSON(innsynskrav, json, expandPaths, currentPath);

    json.setEmail(innsynskrav.getEpost());
    json.setVerified(innsynskrav.isVerified());

    // Add bruker
    var bruker = innsynskrav.getBruker();
    if (bruker != null) {
      var expandableField = brukerService.maybeExpand(bruker, "bruker", expandPaths, currentPath);
      json.setBruker(expandableField);
    }

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
  public void sendAnonymousConfirmationEmail(Innsynskrav innsynskrav) throws MessagingException {
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
  public void sendOrderConfirmationToBruker(Innsynskrav innsynskrav) throws MessagingException {
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
  @SuppressWarnings("java:S6809") // this.toJSON() is OK since we're already in a transaction
  public InnsynskravJSON verify(Innsynskrav innsynskrav, String verificationSecret,
      Set<String> expandPaths) throws UnauthorizedException {

    if (!innsynskrav.isVerified()) {
      // Secret didn't match
      if (!innsynskrav.getVerificationSecret().equals(verificationSecret)) {
        throw new UnauthorizedException("Verification secret did not match");
      }

      innsynskrav.setVerified(true);
      repository.saveAndFlush(innsynskrav);
      innsynskravSenderService.sendInnsynskrav(innsynskrav);

      try {
        sendOrderConfirmationToBruker(innsynskrav);
      } catch (Exception e) {
        log.error("Could not send order confirmation email", e);
      }
    }

    return toJSON(innsynskrav, expandPaths);
  }


  /**
   * Delete innsynskrav. This will cascade to InnsynskravDel and InnsynskravDelStatus.
   */
  @Transactional
  public InnsynskravJSON delete(Innsynskrav innsynskrav) {
    var json = newJSON();
    repository.delete(innsynskrav);
    json.setDeleted(true);
    return json;
  }

}
