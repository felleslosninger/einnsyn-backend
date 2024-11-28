package no.einnsyn.backend.entities.apikey;

import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.paginators.Paginators;
import no.einnsyn.backend.entities.apikey.models.ApiKey;
import no.einnsyn.backend.entities.apikey.models.ApiKeyDTO;
import no.einnsyn.backend.entities.apikey.models.ApiKeyListQueryDTO;
import no.einnsyn.backend.entities.base.BaseService;
import no.einnsyn.backend.entities.base.models.BaseListQueryDTO;
import no.einnsyn.backend.error.exceptions.EInnsynException;
import no.einnsyn.backend.error.exceptions.ForbiddenException;
import no.einnsyn.backend.utils.idgenerator.IdGenerator;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ApiKeyService extends BaseService<ApiKey, ApiKeyDTO> {

  @Getter private final ApiKeyRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private ApiKeyService proxy;

  public ApiKeyService(ApiKeyRepository repository) {
    this.repository = repository;
  }

  public ApiKey newObject() {
    return new ApiKey();
  }

  public ApiKeyDTO newDTO() {
    return new ApiKeyDTO();
  }

  /**
   * Override add(), to add secretKey on creation
   *
   * @param dto The DTO to add
   * @return The added DTO
   * @throws EInnsynException If the operation fails
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  @Retryable(
      retryFor = {DataIntegrityViolationException.class, OptimisticLockingFailureException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000))
  public ApiKeyDTO add(ApiKeyDTO dto) throws EInnsynException {
    // Generate a new secret
    var secret = IdGenerator.generateSecret("secret");
    dto.setSecretKey(secret);
    var apiKeyDTO = super.add(dto);
    apiKeyDTO.setSecretKey(secret);
    return apiKeyDTO;
  }

  @Override
  protected Paginators<ApiKey> getPaginators(BaseListQueryDTO params) {
    if (params instanceof ApiKeyListQueryDTO p && p.getEnhetId() != null) {
      var enhet = enhetService.findById(p.getEnhetId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(enhet, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(enhet, pivot, pageRequest));
    }
    return super.getPaginators(params);
  }

  @Override
  protected ApiKey fromDTO(ApiKeyDTO dto, ApiKey apiKey) throws EInnsynException {
    super.fromDTO(dto, apiKey);

    // This is a readOnly field, but we set it internally in add().
    if (dto.getSecretKey() != null) {
      var hashedSecret = DigestUtils.sha256Hex(dto.getSecretKey());
      apiKey.setSecret(hashedSecret);
      log.trace("apiKey.setSecretKey(" + hashedSecret + ")");
    }

    if (dto.getName() != null) {
      apiKey.setName(dto.getName());
      log.trace("apiKey.setName(" + apiKey.getName() + ")");
    }

    if (dto.getEnhet() != null) {
      var enhetId = dto.getEnhet().getId();
      var enhet = enhetService.findById(enhetId);
      apiKey.setEnhet(enhet);
      log.trace("apiKey.setEnhet(" + apiKey.getEnhet() + ")");
    }

    return apiKey;
  }

  @Override
  protected ApiKeyDTO toDTO(
      ApiKey object, ApiKeyDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(object, dto, expandPaths, currentPath);

    dto.setName(object.getName());
    dto.setEnhet(enhetService.maybeExpand(object.getEnhet(), "enhet", expandPaths, currentPath));

    return dto;
  }

  @Transactional
  public ApiKey findBySecretKey(String secretKey) {
    var hashedSecretKey = DigestUtils.sha256Hex(secretKey);
    return repository.findBySecret(hashedSecretKey);
  }

  /**
   * Authorize the list operation. Admins and users with access to the given enhet can list ApiKeys.
   *
   * @param params The list query
   * @throws ForbiddenException If the user is not authorized
   */
  @Override
  protected void authorizeList(BaseListQueryDTO params) throws EInnsynException {
    if (authenticationService.isAdmin()) {
      return;
    }

    var loggedInAs = authenticationService.getJournalenhetId();
    if (params instanceof ApiKeyListQueryDTO p
        && p.getEnhetId() != null
        && enhetService.isAncestorOf(loggedInAs, p.getEnhetId())) {
      return;
    }

    throw new ForbiddenException("Not authorized to list ApiKeys");
  }

  /**
   * Authorize the get operation. Admins and users with access to the given enhet can get ApiKeys.
   *
   * @param id The id of the object to get
   * @throws ForbiddenException If the user is not authorized
   */
  @Override
  protected void authorizeGet(String id) throws EInnsynException {
    var loggedInAs = authenticationService.getJournalenhetId();
    var apiKey = apiKeyService.findById(id);
    if (!enhetService.isAncestorOf(loggedInAs, apiKey.getEnhet().getId())) {
      throw new ForbiddenException("Not authorized to get " + id);
    }
  }

  /**
   * Authorize the add operation. Only users with a journalenhet can add ApiKeys.
   *
   * @param dto The DTO to add
   * @throws ForbiddenException If the user is not authorized
   */
  @Override
  protected void authorizeAdd(ApiKeyDTO dto) throws EInnsynException {
    var loggedInAs = authenticationService.getJournalenhetId();
    if (loggedInAs == null) {
      throw new ForbiddenException("Not authenticated to add ApiKey.");
    }

    var apiKeyEnhetId = dto.getEnhet().getId();
    if (apiKeyEnhetId == null) {
      throw new ForbiddenException("EnhetId is required");
    }

    if (!enhetService.isAncestorOf(loggedInAs, apiKeyEnhetId)) {
      throw new ForbiddenException("Not authorized to add ApiKey");
    }
  }

  /**
   * Authorize the update operation. Only users representing a journalenhet that owns the object can
   * update.
   *
   * @param id The id of the object to update
   * @param dto The DTO to update
   * @throws ForbiddenException If the user is not authorized
   */
  @Override
  protected void authorizeUpdate(String id, ApiKeyDTO dto) throws EInnsynException {
    var loggedInAs = authenticationService.getJournalenhetId();

    // Make sure we're not changing the Enhet to one we're not authorized to
    if (dto.getEnhet() != null && !enhetService.isAncestorOf(loggedInAs, dto.getEnhet().getId())) {
      throw new ForbiddenException("Not authorized set Enhet to " + dto.getEnhet().getId());
    }

    var wantsToUpdate = apiKeyService.findById(id);
    if (!enhetService.isAncestorOf(loggedInAs, wantsToUpdate.getEnhet().getId())) {
      throw new ForbiddenException("Not authorized to update " + id);
    }
  }

  /**
   * Authorize the delete operation. Only users representing a journalenhet that owns the object can
   * delete.
   *
   * @param id The id of the object to delete
   * @throws ForbiddenException If the user is not authorized
   */
  @Override
  protected void authorizeDelete(String id) throws EInnsynException {
    var loggedInAs = authenticationService.getJournalenhetId();
    var wantsToDelete = apiKeyService.findById(id);
    if (!enhetService.isAncestorOf(loggedInAs, wantsToDelete.getEnhet().getId())) {
      throw new ForbiddenException("Not authorized to delete " + id);
    }
  }
}
