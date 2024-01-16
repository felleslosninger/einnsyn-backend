package no.einnsyn.apiv3.entities.bruker;

import jakarta.annotation.Nullable;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.authentication.bruker.models.BrukerUserDetails;
import no.einnsyn.apiv3.common.exceptions.UnauthorizedException;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.bruker.BrukerController.PutBrukerPasswordDTO;
import no.einnsyn.apiv3.entities.bruker.BrukerController.PutBrukerPasswordWithSecretDTO;
import no.einnsyn.apiv3.entities.bruker.models.Bruker;
import no.einnsyn.apiv3.entities.bruker.models.BrukerDTO;
import no.einnsyn.apiv3.entities.bruker.models.LanguageEnum;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravListQueryDTO;
import no.einnsyn.apiv3.entities.lagretsak.models.LagretSakDTO;
import no.einnsyn.apiv3.entities.lagretsak.models.LagretSakListQueryDTO;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoekDTO;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoekListQueryDTO;
import no.einnsyn.apiv3.utils.IdGenerator;
import no.einnsyn.apiv3.utils.MailSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class BrukerService extends BaseService<Bruker, BrukerDTO> {

  @Getter private final BrukerRepository repository;

  @Getter @Lazy @Autowired protected BrukerService proxy;

  private final MailSender mailSender;
  private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Value("${application.email.from}")
  private String emailFrom;

  @Value("${application.baseUrl}")
  private String emailBaseUrl;

  @Value("${application.userSecretExpirationTime}")
  private int userSecretExpirationTime;

  public BrukerService(BrukerRepository brukerRepository, MailSender mailSender) {
    super();
    this.repository = brukerRepository;
    this.mailSender = mailSender;
  }

  @Override
  public BrukerDTO newDTO() {
    return new BrukerDTO();
  }

  @Override
  public Bruker newObject() {
    return new Bruker();
  }

  /**
   * Update or create a "bruker"
   *
   * @param id
   * @param dto
   * @return
   */
  @Override
  @Transactional
  public BrukerDTO update(String id, BrukerDTO dto) {
    // Run regular update/insert procedure
    dto = super.update(id, dto);

    // Send activation email if this is an insert
    if (id == null) {
      var object = findById(dto.getId());
      try {
        sendActivationEmail(object);
      } catch (Exception e) {
        // TODO: We couldn't send the verification email, log / report this
        System.out.println(e);
        e.printStackTrace();
      }
    }

    return dto;
  }

  @Override
  @Transactional
  public boolean existsById(String id) {
    // Try to lookup by email if id contains @
    if (id.contains("@")) {
      var bruker = repository.existsByEmail(id);
      if (bruker) {
        return bruker;
      }
    }
    return super.existsById(id);
  }

  /** Make findById also lookup by email */
  @Override
  @Transactional
  public Bruker findById(String id) {
    // Try to lookup by email if id contains @
    if (id != null && id.contains("@")) {
      var bruker = repository.findByEmail(id);
      if (bruker != null) {
        return bruker;
      }
    }
    return super.findById(id);
  }

  /** Get bruker from authentication */
  @Transactional
  public Bruker getBrukerFromAuthentication() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null) {
      return null;
    }

    var principal = authentication.getPrincipal();
    if (principal == null) {
      return null;
    }

    if (!(principal instanceof BrukerUserDetails)) {
      return null;
    }

    var brukerUserDetails = (BrukerUserDetails) principal;
    return findById(brukerUserDetails.getId());
  }

  @Override
  @Transactional
  public Bruker fromDTO(BrukerDTO dto, Bruker bruker, Set<String> paths, String currentPath) {
    super.fromDTO(dto, bruker, paths, currentPath);

    // This is an insert, create activation secret
    if (bruker.getId() == null) {
      String secret = IdGenerator.generate("usec");
      bruker.setSecret(secret);
      bruker.setSecretExpiry(ZonedDateTime.now().plusSeconds(userSecretExpirationTime));
    }

    if (dto.getEmail() != null) {
      bruker.setEmail(dto.getEmail());
    }

    if (dto.getLanguage() != null) {
      bruker.setLanguage(LanguageEnum.valueOf(dto.getLanguage()));
    }

    return bruker;
  }

  @Override
  @Transactional
  public BrukerDTO toDTO(
      Bruker bruker, BrukerDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(bruker, dto, expandPaths, currentPath);

    dto.setEmail(bruker.getEmail());
    dto.setActive(bruker.isActive());
    dto.setLanguage(bruker.getLanguage().toString());

    return dto;
  }

  /**
   * Activate a new bruker
   *
   * @param bruker
   * @param secret
   * @return
   * @throws UnauthorizedException
   */
  @Transactional
  public BrukerDTO activate(String id, String secret) throws UnauthorizedException {
    var bruker = proxy.findById(id);

    if (!bruker.isActive()) {
      // Secret didn't match
      if (!bruker.getSecret().equals(secret)) {
        throw new UnauthorizedException("Invalid activation secret");
      }

      if (bruker.getSecretExpiry().isBefore(ZonedDateTime.now())) {
        throw new UnauthorizedException("Activation secret has expired");
      }

      bruker.setActive(true);
      bruker.setSecret(null);
      bruker.setSecretExpiry(null);
    }

    return toDTO(bruker);
  }

  /**
   * Request a password reset for bruker
   *
   * @param bruker
   * @return
   * @throws MessagingException
   */
  @Transactional
  public BrukerDTO requestPasswordReset(String id) throws MessagingException {
    var bruker = this.findById(id);
    var language = bruker.getLanguage();
    var context = new HashMap<String, Object>();

    var secret = IdGenerator.generate("usec");
    bruker.setSecret(secret);
    bruker.setSecretExpiry(ZonedDateTime.now().plusSeconds(userSecretExpirationTime));

    // TODO: Final URL will be different (not directly to the API)
    context.put("actionUrl", emailBaseUrl + "/bruker/" + bruker.getId() + "/setPassword/" + secret);
    mailSender.send(
        emailFrom, bruker.getEmail(), "userResetPassword", language.toString(), context);

    return proxy.toDTO(bruker);
  }

  /** Set password for bruker, validate secret */
  @Transactional
  public BrukerDTO updatePasswordWithSecret(
      String brukerId, String secret, PutBrukerPasswordWithSecretDTO requestBody)
      throws UnauthorizedException {
    var bruker = proxy.findById(brukerId);

    // Secret didn't match
    if (!bruker.getSecret().equals(secret)) {
      throw new UnauthorizedException("Invalid password reset token");
    }

    if (bruker.getSecretExpiry().isBefore(ZonedDateTime.now())) {
      throw new UnauthorizedException("Password reset token has expired");
    }

    bruker.setActive(true);
    bruker.setSecret(null);
    bruker.setSecretExpiry(null);

    var hashedPassword = passwordEncoder.encode(requestBody.getNewPassword());
    bruker.setPassword(hashedPassword);

    return proxy.toDTO(bruker);
  }

  /**
   * Set password for bruker, validate old password
   *
   * @param bruker
   * @param oldPassword
   * @param password
   * @throws Exception
   */
  @Transactional
  public BrukerDTO updatePassword(String brukerId, PutBrukerPasswordDTO requestBody)
      throws UnauthorizedException {

    var bruker = proxy.findById(brukerId);
    var currentPassword = bruker.getPassword();
    var oldPasswordRequest = requestBody.getOldPassword();
    var newPasswordRequest = requestBody.getNewPassword();

    if (!passwordEncoder.matches(oldPasswordRequest, currentPassword)) {
      throw new UnauthorizedException("Old password did not match");
    }

    var hashedPassword = passwordEncoder.encode(newPasswordRequest);
    bruker.setPassword(hashedPassword);

    return proxy.toDTO(bruker);
  }

  /**
   * Authenticate bruker
   *
   * @param bruker
   * @param password
   * @return
   */
  public boolean authenticate(@Nullable Bruker bruker, String password) {
    // @formatter:off
    return (bruker != null
        && bruker.isActive()
        && passwordEncoder.matches(password, bruker.getPassword()));
    // @formatter:on
  }

  /**
   * Send activation e-mail to bruker
   *
   * @param brukerJSON
   * @throws MessagingException
   * @throws Exception
   */
  protected void sendActivationEmail(Bruker bruker) throws MessagingException {
    var language = bruker.getLanguage();
    var context = new HashMap<String, Object>();

    // TODO: Final URL will be different (not directly to the API)
    context.put(
        "actionUrl",
        emailBaseUrl + "/bruker/" + bruker.getId() + "/activate/" + bruker.getSecret());
    mailSender.send(emailFrom, bruker.getEmail(), "userActivate", language.toString(), context);
  }

  /** Delete a bruker */
  @Override
  @Transactional
  public BrukerDTO delete(Bruker bruker) {
    var dto = newDTO();

    // Delete innsynskrav
    var innsynskravList = bruker.getInnsynskrav();
    if (innsynskravList != null) {
      innsynskravList.forEach(innsynskravService::delete);
      bruker.setInnsynskrav(List.of());
    }

    // Delete bruker
    repository.delete(bruker);

    dto.setDeleted(true);
    return dto;
  }

  //
  // Innsynskrav

  public ResultList<InnsynskravDTO> getInnsynskravList(
      String brukerId, InnsynskravListQueryDTO query) {
    query.setBruker(brukerId);
    var resultPage = innsynskravService.getPage(query);
    return innsynskravService.list(query, resultPage);
  }

  public InnsynskravDTO addInnsynskrav(String brukerId, InnsynskravDTO body) {
    body.setBruker(new ExpandableField<>(brukerId));
    return innsynskravService.add(body);
  }

  public BrukerDTO deleteInnsynskrav(String brukerId, String innsynskravId) {
    innsynskravService.delete(innsynskravId);
    var bruker = proxy.findById(brukerId);
    return proxy.toDTO(bruker);
  }

  //
  // Lagret sak

  public ResultList<LagretSakDTO> getLagretSakList(String brukerId, LagretSakListQueryDTO query) {
    query.setBruker(brukerId);
    var resultPage = lagretSakService.getPage(query);
    return lagretSakService.list(query, resultPage);
  }

  public LagretSakDTO addLagretSak(String brukerId, LagretSakDTO body) {
    body.setBruker(new ExpandableField<>(brukerId));
    return lagretSakService.add(body);
  }

  public BrukerDTO deleteLagretSak(String brukerId, String lagretSakId) {
    lagretSakService.delete(lagretSakId);
    var bruker = proxy.findById(brukerId);
    return proxy.toDTO(bruker);
  }

  //
  // Lagret soek

  public ResultList<LagretSoekDTO> getLagretSoekList(
      String brukerId, LagretSoekListQueryDTO query) {
    query.setBruker(brukerId);
    var resultPage = lagretSoekService.getPage(query);
    return lagretSoekService.list(query, resultPage);
  }

  public LagretSoekDTO addLagretSoek(String brukerId, LagretSoekDTO body) {
    body.setBruker(new ExpandableField<>(brukerId));
    return lagretSoekService.add(body);
  }

  public BrukerDTO deleteLagretSoek(String brukerId, String lagretSoekId) {
    lagretSoekService.delete(lagretSoekId);
    var bruker = proxy.findById(brukerId);
    return proxy.toDTO(bruker);
  }
}
