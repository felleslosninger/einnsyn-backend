package no.einnsyn.apiv3.entities.innsynskrav;

import jakarta.mail.MessagingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.exceptions.UnauthorizedException;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.paginators.Paginators;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravListQueryDTO;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelService;
import no.einnsyn.apiv3.utils.MailSender;
import no.einnsyn.apiv3.utils.idgenerator.IdGenerator;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InnsynskravService extends BaseService<Innsynskrav, InnsynskravDTO> {

  @Getter private InnsynskravRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private InnsynskravService proxy;

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
  public InnsynskravDTO update(String id, InnsynskravDTO dto) throws EInnsynException {

    var bruker = brukerService.getBrukerFromAuthentication();

    if (id == null && bruker != null) {
      dto.setVerified(true);
      dto.setBruker(new ExpandableField<>(bruker.getId()));
    }

    // Run regular update/insert procedure
    dto = super.update(id, dto);

    if (id == null) {
      var innsynskrav = repository.findById(dto.getId()).orElse(null);
      if (bruker != null) {
        innsynskravSenderService.sendInnsynskrav(innsynskrav);
      } else {
        try {
          sendAnonymousConfirmationEmail(innsynskrav);
        } catch (Exception e) {
          throw new EInnsynException("Unable to send confirmation email", e);
        }
      }
    }

    return dto;
  }

  @Override
  public Innsynskrav fromDTO(
      InnsynskravDTO dto, Innsynskrav innsynskrav, Set<String> paths, String currentPath)
      throws EInnsynException {
    super.fromDTO(dto, innsynskrav, paths, currentPath);

    // This should never pass through the controller, and is only set internally
    if (dto.getVerified() != null) {
      innsynskrav.setVerified(dto.getVerified());
    }

    // This should never pass through the controller, and is only set internally
    if (innsynskrav.getId() == null && !innsynskrav.isVerified()) {
      var secret = IdGenerator.generate("issec");
      innsynskrav.setVerificationSecret(secret);
    }

    if (dto.getEmail() != null) {
      innsynskrav.setEpost(dto.getEmail());
    }

    if (dto.getLanguage() != null) {
      innsynskrav.setLanguage(dto.getLanguage());
    }

    var brukerField = dto.getBruker();
    if (brukerField != null) {
      var bruker = brukerService.findById(brukerField.getId());
      innsynskrav.setBruker(bruker);
    }

    // Persist before adding relations
    if (innsynskrav.getId() == null) {
      innsynskrav = repository.save(innsynskrav);
    }

    // Add InnsynskravDel list
    var innsynskravDelListField = dto.getInnsynskravDel();
    if (innsynskravDelListField != null) {
      for (var innsynskravDelField : innsynskravDelListField) {
        innsynskrav.addInnsynskravDel(
            innsynskravDelService.insertOrThrow(
                innsynskravDelField, "innsynskravDel", paths, currentPath));
      }
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
    var innsynskravDelListDTO = dto.getInnsynskravDel();
    if (innsynskravDelListDTO == null) {
      innsynskravDelListDTO = new ArrayList<>();
      dto.setInnsynskravDel(innsynskravDelListDTO);
    }
    var innsynskravDelList = innsynskrav.getInnsynskravDel();
    if (innsynskravDelList != null) {
      for (var innsynskravDel : innsynskravDelList) {
        var expandableField =
            innsynskravDelService.maybeExpand(
                innsynskravDel, "innsynskravDel", expandPaths, currentPath);
        innsynskravDelListDTO.add(expandableField);
      }
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
  public InnsynskravDTO verifyInnsynskrav(String innsynskravId, String verificationSecret)
      throws UnauthorizedException {
    var innsynskrav = innsynskravService.findById(innsynskravId);

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

    return proxy.toDTO(innsynskrav);
  }

  /**
   * Delete an Innsynskrav
   *
   * @param innsynskrav
   */
  @Transactional
  public InnsynskravDTO delete(Innsynskrav innsynskrav) {
    var dto = innsynskravService.toDTO(innsynskrav);

    // Delete all InnsynskravDel objects
    var innsynskravDelList = innsynskrav.getInnsynskravDel();
    if (innsynskravDelList != null) {
      for (var innsynskravDel : innsynskravDelList) {
        innsynskravDelService.delete(innsynskravDel);
      }
    }

    repository.delete(innsynskrav);
    dto.setDeleted(true);
    return dto;
  }

  @Override
  public Paginators<Innsynskrav> getPaginators(BaseListQueryDTO params) {
    if (params instanceof InnsynskravListQueryDTO p && p.getBrukerId() != null) {
      var bruker = brukerService.findById(p.getBrukerId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(bruker, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(bruker, pivot, pageRequest));
    }
    return super.getPaginators(params);
  }
}
