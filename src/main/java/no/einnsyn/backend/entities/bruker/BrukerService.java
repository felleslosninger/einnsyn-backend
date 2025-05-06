package no.einnsyn.backend.entities.bruker;

import jakarta.annotation.Nullable;
import jakarta.mail.MessagingException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.exceptions.models.AuthorizationException;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.exceptions.models.InternalServerErrorException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.base.BaseService;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.bruker.models.Bruker;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import no.einnsyn.backend.entities.bruker.models.ListByBrukerParameters;
import no.einnsyn.backend.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.backend.entities.lagretsak.LagretSakRepository;
import no.einnsyn.backend.entities.lagretsak.models.LagretSakDTO;
import no.einnsyn.backend.entities.lagretsoek.LagretSoekRepository;
import no.einnsyn.backend.entities.lagretsoek.models.LagretSoekDTO;
import no.einnsyn.backend.utils.MailSender;
import no.einnsyn.backend.utils.idgenerator.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.util.Pair;
import org.springframework.retry.annotation.Retryable;
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
      throw new InternalServerErrorException("Unable to send activation email", e);
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
   * Extend findPropertyAndObjectByDTO to also lookup by email
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
        throw new AuthorizationException(
            "Password can only be updated by requesting a password reset");
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
   * @throws AuthorizationException if the secret is invalid
   */
  @Transactional(rollbackFor = Exception.class)
  @Retryable
  public BrukerDTO activate(String id, String secret) throws AuthorizationException {
    var bruker = proxy.findByIdOrThrow(id, AuthorizationException.class);

    if (!bruker.isActive()) {
      // Secret didn't match
      if (!bruker.getSecret().equals(secret)) {
        throw new AuthorizationException("Invalid activation secret");
      }

      if (bruker.getSecretExpiry().isBefore(ZonedDateTime.now())) {
        throw new AuthorizationException("Activation secret has expired");
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
  @Retryable
  public BrukerDTO requestPasswordReset(String id) throws EInnsynException {
    var bruker = brukerService.findByIdOrThrow(id);
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
      throw new InternalServerErrorException("Could not send password reset email", e);
    }

    return proxy.toDTO(bruker);
  }

  /** Set password for bruker, validate secret */
  @Transactional(rollbackFor = Exception.class)
  @Retryable
  public BrukerDTO updatePasswordWithSecret(
      String brukerId, String secret, BrukerController.UpdatePasswordWithSecret requestBody)
      throws AuthorizationException {
    var bruker = proxy.findByIdOrThrow(brukerId, AuthorizationException.class);

    // Secret didn't match
    if (!bruker.getSecret().equals(secret)) {
      throw new AuthorizationException("Invalid password reset token");
    }

    if (bruker.getSecretExpiry().isBefore(ZonedDateTime.now())) {
      throw new AuthorizationException("Password reset token has expired");
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
   * @throws AuthorizationException if the old password is invalid
   */
  @Transactional(rollbackFor = Exception.class)
  @Retryable
  public BrukerDTO updatePassword(String brukerId, BrukerController.UpdatePassword requestBody)
      throws AuthorizationException {

    var bruker = proxy.findByIdOrThrow(brukerId, AuthorizationException.class);
    var currentPassword = bruker.getPassword();
    var oldPasswordRequest = requestBody.getOldPassword();
    var newPasswordRequest = requestBody.getNewPassword();

    if (!passwordEncoder.matches(oldPasswordRequest, currentPassword)) {
      throw new AuthorizationException("Old password did not match");
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

    // Delete innsynskravBestilling
    var innsynskravBestillingList = bruker.getInnsynskravBestilling();
    if (innsynskravBestillingList != null) {
      for (var innsynskravBestilling : innsynskravBestillingList) {
        innsynskravBestillingService.delete(innsynskravBestilling.getId());
      }
      bruker.setInnsynskravBestilling(List.of());
    }

    // Delete all LagretSak
    try (var lagretSakIdStream = lagretSakRepository.streamIdByBrukerId(bruker.getId())) {
      var lagretSakIdIterator = lagretSakIdStream.iterator();
      while (lagretSakIdIterator.hasNext()) {
        lagretSakService.delete(lagretSakIdIterator.next());
      }
    }

    // Delete all LagretSoek
    try (var lagretSoekIdStream = lagretSoekRepository.streamIdByBrukerId(bruker.getId())) {
      var lagretSoekIdIterator = lagretSoekIdStream.iterator();
      while (lagretSoekIdIterator.hasNext()) {
        lagretSoekService.delete(lagretSoekIdIterator.next());
      }
    }

    super.deleteEntity(bruker);
  }

  //
  // InnsynskravBestilling

  public PaginatedList<InnsynskravBestillingDTO> listInnsynskravBestilling(
      String brukerId, ListByBrukerParameters query) throws EInnsynException {
    query.setBrukerId(brukerId);
    return innsynskravBestillingService.list(query);
  }

  public InnsynskravBestillingDTO addInnsynskravBestilling(
      String brukerId, InnsynskravBestillingDTO body) throws EInnsynException {
    body.setBruker(new ExpandableField<>(brukerId));
    return innsynskravBestillingService.add(body);
  }

  //
  // Lagret sak

  public PaginatedList<LagretSakDTO> listLagretSak(String brukerId, ListByBrukerParameters query)
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

  public PaginatedList<LagretSoekDTO> listLagretSoek(String brukerId, ListByBrukerParameters query)
      throws EInnsynException {
    query.setBrukerId(brukerId);
    return lagretSoekService.list(query);
  }

  public LagretSoekDTO addLagretSoek(String brukerId, LagretSoekDTO body) throws EInnsynException {
    body.setBruker(new ExpandableField<>(brukerId));
    return lagretSoekService.add(body);
  }

  protected PaginatedList<InnsynskravDTO> listInnsynskrav(
      String brukerId, ListByBrukerParameters query) throws EInnsynException {
    query.setBrukerId(brukerId);
    return innsynskravService.list(query);
  }

  /**
   * Only admin can list Bruker
   *
   * @throws AuthorizationException if not authorized
   */
  @Override
  public void authorizeList(ListParameters params) throws EInnsynException {
    if (!authenticationService.isAdmin()) {
      throw new AuthorizationException("Not authorized to list Bruker");
    }
  }

  /**
   * Only admin and self can get Bruker
   *
   * @param id the id of the bruker
   * @throws AuthorizationException if not authorized
   */
  @Override
  public void authorizeGet(String id) throws EInnsynException {
    var bruker = brukerService.findByIdOrThrow(id); // Lookup in case ID is email
    if (authenticationService.isAdmin() || authenticationService.isSelf(bruker.getId())) {
      return;
    }
    throw new AuthorizationException("Not authorized to get " + id);
  }

  /**
   * Anyone can add bruker
   *
   * @param dto the bruker to add
   * @throws AuthorizationException if not authorized
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
   * @throws AuthorizationException if not authorized
   */
  @Override
  public void authorizeUpdate(String id, BrukerDTO dto) throws EInnsynException {
    var bruker = brukerService.findByIdOrThrow(id); // Lookup in case ID is email
    if (authenticationService.isAdmin() || authenticationService.isSelf(bruker.getId())) {
      return;
    }
    throw new AuthorizationException("Not authorized to update " + id);
  }

  /**
   * Only admin and self can delete Bruker
   *
   * @param id the id of the bruker
   * @throws AuthorizationException if not authorized
   */
  @Override
  public void authorizeDelete(String id) throws EInnsynException {
    var bruker = brukerService.findByIdOrThrow(id); // Lookup in case ID is email
    if (authenticationService.isAdmin() || authenticationService.isSelf(bruker.getId())) {
      return;
    }
    throw new AuthorizationException("Not authorized to delete " + id);
  }
}
