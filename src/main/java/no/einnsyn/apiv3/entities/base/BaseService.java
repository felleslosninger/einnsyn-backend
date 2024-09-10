package no.einnsyn.apiv3.entities.base;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.google.gson.Gson;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.annotation.NewSpan;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import no.einnsyn.apiv3.authentication.AuthenticationService;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.indexable.Indexable;
import no.einnsyn.apiv3.common.indexable.IndexableRepository;
import no.einnsyn.apiv3.common.paginators.Paginators;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.apikey.ApiKeyService;
import no.einnsyn.apiv3.entities.arkiv.ArkivService;
import no.einnsyn.apiv3.entities.arkivdel.ArkivdelService;
import no.einnsyn.apiv3.entities.base.models.Base;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import no.einnsyn.apiv3.entities.base.models.BaseES;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.behandlingsprotokoll.BehandlingsprotokollService;
import no.einnsyn.apiv3.entities.bruker.BrukerService;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.DokumentbeskrivelseService;
import no.einnsyn.apiv3.entities.dokumentobjekt.DokumentobjektService;
import no.einnsyn.apiv3.entities.enhet.EnhetService;
import no.einnsyn.apiv3.entities.identifikator.IdentifikatorService;
import no.einnsyn.apiv3.entities.innsynskrav.InnsynskravService;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelService;
import no.einnsyn.apiv3.entities.journalpost.JournalpostService;
import no.einnsyn.apiv3.entities.klasse.KlasseService;
import no.einnsyn.apiv3.entities.klassifikasjonssystem.KlassifikasjonssystemService;
import no.einnsyn.apiv3.entities.korrespondansepart.KorrespondansepartService;
import no.einnsyn.apiv3.entities.lagretsak.LagretSakService;
import no.einnsyn.apiv3.entities.lagretsoek.LagretSoekService;
import no.einnsyn.apiv3.entities.moetedeltaker.MoetedeltakerService;
import no.einnsyn.apiv3.entities.moetedokument.MoetedokumentService;
import no.einnsyn.apiv3.entities.moetemappe.MoetemappeService;
import no.einnsyn.apiv3.entities.moetesak.MoetesakService;
import no.einnsyn.apiv3.entities.moetesaksbeskrivelse.MoetesaksbeskrivelseService;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeService;
import no.einnsyn.apiv3.entities.skjerming.SkjermingService;
import no.einnsyn.apiv3.entities.tilbakemelding.TilbakemeldingService;
import no.einnsyn.apiv3.entities.utredning.UtredningService;
import no.einnsyn.apiv3.entities.vedtak.VedtakService;
import no.einnsyn.apiv3.entities.votering.VoteringService;
import no.einnsyn.apiv3.error.exceptions.ConflictException;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.error.exceptions.ForbiddenException;
import no.einnsyn.apiv3.error.exceptions.NotFoundException;
import no.einnsyn.apiv3.tasks.elasticsearch.ElasticsearchIndexQueue;
import no.einnsyn.apiv3.utils.ExpandPathResolver;
import no.einnsyn.apiv3.utils.idgenerator.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Abstract base service class providing generic functionalities for entity services. This class is
 * designed to be extended by entity service implementations, providing a common framework for
 * handling entities and their data transfer objects (DTOs).
 *
 * @param <O> the type of the entity object
 * @param <D> the type of the data transfer object (DTO)
 */
@SuppressWarnings({"java:S6813", "java:S1192", "LoggingPlaceholderCountMatchesArgumentCount"})
@Slf4j
public abstract class BaseService<O extends Base, D extends BaseDTO> {

  @Lazy @Autowired protected ApiKeyService apiKeyService;
  @Lazy @Autowired protected ArkivService arkivService;
  @Lazy @Autowired protected ArkivdelService arkivdelService;
  @Lazy @Autowired protected BehandlingsprotokollService behandlingsprotokollService;
  @Lazy @Autowired protected BrukerService brukerService;
  @Lazy @Autowired protected DokumentbeskrivelseService dokumentbeskrivelseService;
  @Lazy @Autowired protected DokumentobjektService dokumentobjektService;
  @Lazy @Autowired protected EnhetService enhetService;
  @Lazy @Autowired protected IdentifikatorService identifikatorService;
  @Lazy @Autowired protected InnsynskravService innsynskravService;
  @Lazy @Autowired protected InnsynskravDelService innsynskravDelService;
  @Lazy @Autowired protected JournalpostService journalpostService;
  @Lazy @Autowired protected KlasseService klasseService;
  @Lazy @Autowired protected KlassifikasjonssystemService klassifikasjonssystemService;
  @Lazy @Autowired protected KorrespondansepartService korrespondansepartService;
  @Lazy @Autowired protected LagretSakService lagretSakService;
  @Lazy @Autowired protected LagretSoekService lagretSoekService;
  @Lazy @Autowired protected MoetedeltakerService moetedeltakerService;
  @Lazy @Autowired protected MoetedokumentService moetedokumentService;
  @Lazy @Autowired protected MoetemappeService moetemappeService;
  @Lazy @Autowired protected MoetesakService moetesakService;
  @Lazy @Autowired protected MoetesaksbeskrivelseService moetesaksbeskrivelseService;
  @Lazy @Autowired protected SaksmappeService saksmappeService;
  @Lazy @Autowired protected SkjermingService skjermingService;
  @Lazy @Autowired protected TilbakemeldingService tilbakemeldingService;
  @Lazy @Autowired protected UtredningService utredningService;
  @Lazy @Autowired protected VedtakService vedtakService;
  @Lazy @Autowired protected VoteringService voteringService;
  @Lazy @Autowired protected AuthenticationService authenticationService;

  protected abstract BaseRepository<O> getRepository();

  protected abstract BaseService<O, D> getProxy();

  @Autowired protected HttpServletRequest request;

  @Autowired protected EntityManager entityManager;

  @Autowired
  @Qualifier("compact")
  protected Gson gson;

  @Autowired private MeterRegistry meterRegistry;
  private Counter insertCounter;
  private Counter updateCounter;
  private Counter getCounter;
  private Counter deleteCounter;

  protected final Class<? extends Base> objectClass = newObject().getClass();
  protected final String objectClassName = objectClass.getSimpleName();
  protected final String idPrefix = IdGenerator.getPrefix(objectClass);

  // Elasticsearch indexing
  @Value("${application.elasticsearchIndex}")
  private String elasticsearchIndex;

  @Autowired private ElasticsearchClient esClient;
  @Autowired private ElasticsearchIndexQueue esQueue;

  /**
   * Initialize the counters in PostConstruct. We won't use a constructor in the superclass, since
   * this complicates the subclass constructors.
   */
  @PostConstruct
  void initCounters() {
    var name = objectClassName.toLowerCase();
    insertCounter = meterRegistry.counter("ein_action", "entity", name, "type", "insert");
    updateCounter = meterRegistry.counter("ein_action", "entity", name, "type", "update");
    getCounter = meterRegistry.counter("ein_action", "entity", name, "type", "get");
    deleteCounter = meterRegistry.counter("ein_action", "entity", name, "type", "delete");
  }

  /**
   * Creates a new instance of the Data Transfer Object (DTO) associated with this service. This
   * method is typically used to instantiate a DTO for data conversion or initial population.
   *
   * @return a new instance of the DTO type associated with this service
   */
  public abstract D newDTO();

  /**
   * Creates a new instance of the entity object associated with this service. This method is
   * commonly used to instantiate an entity before persisting it to the database, or before
   * populating it with data for further processing.
   *
   * @return a new instance of the entity type associated with this service
   */
  public abstract O newObject();

  /**
   * Finds an entity by its unique identifier. If the ID does not start with the current entity's ID
   * prefix, it is treated as an external ID or a system ID. This method can be extended by entity
   * services to provide additional lookup logic, for instance lookup by email address.
   *
   * @param id The unique identifier of the entity
   * @return the entity object if found, or null
   */
  @Transactional(readOnly = true)
  public O findById(String id) {
    var repository = getRepository();
    // If the ID doesn't start with our prefix, it is an external ID or a system ID
    if (!id.startsWith(idPrefix)) {
      var object = repository.findByExternalId(id);
      log.trace("findByExternalId {}:{}, {}", objectClassName, id, object);
      if (object != null) {
        return object;
      }
    }

    var object = repository.findById(id).orElse(null);
    log.trace("findById {}:{}, {}", objectClassName, id, object);
    return object;
  }

  /**
   * Look up an entity based on known unique fields in a DTO. This method is intended to be extended
   * by subclasses.
   *
   * @param dto The DTO to look up. NOTE: We specify BaseDTO here instead of D, to be able to use
   *     the generic type in @IdOrNewObject.
   * @return The entity if found, or null
   */
  @Transactional(readOnly = true)
  public O findByDTO(BaseDTO dto) {
    var repository = getRepository();
    if (dto.getId() != null) {
      var found = repository.findById(dto.getId()).orElse(null);
      if (found != null) {
        return found;
      }
    }

    if (dto.getExternalId() != null) {
      var found = repository.findByExternalId(dto.getExternalId());
      if (found != null) {
        return found;
      }
    }

    return null;
  }

  /**
   * Retrieves a DTO representation of an object based on a unique identifier.
   *
   * @param id The unique identifier of the entity
   * @return DTO object
   * @throws EInnsynException if the entity is not found
   */
  public D get(String id) throws EInnsynException {
    return getProxy().get(id, new BaseGetQueryDTO());
  }

  /**
   * Retrieves a DTO representation of an object based on a unique identifier.
   *
   * @param id The unique identifier of the entity
   * @return the DTO of the entity if found
   * @throws EInnsynException if the entity is not found
   */
  @NewSpan
  @Transactional(readOnly = true)
  public D get(String id, BaseGetQueryDTO query) throws EInnsynException {
    log.debug("get {}:{}", objectClassName, id);
    authorizeGet(id);

    var proxy = getProxy();
    var obj = proxy.findById(id);
    if (obj == null) {
      throw new NotFoundException("No object found with id " + id);
    }

    var expandSet = expandListToSet(query.getExpand());
    var dto = proxy.toDTO(obj, expandSet);
    if (log.isDebugEnabled()) {
      log.debug(
          "got {}:{}", objectClassName, id, StructuredArguments.raw("payload", gson.toJson(dto)));
    }
    getCounter.increment();
    return dto;
  }

  /**
   * Adds a new entity to the database. This is currently a wrapper for addOrUpdate() method, which
   * handles both new objects and updates.
   *
   * @param dto The entity object to add
   * @return the added entity
   */
  @NewSpan
  @Transactional(rollbackFor = Exception.class)
  @Retryable(
      retryFor = {DataIntegrityViolationException.class, OptimisticLockingFailureException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000))
  public D add(D dto) throws EInnsynException {
    authorizeAdd(dto);

    // Make sure the object doesn't already exist
    var existingObject = getProxy().findByDTO(dto);
    if (existingObject != null) {
      throw new ConflictException(
          "A conflicting object (" + existingObject.getId() + ") already exists.");
    }

    var paths = ExpandPathResolver.resolve(dto);
    var addedObj = addEntity(dto);
    scheduleReindex(addedObj);
    return getProxy().toDTO(addedObj, paths);
  }

  /**
   * Updates an entity. This is currently a wrapper for addOrUpdate() method, which handles both new
   * objects and updates.
   *
   * @param id ID of the object to update
   * @param dto The entity object to add
   * @return the added entity
   */
  @NewSpan
  @Transactional(rollbackFor = Exception.class)
  @Retryable(
      retryFor = {DataIntegrityViolationException.class, OptimisticLockingFailureException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000))
  public D update(String id, D dto) throws EInnsynException {
    authorizeUpdate(id, dto);
    var paths = ExpandPathResolver.resolve(dto);
    var obj = getProxy().findById(id);
    var updatedObj = updateEntity(obj, dto);
    scheduleReindex(updatedObj);
    return getProxy().toDTO(updatedObj, paths);
  }

  /**
   * Deletes an entity based on its ID. The method finds the entity, delegates to the abstract
   * delete method, and returns the deleted entity's DTO.
   *
   * @param id The unique identifier of the entity to delete
   * @return the DTO of the deleted entity
   */
  @NewSpan
  @Transactional(rollbackFor = Exception.class)
  @Retryable(
      retryFor = {DataIntegrityViolationException.class, OptimisticLockingFailureException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000))
  public D delete(String id) throws EInnsynException {
    authorizeDelete(id);
    var obj = getProxy().findById(id);

    // Schedule reindex before deleting, when we still have access to relations
    scheduleReindex(obj);

    // Create a DTO before it is deleted, so we can return it
    var dto = toDTO(obj);
    dto.setDeleted(true);

    deleteEntity(obj);

    return dto;
  }

  /**
   * Create and persist a new object. The method will handle persisting to the database, indexing to
   * ElasticSearch, and returning the updated entity's DTO.
   *
   * @param dto The DTO representation of the entity to update or add
   * @return the created entity object
   */
  protected O addEntity(D dto) throws EInnsynException {
    var repository = getRepository();
    var payload = StructuredArguments.raw("payload", gson.toJson(dto));
    var startTime = System.currentTimeMillis();
    log.debug("add {}", objectClassName, payload);

    // Generate database object from JSON
    var obj = fromDTO(dto, newObject());
    log.trace("addEntity saveAndFlush {}", objectClassName, payload);
    repository.saveAndFlush(obj);

    var duration = System.currentTimeMillis() - startTime;
    log.info(
        "added {}:{}",
        objectClassName,
        obj.getId(),
        payload,
        StructuredArguments.raw("duration", duration + ""));
    insertCounter.increment();

    return obj;
  }

  /**
   * Update an entity object in the database. This method will handle updating the database,
   * indexing to ElasticSearch, and returning the updated entity's DTO.
   *
   * @param id ID of the object to update
   * @param dto The DTO representation of the entity to update or add
   * @return Updated entity object
   * @throws EInnsynException if the update fails
   */
  protected O updateEntity(O obj, D dto) throws EInnsynException {
    var repository = getRepository();
    var payload = StructuredArguments.raw("payload", gson.toJson(dto));
    var startTime = System.currentTimeMillis();
    log.debug("update {}:{}", objectClassName, obj.getId(), payload);

    // Generate database object from JSON
    obj = fromDTO(dto, obj);
    log.trace("updateEntity saveAndFlush {}:{}", objectClassName, obj.getId(), payload);
    repository.saveAndFlush(obj);

    var duration = System.currentTimeMillis() - startTime;
    log.info(
        "updated {}:{}",
        objectClassName,
        obj.getId(),
        payload,
        StructuredArguments.raw("duration", duration + ""));
    updateCounter.increment();

    return obj;
  }

  /**
   * Delete an entity object from the database.
   *
   * @param obj The entity object to be deleted
   * @throws EInnsynException if the deletion fails
   */
  protected void deleteEntity(O obj) throws EInnsynException {
    var repository = getRepository();
    var startTime = System.currentTimeMillis();
    log.debug("delete {}:{}", objectClassName, obj.getId());

    try {
      repository.delete(obj);
    } catch (Exception e) {
      throw new EInnsynException(
          "Could not delete " + objectClassName + " object with id " + obj.getId());
    }

    var duration = System.currentTimeMillis() - startTime;
    log.info(
        "deleted {}:{}",
        objectClassName,
        obj.getId(),
        StructuredArguments.raw("duration", duration + ""));
    deleteCounter.increment();
  }

  /**
   * Takes an expandable field, inserts the object if it's a new object, or returns the existing
   * object if it's an existing object. This is a helper method for the fromDTO method, to handle
   * nested objects.
   *
   * @param dtoField Expandable DTO field
   * @return the created or existing entity
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public O createOrReturnExisting(ExpandableField<D> dtoField) throws EInnsynException {
    var id = dtoField.getId();
    var dto = dtoField.getExpandedObject();

    if (log.isTraceEnabled()) {
      log.trace(
          "createOrReturnExisting {}",
          id == null ? objectClassName : objectClassName + ":" + id,
          StructuredArguments.raw("payload", gson.toJson(dtoField)));
    }

    // If an ID is given, return the object
    var obj = id != null ? getProxy().findById(id) : getProxy().findByDTO(dto);

    // Verify that we're allowed to modify the found object
    if (obj != null) {
      try {
        getProxy().authorizeUpdate(obj.getId(), dto);
      } catch (ForbiddenException e) {
        throw new ForbiddenException("Not authorized to relate to " + objectClassName + ":" + id);
      }
      return obj;
    }

    return addEntity(dto);
  }

  /**
   * Takes an expandable field, inserts the object if it's a new object, throws if not. This is a
   * helper method for the fromDTO method, to handle nested objects.
   *
   * @param dtoField Expandable DTO field
   * @throws EInnsynException if the object is not found
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public O createOrThrow(ExpandableField<D> dtoField) throws EInnsynException {

    log.trace(
        "createOrThrow {}",
        objectClassName,
        StructuredArguments.raw("payload", gson.toJson(dtoField)));

    if (dtoField.getId() != null) {
      throw new ConflictException("Cannot create an object with an ID set: " + dtoField.getId());
    }

    if (getProxy().findByDTO(dtoField.getExpandedObject()) != null) {
      throw new ConflictException(
          "Cannot create object, a conflicting unique field already exists");
    }

    return addEntity(dtoField.getExpandedObject());
  }

  /**
   * Takes an expandable field, returns the object if it's an existing object, or throws if it's a
   * new object. This is a helper method for the fromDTO method, to handle nested objects.
   *
   * @param dtoField Expandable DTO field
   * @throws EInnsynException if the object is not found
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public O returnExistingOrThrow(ExpandableField<D> dtoField) throws EInnsynException {
    var id = dtoField.getId();
    var dto = dtoField.getExpandedObject();

    log.trace(
        "returnExistingOrThrow {}",
        id == null ? objectClassName : objectClassName + ":" + id,
        StructuredArguments.raw("payload", gson.toJson(dtoField)));

    var obj = id != null ? getProxy().findById(id) : getProxy().findByDTO(dto);

    if (obj == null) {
      throw new EInnsynException("Cannot return a new object");
    }

    return obj;
  }

  /**
   * Get a DTO from an expandable field. If the object is expanded (most likely a new object without
   * an ID), it will be returned. If not, the object will be fetched from the database.
   *
   * @param dtoField expandable DTO field
   * @return DTO object
   * @throws EInnsynException if the object is not found
   */
  public D getDTO(ExpandableField<D> dtoField) throws EInnsynException {
    if (dtoField.getExpandedObject() != null) {
      return dtoField.getExpandedObject();
    }
    return getProxy().get(dtoField.getId());
  }

  /**
   * Schedule a (re)index of a given object. The object will be indexed at the end of the current
   * request.
   *
   * @param obj
   */
  public void scheduleReindex(O obj) {
    scheduleReindex(obj, 0);
  }

  /**
   * Schedule a (re)index of a given object. The object will be indexed at the end of the current
   * request.
   *
   * @param obj The entity object to index
   * @param recurseDirection -1 for parents, 1 for children, 0 for both
   */
  public void scheduleReindex(O obj, int recurseDirection) {
    // Only access esQueue when we're in a request scope (not in service tests)
    if (obj instanceof Indexable && RequestContextHolder.getRequestAttributes() != null) {
      esQueue.add(obj);
    }
  }

  /**
   * Execute a scheduled reindex of an object. This method is called by the ElasticSearchIndexQueue
   * after the object has been added to the queue.
   *
   * @param id
   * @throws EInnsynException
   */
  @Async
  public void index(String id) {
    var esDocument = getProxy().toLegacyES(id);
    if (esDocument != null) {
      log.debug("index {}:{}", objectClassName, id);
      try {
        esClient.index(i -> i.index(elasticsearchIndex).id(id).document(esDocument));
      } catch (Exception e) {
        // Don't throw in Async
        log.error(
            "Could not index "
                + objectClassName
                + ":"
                + id
                + " to ElasticSearch: "
                + e.getMessage(),
            e);
      }
      try {
        var repository = getRepository();
        if (repository instanceof IndexableRepository indexableRepository) {
          indexableRepository.updateLastIndexed(id, Instant.now());
        }
      } catch (Exception e) {
        // Don't throw in Async
        log.error(
            "Could not update indexed timestamp for "
                + objectClassName
                + ":"
                + id
                + ": "
                + e.getMessage(),
            e);
      }
    }

    // Delete ES document
    else {
      log.debug("delete from index {}:{}", objectClassName, id);
      try {
        esClient.delete(d -> d.index(elasticsearchIndex).id(id));
      } catch (Exception e) {
        // Don't throw in Async
        log.error(
            "Could not delete "
                + objectClassName
                + ":"
                + id
                + " from ElasticSearch: "
                + e.getMessage(),
            e);
      }
    }
  }

  /**
   * Converts a Data Transfer Object (DTO) to its corresponding entity object (O). This method is
   * intended for reconstructing an entity from its DTO, typically used when persisting data
   * received in the form of a DTO to the database.
   *
   * @param dto the DTO to be converted to an entity
   * @param object the entity object to be populated
   * @return an entity object corresponding to the DTO
   */
  @SuppressWarnings({"java:S1130"}) // Subclasses might throw EInnsynException
  protected O fromDTO(D dto, O object) throws EInnsynException {

    if (dto.getExternalId() != null) {
      object.setExternalId(dto.getExternalId());
    }

    return object;
  }

  /**
   * Wrapper for toDTO with defaults for dto, paths, and currentPath.
   *
   * @param object Entity object to convert
   * @return DTO object
   */
  protected D toDTO(O object) {
    return getProxy().toDTO(object, newDTO(), new HashSet<>(), "");
  }

  /**
   * Wrapper for toDTO with defaults for dto and paths.
   *
   * @param object Entity object to convert
   * @param expandPaths Paths to expand
   * @return DTO object
   */
  protected D toDTO(O object, Set<String> expandPaths) {
    return getProxy().toDTO(object, newDTO(), expandPaths, "");
  }

  /**
   * Wrapper for toDTO with default dto.
   *
   * @param object Entity object to convert
   * @param expandPaths Paths to expand
   * @param currentPath Current path in the object tree
   * @return DTO object
   */
  protected D toDTO(O object, Set<String> expandPaths, String currentPath) {
    return getProxy().toDTO(object, newDTO(), expandPaths, currentPath);
  }

  /**
   * Wrapper for toDTO with defaults for paths and currentPath.
   *
   * @param object Entity object to convert
   * @param dto DTO object to populate
   * @return DTO object
   */
  protected D toDTO(O object, D dto) {
    return getProxy().toDTO(object, dto, new HashSet<>(), "");
  }

  /**
   * Converts an entity object (O) to its corresponding Data Transfer Object (DTO).
   *
   * @param object the entity object to be converted
   * @param dto the target DTO object
   * @param expandPaths a set of paths indicating properties to expand
   * @param currentPath the current path in the object tree, used for nested expansions
   * @return a DTO representation of the entity
   */
  protected D toDTO(O object, D dto, Set<String> expandPaths, String currentPath) {
    log.trace(
        "toDTO {}:{}, expandPaths: {}, currentPath: '{}'",
        objectClassName,
        object.getId(),
        expandPaths,
        currentPath);

    dto.setId(object.getId());
    dto.setExternalId(object.getExternalId());
    dto.setCreated(object.getCreated().toString());
    dto.setUpdated(object.getUpdated().toString());

    return dto;
  }

  /**
   * Converts an entity object to a legacy ElasticSearch document.
   *
   * @param id
   * @return
   */
  @Transactional(readOnly = true)
  public BaseES toLegacyES(String id) {
    var obj = getProxy().findById(id);
    if (!(obj instanceof Indexable)) {
      return null;
    }
    return toLegacyES(obj);
  }

  /**
   * Wrapper that creates a BaseES object for toLegacyES()
   *
   * @param object
   * @return
   */
  protected BaseES toLegacyES(O object) {
    return toLegacyES(object, new BaseES());
  }

  /**
   * Converts an entity object to a legacy ElasticSearch document. This format is used by the old
   * API and front-end, and should likely be replaced by an extended version of the DTO model in the
   * future.
   *
   * @param object
   * @param es
   * @return
   */
  protected BaseES toLegacyES(O object, BaseES es) {
    es.setId(object.getId());
    es.setExternalId(object.getExternalId());
    es.setType(List.of(object.getClass().getSimpleName()));
    return es;
  }

  /**
   * Retrieves a list of DTOs based on provided query parameters. This method uses the entity
   * service's getPage() implementation to get a paginated list of entities, and then converts the
   * page to a ResponseList.
   *
   * <p>Note: When searching using "endingBefore", the result list will be reversed. This is because
   * we use the "endingBefore" id as a pivot, and the DB will return the ordered list starting from
   * the pivot.
   *
   * @param params The query parameters for filtering and pagination
   * @return a ResultList containing DTOs that match the query criteria
   */
  @Transactional(readOnly = true)
  @SuppressWarnings("java:S3776") // Allow complexity of 19
  public ResultList<D> list(BaseListQueryDTO params) throws EInnsynException {
    log.debug("list {}, {}", objectClassName, params);
    authorizeList(params);

    var response = new ResultList<D>();
    var startingAfter = params.getStartingAfter();
    var endingBefore = params.getEndingBefore();
    var limit = params.getLimit();
    var hasNext = false;
    var hasPrevious = false;
    var uri = request.getRequestURI();
    var uriBuilder = UriComponentsBuilder.fromUriString(uri);

    // Ask for 2 more, so we can check if there is a next / previous page
    var responseList = listEntity(params, limit + 2);
    if (responseList.isEmpty()) {
      return response;
    }

    // If starting after, remove the first item if it's the same as the startingAfter value
    if (startingAfter != null) {
      var firstItem = responseList.getFirst();
      if (firstItem.getId().equals(startingAfter)) {
        hasPrevious = true;
        responseList = responseList.subList(1, responseList.size());
      }

      // If there are more items, remove remaining items and set "nextId"
      if (responseList.size() > limit) {
        hasNext = true;
        responseList = responseList.subList(0, limit);
      }
    }

    // If ending before, remove the first item if it's the same as the endingBefore value
    else if (endingBefore != null) {
      var lastItem = responseList.getLast();
      if (lastItem.getId().equals(endingBefore)) {
        hasNext = true;
        responseList = responseList.subList(0, responseList.size() - 1);
      }

      if (responseList.size() > limit) {
        hasPrevious = true;
        responseList = responseList.subList(responseList.size() - limit, responseList.size());
      }
    }

    // If we don't have startingAfter or endingBefore, we're at the beginning of the list, but we
    // might have more items
    else if (responseList.size() > limit) {
      hasNext = true;
      responseList = responseList.subList(0, limit);
    }

    if (hasNext) {
      var nextId = responseList.isEmpty() ? "" : responseList.getLast().getId();
      uriBuilder.replaceQueryParam("endingBefore");
      uriBuilder.replaceQueryParam("startingAfter", nextId);
      response.setNext(uriBuilder.build().toString());
    }
    if (hasPrevious) {
      var prevId = responseList.isEmpty() ? "" : responseList.getFirst().getId();
      uriBuilder.replaceQueryParam("startingAfter");
      uriBuilder.replaceQueryParam("endingBefore", prevId);
      response.setPrevious(uriBuilder.build().toString());
    }

    // Convert to DTO
    var expandPaths = expandListToSet(params.getExpand());
    var responseDtoList = new ArrayList<D>();
    for (var responseObject : responseList) {
      responseDtoList.add(toDTO(responseObject, expandPaths));
    }

    response.setItems(responseDtoList);

    return response;
  }

  /**
   * This method can be overridden by subclasses, to add custom logic for pagination, i.e. filtering
   * by parent. The {@link Paginators} object keeps one function for getting a page in ascending
   * order, and one for descending order.
   *
   * @param params The query parameters for pagination
   * @return a Paginators object
   */
  protected Paginators<O> getPaginators(BaseListQueryDTO params) {
    var repository = getRepository();
    var startingAfter = params.getStartingAfter();
    var endingBefore = params.getEndingBefore();

    if ((startingAfter != null && !startingAfter.isEmpty())
        || (endingBefore != null) && !endingBefore.isEmpty()) {
      return new Paginators<>(
          repository::findByIdGreaterThanEqualOrderByIdAsc,
          repository::findByIdLessThanEqualOrderByIdDesc);
    }
    return new Paginators<>(
        (pivot, pageRequest) -> repository.findAllByOrderByIdAsc(pageRequest),
        (pivot, pageRequest) -> repository.findAllByOrderByIdDesc(pageRequest));
  }

  /**
   * Retrieves a paginated list of entity objects based on query parameters. Supports pagination
   * through 'startingAfter' and 'endingBefore' fields in the query DTO.
   *
   * <p>We will always fetch one item more than "limit", to make the list() method able to detect
   * whether there are more items available.
   *
   * @param params The query parameters for pagination
   * @return a Page object containing the list of entities
   */
  protected List<O> listEntity(BaseListQueryDTO params, int limit) {
    var pageRequest = PageRequest.of(0, limit);
    var startingAfter = params.getStartingAfter();
    var endingBefore = params.getEndingBefore();
    var sortOrder = params.getSortOrder();
    var hasStartingAfter = startingAfter != null;
    var hasEndingBefore = endingBefore != null;
    var ascending = "asc".equals(sortOrder);
    var pivot = hasStartingAfter ? startingAfter : endingBefore;
    var paginators = getPaginators(params);

    var ids = params.getIds();
    if (ids != null) {
      var entityList = getRepository().findByIdIn(ids);
      Collections.sort(entityList, Comparator.comparingInt(entity -> ids.indexOf(entity.getId())));
      return entityList;
    }

    var externalIds = params.getExternalIds();
    if (externalIds != null) {
      var entityList = getRepository().findByExternalIdIn(externalIds);
      Collections.sort(
          entityList,
          Comparator.comparingInt(entity -> externalIds.indexOf(entity.getExternalId())));
      return entityList;
    }

    // If startingAfter / endingBefore is given but an empty string, it should match anything from
    // the beginning / the end of the list
    if ("".equals(pivot)) {
      pivot = null;
    }

    // The DB will always return the earliest possible matches, so when using endingBefore we need
    // to reverse the query, get the results immediately after the pivot, and then reverse the list.
    if (hasEndingBefore) {
      var page =
          ascending
              ? paginators.getDesc(pivot, pageRequest)
              : paginators.getAsc(pivot, pageRequest);
      return page.getContent().reversed();
    }

    var page =
        ascending ? paginators.getAsc(pivot, pageRequest) : paginators.getDesc(pivot, pageRequest);
    return page.getContent();
  }

  /**
   * Optionally expands an entity with the given property name and expand paths. If the property is
   * within the expand paths, a full DTO is provided; otherwise, only the ID is returned.
   *
   * @param obj The entity object to expand
   * @param propertyName The property name to check for expansion
   * @param expandPaths A set of paths indicating properties to expand
   * @param currentPath The current path in the object tree, used for nested expansions
   * @return an ExpandableField containing either a full DTO or just the ID
   */
  public ExpandableField<D> maybeExpand(
      O obj, String propertyName, Set<String> expandPaths, String currentPath) {
    if (obj == null) {
      return null;
    }
    if (currentPath == null) {
      currentPath = "";
    }
    var updatedPath = currentPath.isEmpty() ? propertyName : currentPath + "." + propertyName;
    var shouldExpand = expandPaths != null && expandPaths.contains(updatedPath);
    log.trace("maybeExpand {}:{}, {}", objectClassName, obj.getId(), shouldExpand);
    var expandedObject = shouldExpand ? toDTO(obj, newDTO(), expandPaths, updatedPath) : null;
    return new ExpandableField<>(obj.getId(), expandedObject);
  }

  /**
   * Wrapper around maybeExpand for lists. This method will expand all objects in the list, and
   * return a list of ExpandableFields.
   *
   * @param objList The list of entity objects to expand
   * @param propertyName The property name to check for expansion
   * @param expandPaths A set of paths indicating properties to expand
   * @param currentPath The current path in the object tree, used for nested expansions
   * @return a list of ExpandableFields containing either full DTOs or just the IDs
   */
  public List<ExpandableField<D>> maybeExpand(
      List<O> objList, String propertyName, Set<String> expandPaths, String currentPath) {
    if (objList == null) {
      return List.of();
    }
    return objList.stream()
        .map(obj -> maybeExpand(obj, propertyName, expandPaths, currentPath))
        .toList();
  }

  /**
   * Converts a list of expand strings to a set. For entries that contain a dot, the method will
   * also add the parent path to the set, so you don't need to add all levels of expansion.
   *
   * <p>Example:
   *
   * <p>Input: ["journalpost.journalenhet"]
   *
   * <p>Output: ["journalpost", "journalpost.journalenhet"]
   *
   * @param list The list of expand strings
   * @return a set of expand paths
   */
  public Set<String> expandListToSet(List<String> list) {
    if (list == null || list.isEmpty()) {
      return new HashSet<>();
    }
    var set = new HashSet<>(list);
    for (var item : list) {
      var dotIndex = item.indexOf('.');
      while (dotIndex >= 0) {
        set.add(item.substring(0, dotIndex));
        dotIndex = item.indexOf('.', dotIndex + 1);
      }
    }
    return set;
  }

  protected void authorizeList(BaseListQueryDTO params) throws EInnsynException {
    throw new ForbiddenException("Not authorized to list " + objectClassName);
  }

  protected void authorizeGet(String id) throws EInnsynException {
    throw new ForbiddenException("Not authorized to get " + objectClassName + " with id " + id);
  }

  protected void authorizeAdd(D dto) throws EInnsynException {
    throw new ForbiddenException("Not authorized to add " + objectClassName);
  }

  protected void authorizeUpdate(String id, D dto) throws EInnsynException {
    throw new ForbiddenException("Not authorized to update " + objectClassName + " with id " + id);
  }

  protected void authorizeDelete(String id) throws EInnsynException {
    throw new ForbiddenException("Not authorized to delete " + objectClassName + " with id " + id);
  }
}
