package no.einnsyn.backend.entities.bruker;

import jakarta.annotation.Nullable;
import jakarta.mail.MessagingException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.exceptions.models.*;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.apikey.ApiKeyRepository;
import no.einnsyn.backend.entities.base.BaseService;
import no.einnsyn.backend.entities.base.UniqueFieldMatch;
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
import no.einnsyn.backend.utils.id.IdGenerator;
import no.einnsyn.backend.utils.mail.MailSenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class BrukerService extends BaseService<Bruker, BrukerDTO> {

  @Getter(onMethod_ = @Override)
  private final BrukerRepository repository;

  private final ApiKeyRepository apiKeyRepository;
  private final LagretSakRepository lagretSakRepository;
  private final LagretSoekRepository lagretSoekRepository;

  @SuppressWarnings("java:S6813")
  @Getter(onMethod_ = @Override)
  @Lazy
  @Autowired
  protected BrukerService proxy;

  private final MailSenderService mailSender;
  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Value("${application.email.from}")
  private String emailFrom;

  @Value("${application.baseUrl}")
  private String emailBaseUrl;

  @Value("${application.userSecretExpirationTime}")
  private int userSecretExpirationTime;

  public BrukerService(
      BrukerRepository brukerRepository,
      MailSenderService mailSender,
      ApiKeyRepository apiKeyRepository,
      LagretSakRepository lagretSakRepository,
      LagretSoekRepository lagretSoekRepository) {
    this.repository = brukerRepository;
    this.mailSender = mailSender;
    this.apiKeyRepository = apiKeyRepository;
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
   * Extend the update logic to send an activation email on insert.
   *
   * @param dto the DTO to update from
   * @return the updated object
   */
  @Override
  protected Bruker addEntity(BrukerDTO dto) throws EInnsynException {
    var bruker = super.addEntity(dto);

    this.startEmailVerification(bruker);

    return bruker;
  }

  @Override
  protected Bruker updateEntity(Bruker bruker, BrukerDTO dto) throws EInnsynException {
    var oldRequestedEmail = bruker.getRequestedEmail();

    bruker = super.updateEntity(bruker, dto);

    var newRequestedEmail = bruker.getRequestedEmail();
    if (newRequestedEmail != null && !newRequestedEmail.equals(oldRequestedEmail)) {
      this.startEmailVerification(bruker);
    }

    return bruker;
  }

  /**
   * Extend find to also look up by email.
   *
   * @param id the id to lookup
   * @return the object
   */
  @Override
  @Transactional(readOnly = true)
  public Bruker find(String id) {
    // Try to lookup by email if id contains @
    if (id != null && id.contains("@")) {
      var bruker = repository.findByEmail(id.toLowerCase());
      if (bruker != null) {
        return bruker;
      }
    }
    return super.find(id);
  }

  /**
   * Extend unique-field lookup to also look up by email.
   *
   * @param baseDTO the DTO to find
   * @return the object with the given email, or null if not found
   */
  @Override
  @Transactional(readOnly = true)
  public UniqueFieldMatch<Bruker> findUniqueFieldMatch(BaseDTO baseDTO) {
    if (baseDTO instanceof BrukerDTO dto && dto.getEmail() != null) {
      var bruker = repository.findByEmail(dto.getEmail().toLowerCase());
      if (bruker != null) {
        return new UniqueFieldMatch<>("email", bruker);
      }
    }
    return super.findUniqueFieldMatch(baseDTO);
  }

  @Override
  protected Bruker fromDTO(BrukerDTO dto, Bruker bruker) throws EInnsynException {
    super.fromDTO(dto, bruker);

    if (dto.getEmail() != null) {
      var newEmail = dto.getEmail().toLowerCase();
      if (authenticationService.isAdmin()) {
        if (bruker.getId() != null) {
          assertEmailAvailable(newEmail, bruker);
        }
        bruker.setEmail(newEmail);
      } else if (bruker.getEmail() == null) {
        bruker.setRequestedEmail(newEmail);
        bruker.setEmail(newEmail);
      } else {
        assertEmailAvailable(newEmail, bruker);
        bruker.setRequestedEmail(newEmail);
      }
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
  public BrukerDTO validateEmail(String id, String secret) throws EInnsynException {
    var bruker = proxy.findOrThrow(id, AuthorizationException.class);

    if (bruker.getValidateEmailSecret() == null
        || !bruker.getValidateEmailSecret().equals(secret)) {
      throw new AuthorizationException("Invalid activation secret");
    }

    if (bruker.getValidateEmailSecretExpiry().isBefore(ZonedDateTime.now())) {
      throw new AuthorizationException("Activation secret has expired");
    }

    var oldEmail = bruker.getEmail();
    var newEmail = bruker.getRequestedEmail();

    if (newEmail != null) {
      assertEmailAvailable(newEmail, bruker);
    }

    bruker.setActive(true);
    bruker.setValidateEmailSecret(null);
    bruker.setValidateEmailSecretExpiry(null);

    if (newEmail != null) {
      bruker.setEmail(newEmail);
      bruker.setBrukernavn(newEmail); // Keep legacy username in sync
      bruker.setRequestedEmail(null);

      // Notify both addresses when an existing, verified email address is changed
      if (oldEmail != null && !oldEmail.equalsIgnoreCase(newEmail)) {
        sendEmailChangeReceipt(bruker, oldEmail, newEmail);
      }
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
    var bruker = brukerService.findOrThrow(id);
    var language = bruker.getLanguage();
    var context = new HashMap<String, Object>();

    var secret = IdGenerator.generateSecret("usec");
    bruker.setResetPasswordSecret(secret);
    bruker.setResetPasswordSecretExpiry(ZonedDateTime.now().plusSeconds(userSecretExpirationTime));

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
    var bruker = proxy.findOrThrow(brukerId, AuthorizationException.class);

    // Secret didn't match
    if (bruker.getResetPasswordSecret() == null
        || !bruker.getResetPasswordSecret().equals(secret)) {
      throw new AuthorizationException("Invalid password reset token");
    }

    if (bruker.getResetPasswordSecretExpiry().isBefore(ZonedDateTime.now())) {
      throw new AuthorizationException("Password reset token has expired");
    }

    bruker.setActive(true);
    bruker.setResetPasswordSecret(null);
    bruker.setResetPasswordSecretExpiry(null);
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

    var bruker = proxy.findOrThrow(brukerId, AuthorizationException.class);
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

  private void startEmailVerification(Bruker bruker) throws EInnsynException {
    bruker.setValidateEmailSecret(IdGenerator.generateSecret("usec"));
    bruker.setValidateEmailSecretExpiry(ZonedDateTime.now().plusSeconds(userSecretExpirationTime));
    sendEmailVerification(bruker);
  }

  /**
   * Send email verification e-mail to bruker.
   *
   * @param bruker the bruker to send the e-mail to
   * @throws EInnsynException if the e-mail could not be sent
   */
  private void sendEmailVerification(Bruker bruker) throws EInnsynException {
    if (bruker.getRequestedEmail() == null) {
      return;
    }

    var language = bruker.getLanguage();
    var context = new HashMap<String, Object>();

    // TODO: Final URL will be different (not directly to the API)
    context.put(
        "actionUrl",
        emailBaseUrl
            + "/bruker/"
            + bruker.getId()
            + "/activate/"
            + bruker.getValidateEmailSecret());

    try {
      log.debug("Sending email verification to {}", bruker.getRequestedEmail());
      mailSender.send(emailFrom, bruker.getRequestedEmail(), "userConfirmEmail", language, context);
    } catch (MessagingException e) {
      throw new InternalServerErrorException("Could not send email verification mail", e);
    }
  }

  /**
   * Send a receipt to both the old and the new address after an email change, so a user is notified
   * if someone else changes the address on their account.
   *
   * @param bruker the bruker whose email was changed
   * @param oldEmail the previous email address
   * @param newEmail the new, now verified, email address
   * @throws EInnsynException if the receipt could not be sent
   */
  private void sendEmailChangeReceipt(Bruker bruker, String oldEmail, String newEmail)
      throws EInnsynException {
    var language = bruker.getLanguage();
    var context = new HashMap<String, Object>();
    context.put("oldEmail", oldEmail);
    context.put("newEmail", newEmail);

    try {
      log.debug("Sending email change receipt to {} and {}", oldEmail, newEmail);
      mailSender.send(emailFrom, newEmail, "userEmailChangeReceipt", language, context);
      mailSender.send(emailFrom, oldEmail, "userEmailChangeReceipt", language, context);
    } catch (MessagingException e) {
      throw new InternalServerErrorException("Could not send email change receipt", e);
    }
  }

  /**
   * Throw a ConflictException if the given email is already used by another bruker.
   *
   * @param email the email address to check (lower-cased)
   * @param self the bruker the email is being set on, excluded from the check
   * @throws ConflictException if the email belongs to a different bruker
   */
  private void assertEmailAvailable(String email, Bruker self) throws ConflictException {
    var existing = brukerService.find(email);
    if (existing != null && !existing.getId().equals(self.getId())) {
      throw new ConflictException("Email already exists on another user: " + email);
    }
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

    // Delete API keys
    try (var apiKeyIdStream = apiKeyRepository.streamIdByBrukerId(bruker.getId())) {
      var apiKeyIdIterator = apiKeyIdStream.iterator();
      while (apiKeyIdIterator.hasNext()) {
        apiKeyService.delete(apiKeyIdIterator.next());
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
    var bruker = brukerService.findOrThrow(id); // Lookup in case ID is email
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
    var bruker = brukerService.findOrThrow(id); // Lookup in case ID is email
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
    var bruker = brukerService.findOrThrow(id); // Lookup in case ID is email
    if (authenticationService.isAdmin() || authenticationService.isSelf(bruker.getId())) {
      return;
    }
    throw new AuthorizationException("Not authorized to delete " + id);
  }
}
