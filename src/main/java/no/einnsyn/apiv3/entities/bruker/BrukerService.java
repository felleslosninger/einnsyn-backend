package no.einnsyn.apiv3.entities.bruker;

import jakarta.annotation.Nullable;
import jakarta.mail.MessagingException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.bruker.BrukerController.PatchBrukerPasswordDTO;
import no.einnsyn.apiv3.entities.bruker.BrukerController.PatchBrukerPasswordWithSecretDTO;
import no.einnsyn.apiv3.entities.bruker.models.Bruker;
import no.einnsyn.apiv3.entities.bruker.models.BrukerDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravListQueryDTO;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelDTO;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelListQueryDTO;
import no.einnsyn.apiv3.entities.lagretsak.LagretSakRepository;
import no.einnsyn.apiv3.entities.lagretsak.models.LagretSakDTO;
import no.einnsyn.apiv3.entities.lagretsak.models.LagretSakListQueryDTO;
import no.einnsyn.apiv3.entities.lagretsoek.LagretSoekRepository;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoekDTO;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoekListQueryDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.error.exceptions.ForbiddenException;
import no.einnsyn.apiv3.utils.MailSender;
import no.einnsyn.apiv3.utils.idgenerator.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.util.Pair;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class BrukerService extends BaseService<Bruker, BrukerDTO> {

  @Getter private final BrukerRepository repository;

  private final LagretSakRepository lagretSakRepository;
  private final LagretSoekRepository lagretSoekRepository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  protected BrukerService proxy;

  private final MailSender mailSender;
  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Value("${application.email.from}")
  private String emailFrom;

  @Value("${application.baseUrl}")
  private String emailBaseUrl;

  @Value("${application.userSecretExpirationTime}")
  private int userSecretExpirationTime;

  public BrukerService(
      BrukerRepository brukerRepository,
      MailSender mailSender,
      LagretSakRepository lagretSakRepository,
      LagretSoekRepository lagretSoekRepository) {
    this.repository = brukerRepository;
    this.mailSender = mailSender;
    this.lagretSakRepository = lagretSakRepository;
    this.lagretSoekRepository = lagretSoekRepository;
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
   * Extend the update logic to send an activation email on insert
   *
   * @param dto the DTO to update from
   * @return the updated object
   */
  @Override
  protected Bruker addEntity(BrukerDTO dto) throws EInnsynException {
    var bruker = super.addEntity(dto);

    // Send activation email
    try {
      log.debug("Sending activation email to {}", dto.getEmail());
      this.sendActivationEmail(bruker);
    } catch (MessagingException e) {
      throw new EInnsynException("Unable to send activation email", e);
    }

    return bruker;
  }

  /**
   * Extend findById to also lookup by email
   *
   * @param id the id to lookup
   * @return the object
   */
  @Override
  @Transactional(readOnly = true)
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

  /**
   * Extend findByDTO to also lookup by email
   *
   * @param dto the DTO to find
   * @return the object with the given email, or null if not found
   */
  @Override
  @Transactional(readOnly = true)
  public Pair<String, Bruker> findPropertyAndObjectByDTO(BaseDTO baseDTO) {
    if (baseDTO instanceof BrukerDTO dto && dto.getEmail() != null) {
      var bruker = repository.findByEmail(dto.getEmail());
      if (bruker != null) {
        return Pair.of("email", bruker);
      }
    }
    return super.findPropertyAndObjectByDTO(baseDTO);
  }

  @Override
  protected Bruker fromDTO(BrukerDTO dto, Bruker bruker) throws EInnsynException {
    super.fromDTO(dto, bruker);

    // This is an insert, create activation secret
    if (bruker.getId() == null) {
      var secret = IdGenerator.generateSecret("usec");
      bruker.setSecret(secret);
      bruker.setSecretExpiry(ZonedDateTime.now().plusSeconds(userSecretExpirationTime));
    }

    if (dto.getEmail() != null) {
      bruker.setEmail(dto.getEmail());
    }

    if (dto.getLanguage() != null) {
      bruker.setLanguage(dto.getLanguage());
    }

    if (dto.getPassword() != null) {
      // Only allow setting password without any confirmation on insert
      if (bruker.getPassword() == null) {
        this.setPassword(bruker, dto.getPassword());
      } else {
        throw new ForbiddenException("Password can only be updated by requesting a password reset");
      }
    }

    return bruker;
  }

  @Override
  protected BrukerDTO toDTO(
      Bruker bruker, BrukerDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(bruker, dto, expandPaths, currentPath);

    dto.setEmail(bruker.getEmail());
    dto.setActive(bruker.isActive());
    dto.setLanguage(bruker.getLanguage());

    return dto;
  }

  /**
   * Activate a new bruker
   *
   * @param id the id of the bruker
   * @param secret the activation secret
   * @return the updated bruker
   * @throws ForbiddenException if the secret is invalid
   */
  @Transactional(rollbackFor = Exception.class)
  public BrukerDTO activate(String id, String secret) throws ForbiddenException {
    var bruker = proxy.findById(id);

    if (!bruker.isActive()) {
      // Secret didn't match
      if (!bruker.getSecret().equals(secret)) {
        throw new ForbiddenException("Invalid activation secret");
      }

      if (bruker.getSecretExpiry().isBefore(ZonedDateTime.now())) {
        throw new ForbiddenException("Activation secret has expired");
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
   * @param id the id of the bruker
   * @return the updated bruker
   * @throws EInnsynException if the email could not be sent
   */
  @Transactional(rollbackFor = Exception.class)
  public BrukerDTO requestPasswordReset(String id) throws EInnsynException {
    var bruker = brukerService.findById(id);
    var language = bruker.getLanguage();
    var context = new HashMap<String, Object>();

    var secret = IdGenerator.generateSecret("usec");
    bruker.setSecret(secret);
    bruker.setSecretExpiry(ZonedDateTime.now().plusSeconds(userSecretExpirationTime));

    // TODO: Final URL will be different (not directly to the API)
    context.put("actionUrl", emailBaseUrl + "/bruker/" + bruker.getId() + "/setPassword/" + secret);
    try {
      log.debug("Sending password reset email to {}", bruker.getEmail());
      mailSender.send(emailFrom, bruker.getEmail(), "userResetPassword", language, context);
    } catch (MessagingException e) {
      throw new EInnsynException("Could not send password reset email", e);
    }

    return proxy.toDTO(bruker);
  }

  /** Set password for bruker, validate secret */
  @Transactional(rollbackFor = Exception.class)
  public BrukerDTO updatePasswordWithSecret(
      String brukerId, String secret, PatchBrukerPasswordWithSecretDTO requestBody)
      throws ForbiddenException {
    var bruker = proxy.findById(brukerId);

    // Secret didn't match
    if (!bruker.getSecret().equals(secret)) {
      throw new ForbiddenException("Invalid password reset token");
    }

    if (bruker.getSecretExpiry().isBefore(ZonedDateTime.now())) {
      throw new ForbiddenException("Password reset token has expired");
    }

    bruker.setActive(true);
    bruker.setSecret(null);
    bruker.setSecretExpiry(null);
    this.setPassword(bruker, requestBody.getNewPassword());

    return proxy.toDTO(bruker);
  }

  /**
   * Set password for bruker
   *
   * @param bruker the bruker to set the password for
   * @param password the password to set
   */
  private void setPassword(Bruker bruker, String password) {
    var hashedPassword = passwordEncoder.encode(password);
    bruker.setPassword(hashedPassword);
  }

  /**
   * Set password for bruker, validate old password
   *
   * @param brukerId the id of the bruker
   * @param requestBody the request body containing the old and new password
   * @throws ForbiddenException if the old password is invalid
   */
  @Transactional(rollbackFor = Exception.class)
  public BrukerDTO updatePassword(String brukerId, PatchBrukerPasswordDTO requestBody)
      throws ForbiddenException {

    var bruker = proxy.findById(brukerId);
    var currentPassword = bruker.getPassword();
    var oldPasswordRequest = requestBody.getOldPassword();
    var newPasswordRequest = requestBody.getNewPassword();

    if (!passwordEncoder.matches(oldPasswordRequest, currentPassword)) {
      throw new ForbiddenException("Old password did not match");
    }

    this.setPassword(bruker, newPasswordRequest);

    return proxy.toDTO(bruker);
  }

  /**
   * Authenticate bruker
   *
   * @param bruker the bruker to authenticate
   * @param password the password to authenticate with
   * @return true if the password matches
   */
  public boolean authenticate(@Nullable Bruker bruker, String password) {
    return (bruker != null
        && bruker.isActive()
        && passwordEncoder.matches(password, bruker.getPassword()));
  }

  /**
   * Send activation e-mail to bruker
   *
   * @param bruker the bruker to send the e-mail to
   * @throws MessagingException if the e-mail could not be sent
   */
  private void sendActivationEmail(Bruker bruker) throws MessagingException {
    var language = bruker.getLanguage();
    var context = new HashMap<String, Object>();

    // TODO: Final URL will be different (not directly to the API)
    context.put(
        "actionUrl",
        emailBaseUrl + "/bruker/" + bruker.getId() + "/activate/" + bruker.getSecret());

    log.debug("Sending activation email to {}", bruker.getEmail());
    mailSender.send(emailFrom, bruker.getEmail(), "userActivate", language, context);
  }

  @Override
  protected void deleteEntity(Bruker bruker) throws EInnsynException {

    // Delete innsynskrav
    var innsynskravList = bruker.getInnsynskrav();
    if (innsynskravList != null) {
      for (var innsynskrav : innsynskravList) {
        innsynskravService.delete(innsynskrav.getId());
      }
      bruker.setInnsynskrav(List.of());
    }

    // Delete all LagretSak
    var lagretSakStream = lagretSakRepository.findByBruker(bruker.getId());
    var lagretSakIterator = lagretSakStream.iterator();
    while (lagretSakIterator.hasNext()) {
      var lagretSak = lagretSakIterator.next();
      lagretSakService.delete(lagretSak.getId());
    }

    // Delete all LagretSoek
    var lagretSoekStream = lagretSoekRepository.findByBruker(bruker.getId());
    var lagretSoekIterator = lagretSoekStream.iterator();
    while (lagretSoekIterator.hasNext()) {
      var lagretSoek = lagretSoekIterator.next();
      lagretSoekService.delete(lagretSoek.getId());
    }

    super.deleteEntity(bruker);
  }

  //
  // Innsynskrav

  public ResultList<InnsynskravDTO> getInnsynskravList(
      String brukerId, InnsynskravListQueryDTO query) throws EInnsynException {
    query.setBrukerId(brukerId);
    return innsynskravService.list(query);
  }

  public InnsynskravDTO addInnsynskrav(String brukerId, InnsynskravDTO body)
      throws EInnsynException {
    body.setBruker(new ExpandableField<>(brukerId));
    return innsynskravService.add(body);
  }

  //
  // Lagret sak

  public ResultList<LagretSakDTO> getLagretSakList(String brukerId, LagretSakListQueryDTO query)
      throws EInnsynException {
    query.setBrukerId(brukerId);
    return lagretSakService.list(query);
  }

  public LagretSakDTO addLagretSak(String brukerId, LagretSakDTO body) throws EInnsynException {
    body.setBruker(new ExpandableField<>(brukerId));
    return lagretSakService.add(body);
  }

  //
  // Lagret soek

  public ResultList<LagretSoekDTO> getLagretSoekList(String brukerId, LagretSoekListQueryDTO query)
      throws EInnsynException {
    query.setBrukerId(brukerId);
    return lagretSoekService.list(query);
  }

  public LagretSoekDTO addLagretSoek(String brukerId, LagretSoekDTO body) throws EInnsynException {
    body.setBruker(new ExpandableField<>(brukerId));
    return lagretSoekService.add(body);
  }

  protected ResultList<InnsynskravDelDTO> getInnsynskravDelList(
      String brukerId, InnsynskravDelListQueryDTO query) throws EInnsynException {
    query.setBrukerId(brukerId);
    return innsynskravDelService.list(query);
  }

  /**
   * Only admin can list Bruker
   *
   * @throws ForbiddenException if not authorized
   */
  @Override
  public void authorizeList(BaseListQueryDTO params) throws EInnsynException {
    if (!authenticationService.isAdmin()) {
      throw new ForbiddenException("Not authorized to list Bruker");
    }
  }

  /**
   * Only admin and self can get Bruker
   *
   * @param id the id of the bruker
   * @throws ForbiddenException if not authorized
   */
  @Override
  public void authorizeGet(String id) throws EInnsynException {
    var bruker = brukerService.findById(id); // Lookup in case ID is email
    if (authenticationService.isAdmin() || authenticationService.isSelf(bruker.getId())) {
      return;
    }
    throw new ForbiddenException("Not authorized to get " + id);
  }

  /**
   * Anyone can add bruker
   *
   * @param dto the bruker to add
   * @throws ForbiddenException if not authorized
   */
  @Override
  public void authorizeAdd(BrukerDTO dto) throws EInnsynException {
    // No authorization needed
  }

  /**
   * Only admin and self can update Bruker
   *
   * @param id the id of the bruker
   * @param dto the updated bruker
   * @throws ForbiddenException if not authorized
   */
  @Override
  public void authorizeUpdate(String id, BrukerDTO dto) throws EInnsynException {
    var bruker = brukerService.findById(id); // Lookup in case ID is email
    if (authenticationService.isAdmin() || authenticationService.isSelf(bruker.getId())) {
      return;
    }
    throw new ForbiddenException("Not authorized to update " + id);
  }

  /**
   * Only admin and self can delete Bruker
   *
   * @param id the id of the bruker
   * @throws ForbiddenException if not authorized
   */
  @Override
  public void authorizeDelete(String id) throws EInnsynException {
    var bruker = brukerService.findById(id); // Lookup in case ID is email
    if (authenticationService.isAdmin() || authenticationService.isSelf(bruker.getId())) {
      return;
    }
    throw new ForbiddenException("Not authorized to delete " + id);
  }
}
