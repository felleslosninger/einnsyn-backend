package no.einnsyn.apiv3.entities.bruker;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.annotation.Nullable;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.authentication.bruker.models.BrukerUserDetails;
import no.einnsyn.apiv3.entities.EinnsynRepository;
import no.einnsyn.apiv3.entities.bruker.models.Bruker;
import no.einnsyn.apiv3.entities.bruker.models.BrukerJSON;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.innsynskrav.InnsynskravService;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravJSON;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelRepository;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelService;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelJSON;
import no.einnsyn.apiv3.exceptions.UnauthorizedException;
import no.einnsyn.apiv3.responses.ResponseList;
import no.einnsyn.apiv3.utils.IdGenerator;
import no.einnsyn.apiv3.utils.MailSender;

@Slf4j
@Service
public class BrukerService extends EinnsynObjectService<Bruker, BrukerJSON> {

  private final BrukerRepository repository;
  private final InnsynskravDelRepository innsynskravDelRepository;
  private final MailSender mailSender;
  private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Lazy
  @Resource
  private InnsynskravService innsynskravService;

  @Resource
  private InnsynskravDelService innsynskravDelService;

  @Value("${application.email.from}")
  private String emailFrom;

  @Value("${application.baseUrl}")
  private String emailBaseUrl;

  @Value("${application.userSecretExpirationTime}")
  private int userSecretExpirationTime;


  public BrukerService(BrukerRepository brukerRepository,
      InnsynskravDelRepository innsynskravDelRepository, MailSender mailSender) {
    super();
    this.repository = brukerRepository;
    this.innsynskravDelRepository = innsynskravDelRepository;
    this.mailSender = mailSender;
  }


  @Override
  protected EinnsynRepository<Bruker, ?> getRepository() {
    return repository;
  }


  @Override
  public BrukerJSON newJSON() {
    return new BrukerJSON();
  }


  @Override
  public Bruker newObject() {
    return new Bruker();
  }


  /**
   * Update or create a "bruker"
   * 
   * @param id
   * @param json
   * @return
   */
  @Override
  @Transactional
  public BrukerJSON update(String id, BrukerJSON json) {
    // Run regular update/insert procedure
    json = super.update(id, json);

    // Send activation email if this is an insert
    if (id == null) {
      var object = findById(json.getId());
      try {
        sendActivationEmail(object);
      } catch (Exception e) {
        log.error("Could not send activation email", e);
      }
    }

    return json;
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


  /**
   * Make findById also lookup by email
   */
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


  /**
   * Get bruker from authentication
   */
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
  public Bruker fromJSON(BrukerJSON json, Bruker bruker, Set<String> paths, String currentPath) {
    super.fromJSON(json, bruker, paths, currentPath);

    // This is an insert, create activation secret
    if (bruker.getId() == null) {
      String secret = IdGenerator.generate("usec");
      bruker.setSecret(secret);
      bruker.setSecretExpiry(ZonedDateTime.now().plusSeconds(userSecretExpirationTime));
    }

    if (json.getEmail() != null) {
      bruker.setEmail(json.getEmail());
    }

    if (json.getPassword() != null) {
      bruker.setPassword(passwordEncoder.encode(json.getPassword()));
    }

    if (json.getLanguage() != null) {
      bruker.setLanguage(json.getLanguage());
    }

    return bruker;
  }


  @Override
  @Transactional
  public BrukerJSON toJSON(Bruker bruker, BrukerJSON json, Set<String> expandPaths,
      String currentPath) {
    super.toJSON(bruker, json, expandPaths, currentPath);

    json.setEmail(bruker.getEmail());
    json.setActive(bruker.isActive());
    json.setLanguage(bruker.getLanguage());

    // Add innsynskrav
    var innsynskravList = bruker.getInnsynskrav();
    var innsynskravJSONList = new ArrayList<ExpandableField<InnsynskravJSON>>();
    if (innsynskravList != null) {
      innsynskravList.forEach(innsynskrav -> {
        var expandableField =
            innsynskravService.maybeExpand(innsynskrav, "innsynskrav", expandPaths, currentPath);
        innsynskravJSONList.add(expandableField);
      });
    }
    json.setInnsynskrav(innsynskravJSONList);

    // Add innsynskravDel
    var limit = 25;
    var innsynskravDelList =
        innsynskravDelRepository.findByBruker(bruker, PageRequest.of(0, limit + 1));
    var innsynskravDelFieldList = new ArrayList<ExpandableField<InnsynskravDelJSON>>();
    innsynskravDelList.forEach(innsynskravDel -> {
      var expandableField = innsynskravDelService.maybeExpand(innsynskravDel, "innsynskravDel",
          expandPaths, currentPath);
      innsynskravDelFieldList.add(expandableField);
    });
    var innsynskravDelResponseList =
        new ResponseList<ExpandableField<InnsynskravDelJSON>>(innsynskravDelFieldList, limit);
    json.setInnsynskravDel(innsynskravDelResponseList);

    return json;
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
  @SuppressWarnings("java:S6809") // this.toJSON() is OK since we're already in a transaction
  public BrukerJSON activate(Bruker bruker, String secret) throws UnauthorizedException {

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

    return toJSON(bruker);
  }


  /**
   * Set password for bruker, validate secret
   * 
   * 
   */
  @Transactional
  @SuppressWarnings("java:S6809") // this.toJSON() is OK since we're already in a transaction
  public BrukerJSON updatePasswordWithSecret(Bruker bruker, String secret, String password)
      throws UnauthorizedException {

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

    String hashedPassword = passwordEncoder.encode(password);
    bruker.setPassword(hashedPassword);

    return toJSON(bruker);
  }


  /**
   * Request a password reset for bruker
   * 
   * @param bruker
   * @return
   * @throws MessagingException
   */
  public void requestPasswordReset(Bruker bruker) throws MessagingException {
    var language = bruker.getLanguage();
    var context = new HashMap<String, Object>();

    String secret = IdGenerator.generate("usec");
    bruker.setSecret(secret);
    bruker.setSecretExpiry(ZonedDateTime.now().plusSeconds(userSecretExpirationTime));

    // TODO: Final URL will be different (not directly to the API)
    context.put("actionUrl", emailBaseUrl + "/bruker/" + bruker.getId() + "/setPassword/" + secret);
    mailSender.send(emailFrom, bruker.getEmail(), "userResetPassword", language, context);
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
  @SuppressWarnings("java:S6809") // this.toJSON() is OK since we're already in a transaction
  public BrukerJSON updatePasswordWithOldPassword(Bruker bruker, String oldPassword,
      String password) throws UnauthorizedException {

    var currentPassword = bruker.getPassword();

    // This should only happen if the user has never set a password
    if (currentPassword == null && (oldPassword == null || oldPassword.isEmpty())) {
      // Noop, don't throw
    }

    // Secret didn't match
    else if (!passwordEncoder.matches(oldPassword, currentPassword)) {
      throw new UnauthorizedException("Old password did not match");
    }

    String hashedPassword = passwordEncoder.encode(password);
    bruker.setPassword(hashedPassword);

    return toJSON(bruker);
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
    return (
      bruker != null &&
      bruker.isActive() &&
      passwordEncoder.matches(password, bruker.getPassword())
    );
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
    context.put("actionUrl",
        emailBaseUrl + "/bruker/" + bruker.getId() + "/activate/" + bruker.getSecret());
    mailSender.send(emailFrom, bruker.getEmail(), "userActivate", language, context);
  }


  /**
   * Delete a bruker
   */
  @Override
  @Transactional
  public BrukerJSON delete(Bruker bruker) {
    var json = newJSON();

    // Delete innsynskrav
    var innsynskravList = bruker.getInnsynskrav();
    if (innsynskravList != null) {
      innsynskravList.forEach(innsynskravService::delete);
      bruker.setInnsynskrav(List.of());
    }

    // Delete bruker
    repository.delete(bruker);

    json.setDeleted(true);
    return json;
  }

}
