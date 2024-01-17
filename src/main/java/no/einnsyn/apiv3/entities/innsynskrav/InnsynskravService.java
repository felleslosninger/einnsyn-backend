package no.einnsyn.apiv3.entities.innsynskrav;

import jakarta.mail.MessagingException;
import java.util.HashMap;
import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.exceptions.UnauthorizedException;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravListQueryDTO;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelService;
import no.einnsyn.apiv3.utils.IdGenerator;
import no.einnsyn.apiv3.utils.MailSender;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InnsynskravService extends BaseService<Innsynskrav, InnsynskravDTO> {

  @Getter private InnsynskravRepository repository;

  @Getter @Lazy @Autowired private InnsynskravService proxy;

  private InnsynskravSenderService innsynskravSenderService;

  private MailSender mailSender;

  @Value("${application.email.from}")
  private String emailFrom;

  @URL
  @Value("${application.baseUrl}")
  private String emailBaseUrl;

  public InnsynskravService(
      InnsynskravRepository repository,
      InnsynskravDelService innsynskravDelService,
      InnsynskravSenderService innsynskravSenderService,
      MailSender mailSender) {
    super();
    this.repository = repository;
    this.innsynskravDelService = innsynskravDelService;
    this.innsynskravSenderService = innsynskravSenderService;
    this.mailSender = mailSender;
  }

  public Innsynskrav newObject() {
    return new Innsynskrav();
  }

  public InnsynskravDTO newDTO() {
    return new InnsynskravDTO();
  }

  /**
   * Update or insert a InnsynskravBestilling from a DTO object. If no ID is given, a new
   * InnsynskravBestilling will be created, and a verification e-mail will be sent unless the user
   * is logged in.
   *
   * @param id
   * @param dto
   * @return
   */
  @Override
  public InnsynskravDTO update(String id, InnsynskravDTO dto) {

    var bruker = brukerService.getBrukerFromAuthentication();

    if (id == null && bruker != null) {
      dto.setVerified(true);
    }

    // Run regular update/insert procedure
    dto = super.update(id, dto);

    if (id == null) {
      var innsynskrav = repository.findById(dto.getId()).orElse(null);
      if (bruker != null) {
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

    return dto;
  }

  @Override
  public Innsynskrav fromDTO(
      InnsynskravDTO dto, Innsynskrav innsynskrav, Set<String> paths, String currentPath) {
    super.fromDTO(dto, innsynskrav, paths, currentPath);

    // This should never pass through the controller, and is only set internally
    if (dto.getVerified() != null) {
      innsynskrav.setVerified(dto.getVerified());
    }

    // This should never pass through the controller, and is only set internally
    if (innsynskrav.getId() == null && !innsynskrav.isVerified()) {
      String secret = IdGenerator.generate("issec");
      innsynskrav.setVerificationSecret(secret);
    }

    if (dto.getEmail() != null) {
      innsynskrav.setEpost(dto.getEmail());
    }

    if (dto.getLanguage() != null) {
      innsynskrav.setLanguage(dto.getLanguage());
    }

    var brukerField = dto.getBruker();
    if (dto.getBruker() != null) {
      var bruker = brukerService.findById(brukerField.getId());
      innsynskrav.setBruker(bruker);
    }

    // Add InnsynskravDel list
    var innsynskravDelListField = dto.getInnsynskravDel();
    if (innsynskravDelListField != null) {
      innsynskravDelListField.forEach(
          innsynskravDelField -> {

            // We don't accept existing InnsynskravDel objects
            var innsynskravDelDTO = innsynskravDelField.getExpandedObject();
            if (innsynskravDelDTO == null) {
              return;
            }

            // Set reference to innsynskrav if it's not already set
            if (innsynskravDelDTO.getInnsynskrav() == null) {
              innsynskravDelDTO.setInnsynskrav(new ExpandableField<>(innsynskrav.getId()));
            }

            var path = currentPath.isEmpty() ? "krav" : currentPath + ".krav";
            paths.add(path);
            var innsynskravDel =
                innsynskravDelService.fromDTO(innsynskravDelField.getExpandedObject(), paths, path);
            innsynskrav.getInnsynskravDel().add(innsynskravDel);
            innsynskravDel.setInnsynskrav(innsynskrav);
          });
    }

    return innsynskrav;
  }

  @Override
  public InnsynskravDTO toDTO(
      Innsynskrav innsynskrav, InnsynskravDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(innsynskrav, dto, expandPaths, currentPath);

    dto.setEmail(innsynskrav.getEpost());
    dto.setVerified(innsynskrav.isVerified());

    // Add bruker
    var bruker = innsynskrav.getBruker();
    if (bruker != null) {
      var expandableField = brukerService.maybeExpand(bruker, "bruker", expandPaths, currentPath);
      dto.setBruker(expandableField);
    }

    // Add InnsynskravDel list
    var innsynskravDelList = innsynskrav.getInnsynskravDel();
    var innsynskravDelJSONList = dto.getInnsynskravDel();
    if (innsynskravDelList != null) {
      innsynskravDelList.forEach(
          innsynskravDel -> {
            var expandableField =
                innsynskravDelService.maybeExpand(
                    innsynskravDel, "innsynskravDel", expandPaths, currentPath);
            innsynskravDelJSONList.add(expandableField);
          });
    }

    return dto;
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
    context.put(
        "actionUrl",
        emailBaseUrl
            + "/innsynskrav/"
            + innsynskrav.getId()
            + "/verify/"
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

    mailSender.send(
        emailFrom, innsynskrav.getEpost(), "orderConfirmationToBruker", language, context);
  }

  /**
   * Verify an anonymous innsynskrav
   *
   * @param innsynskrav
   * @param verificationSecret
   * @return
   */
  @Transactional
  public InnsynskravDTO verify(
      Innsynskrav innsynskrav, String verificationSecret, Set<String> expandPaths)
      throws UnauthorizedException {

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
        // TODO: Proper error handling
        System.out.println(e);
      }
    }

    return proxy.toDTO(innsynskrav, expandPaths);
  }

  /** Delete innsynskrav. This will cascade to InnsynskravDel and InnsynskravDelStatus. */
  @Transactional
  public InnsynskravDTO delete(Innsynskrav innsynskrav) {
    var dto = newDTO();
    repository.delete(innsynskrav);
    dto.setDeleted(true);
    return dto;
  }

  /**
   * Extend getPage to supprt filtering by "bruker"
   *
   * @param query
   * @return
   */
  @Transactional
  public Page<Innsynskrav> getPage(InnsynskravListQueryDTO query) {
    var brukerId = query.getBruker();
    var bruker = brukerService.findById(brukerId);

    if (bruker != null) {
      if (query.getStartingAfter() != null) {
        return repository.findByBrukerAndIdGreaterThanOrderByIdDesc(
            bruker, query.getStartingAfter(), PageRequest.of(0, query.getLimit() + 1));
      } else if (query.getEndingBefore() != null) {
        return repository.findByBrukerAndIdLessThanOrderByIdDesc(
            bruker, query.getEndingBefore(), PageRequest.of(0, query.getLimit() + 1));
      } else {
        return repository.findByBrukerOrderByIdDesc(
            bruker, PageRequest.of(0, query.getLimit() + 1));
      }
    }

    return super.getPage(query);
  }
}
