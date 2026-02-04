package no.einnsyn.backend.entities.base;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
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
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.authentication.AuthenticationService;
import no.einnsyn.backend.common.exceptions.models.AuthorizationException;
import no.einnsyn.backend.common.exceptions.models.BadRequestException;
import no.einnsyn.backend.common.exceptions.models.ConflictException;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.exceptions.models.InternalServerErrorException;
import no.einnsyn.backend.common.exceptions.models.NotFoundException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.indexable.Indexable;
import no.einnsyn.backend.common.indexable.IndexableRepository;
import no.einnsyn.backend.common.paginators.Paginators;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.apikey.ApiKeyService;
import no.einnsyn.backend.entities.arkiv.ArkivService;
import no.einnsyn.backend.entities.arkivdel.ArkivdelService;
import no.einnsyn.backend.entities.base.models.Base;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.base.models.BaseES;
import no.einnsyn.backend.entities.behandlingsprotokoll.BehandlingsprotokollService;
import no.einnsyn.backend.entities.bruker.BrukerService;
import no.einnsyn.backend.entities.dokumentbeskrivelse.DokumentbeskrivelseService;
import no.einnsyn.backend.entities.dokumentobjekt.DokumentobjektService;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.entities.identifikator.IdentifikatorService;
import no.einnsyn.backend.entities.innsynskrav.InnsynskravService;
import no.einnsyn.backend.entities.innsynskravbestilling.InnsynskravBestillingService;
import no.einnsyn.backend.entities.journalpost.JournalpostService;
import no.einnsyn.backend.entities.klasse.KlasseService;
import no.einnsyn.backend.entities.klassifikasjonssystem.KlassifikasjonssystemService;
import no.einnsyn.backend.entities.korrespondansepart.KorrespondansepartService;
import no.einnsyn.backend.entities.lagretsak.LagretSakService;
import no.einnsyn.backend.entities.lagretsoek.LagretSoekService;
import no.einnsyn.backend.entities.moetedeltaker.MoetedeltakerService;
import no.einnsyn.backend.entities.moetedokument.MoetedokumentService;
import no.einnsyn.backend.entities.moetemappe.MoetemappeService;
import no.einnsyn.backend.entities.moetesak.MoetesakService;
import no.einnsyn.backend.entities.moetesaksbeskrivelse.MoetesaksbeskrivelseService;
import no.einnsyn.backend.entities.saksmappe.SaksmappeService;
import no.einnsyn.backend.entities.skjerming.SkjermingService;
import no.einnsyn.backend.entities.tilbakemelding.TilbakemeldingService;
import no.einnsyn.backend.entities.utredning.UtredningService;
import no.einnsyn.backend.entities.vedtak.VedtakService;
import no.einnsyn.backend.entities.votering.VoteringService;
import no.einnsyn.backend.tasks.events.DeleteEvent;
import no.einnsyn.backend.tasks.events.GetEvent;
import no.einnsyn.backend.tasks.events.IndexEvent;
import no.einnsyn.backend.tasks.events.InsertEvent;
import no.einnsyn.backend.tasks.events.UpdateEvent;
import no.einnsyn.backend.tasks.handlers.index.ElasticsearchIndexQueue;
import no.einnsyn.backend.utils.ExpandPathResolver;
import no.einnsyn.backend.utils.TimeConverter;
import no.einnsyn.backend.utils.id.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.util.Pair;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
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

  // This service is the base of all entity-services. Since we have a nested data model,
  // we're bound to get circular dependencies, and we need to lazy-load them. We load all entity
  // services here, so that all entities can handle each other, and we avoid having lazy-loaded
  // beans elsewhere.
  @Lazy @Autowired protected ApiKeyService apiKeyService;
  @Lazy @Autowired protected ArkivService arkivService;
  @Lazy @Autowired protected ArkivdelService arkivdelService;
  @Lazy @Autowired protected BehandlingsprotokollService behandlingsprotokollService;
  @Lazy @Autowired protected BrukerService brukerService;
  @Lazy @Autowired protected DokumentbeskrivelseService dokumentbeskrivelseService;
  @Lazy @Autowired protected DokumentobjektService dokumentobjektService;
  @Lazy @Autowired protected EnhetService enhetService;
  @Lazy @Autowired protected IdentifikatorService identifikatorService;
  @Lazy @Autowired protected InnsynskravBestillingService innsynskravBestillingService;
  @Lazy @Autowired protected InnsynskravService innsynskravService;
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

  // These beans are autowired instead of injected in the constructor, so that we don't need to
  // handle them in all subclasses' constructors.
  @Autowired protected HttpServletRequest request;
  @Autowired protected EntityManager entityManager;
  @Autowired protected ApplicationEventPublisher eventPublisher;

  @Autowired
  @Qualifier("compact")
  protected Gson gson;

  @Autowired
  @Qualifier("pretty")
  protected Gson gsonPretty;

  @Autowired private MeterRegistry meterRegistry;
  private Counter insertCounter;
  private Counter updateCounter;
  private Counter getCounter;
  private Counter deleteCounter;

  protected final Class<? extends Base> objectClass = newObject().getClass();
  protected final String objectClassName = objectClass.getSimpleName();
  protected final String idPrefix = IdUtils.getPrefix(objectClass.getSimpleName()) + "_";

  // Elasticsearch indexing
  @Getter
  @Setter
  @Value("${application.elasticsearch.index}")
  protected String elasticsearchIndex;

  @Autowired protected ElasticsearchClient esClient;
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
   * Given an unique identifier (systemId, orgnummer, id, ...), resolve the entity ID.
   *
   * @param identifier the unique identifier to resolve
   * @return the entity ID
   */
  public String resolveId(String identifier) {
    if (objectClassName.equals(IdUtils.resolveEntity(identifier))) {
      return identifier;
    }
    var object = getProxy().findById(identifier);
    if (object != null) {
      return object.getId();
    }
    return null;
  }

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
   * Wrapper for findById() that throws a NotFoundException if the object is not found.
   *
   * @param id The ID of the object to find
   * @return The object with the given ID
   * @throws BadRequestException if the object is not found
   */
  public O findByIdOrThrow(String id) throws BadRequestException {
    return findByIdOrThrow(id, BadRequestException.class);
  }

  /**
   * Wrapper for findById() that throws a NotFoundException if the object is not found.
   *
   * @param id The ID of the object to find
   * @param exceptionClass The class of the exception to throw
   * @return The object with the given ID
   * @throws Exception if the object is not found
   */
  public <E extends Exception> O findByIdOrThrow(String id, Class<E> exceptionClass) throws E {
    var obj = getProxy().findById(id);
    if (obj == null) {
      try {
        throw exceptionClass
            .getDeclaredConstructor(String.class)
            .newInstance("No " + objectClassName + " found with id " + id);
      } catch (ReflectiveOperationException e) {
        throw new RuntimeException(
            "Failed to instantiate exception of type " + exceptionClass.getName(), e);
      }
    }
    return obj;
  }

  /**
   * Look up an entity based on known unique fields in a DTO. This method is intended to be extended
   * by subclasses.
   *
   * @param dto The DTO to look up.
   * @return The matching object if found, or null if not found.
   */
  public O findByDTO(BaseDTO dto) {
    var keyAndObject = getProxy().findPropertyAndObjectByDTO(dto);
    if (keyAndObject == null) {
      return null;
    }
    return keyAndObject.getSecond();
  }

  /**
   * Look up an entity based on known unique fields in a DTO. This method is intended to be extended
   * by subclasses.
   *
   * @param dto The DTO to look up.
   * @return A pair containing the matching property and the object if found, or null if not found.
   */
  @Transactional(readOnly = true)
  public Pair<String, O> findPropertyAndObjectByDTO(BaseDTO dto) {
    var repository = getRepository();
    if (dto.getId() != null) {
      var obj = repository.findById(dto.getId()).orElse(null);
      if (obj != null) {
        return Pair.of("id", obj);
      }
    }

    if (dto.getExternalId() != null) {
      var obj = repository.findByExternalId(dto.getExternalId());
      if (obj != null) {
        return Pair.of("externalId", obj);
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
    return getProxy().get(id, new GetParameters());
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
  public D get(String id, GetParameters query) throws EInnsynException {
    log.debug("get {}:{}", objectClassName, id);
    authorizeGet(id);

    var proxy = getProxy();
    var obj = proxy.findByIdOrThrow(id, NotFoundException.class);

    var expandSet = expandListToSet(query.getExpand());
    var dto = proxy.toDTO(obj, expandSet);
    log.atDebug()
        .setMessage("got {}:{}")
        .addArgument(objectClassName)
        .addArgument(id)
        .addKeyValue("payload", gson.toJson(dto))
        .log();
    getCounter.increment();

    eventPublisher.publishEvent(new GetEvent(this, dto));

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
      retryFor = {ObjectOptimisticLockingFailureException.class},
      backoff = @Backoff(delay = 100, random = true))
  public D add(D dto) throws EInnsynException {
    authorizeAdd(dto);

    // Make sure the object doesn't already exist
    var existingObjectPair = getProxy().findPropertyAndObjectByDTO(dto);
    if (existingObjectPair != null) {
      var property = existingObjectPair.getFirst();
      var existingObject = existingObjectPair.getSecond();
      var cause = new Exception(gsonPretty.toJson(dto));
      throw new ConflictException(
          "A conflicting object ("
              + existingObject.getId()
              + ") already exists. Duplicate value in the field `"
              + property
              + "`.",
          cause);
    }

    var paths = ExpandPathResolver.resolve(dto);
    var addedObj = addEntity(dto);

    scheduleIndex(addedObj.getId());
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
      retryFor = {ObjectOptimisticLockingFailureException.class},
      backoff = @Backoff(delay = 100, random = true))
  public D update(String id, D dto) throws EInnsynException {
    authorizeUpdate(id, dto);

    var paths = ExpandPathResolver.resolve(dto);
    var obj = getProxy().findByIdOrThrow(id);
    var updatedObj = updateEntity(obj, dto);

    scheduleIndex(obj.getId());
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
      retryFor = {ObjectOptimisticLockingFailureException.class},
      backoff = @Backoff(delay = 100, random = true))
  public D delete(String id) throws EInnsynException {
    authorizeDelete(id);
    var obj = getProxy().findByIdOrThrow(id);

    // Schedule reindex before deleting, when we still have access to relations
    getProxy().scheduleIndex(obj.getId());

    // Create a DTO before it is deleted, so we can return it
    var dto = toDTO(obj);
    dto.setDeleted(true);

    deleteEntity(obj);

    return dto;
  }

  /**
   * Deletes an entity based on its ID. The method finds the entity, delegates to the abstract
   * delete method, and returns the deleted entity's DTO.
   *
   * @param id The unique identifier of the entity to delete
   * @return the DTO of the deleted entity
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public D deleteInNewTransaction(String id) throws EInnsynException {
    return getProxy().delete(id);
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
    var jsonPayload = gson.toJson(dto);
    var startTime = System.currentTimeMillis();
    log.atDebug()
        .setMessage("add {}")
        .addArgument(objectClassName)
        .addKeyValue("payload", jsonPayload)
        .log();

    // Generate database object from JSON
    var obj = fromDTO(dto, newObject());
    log.atTrace()
        .setMessage("addEntity saveAndFlush {}")
        .addArgument(objectClassName)
        .addKeyValue("payload", jsonPayload)
        .log();
    repository.saveAndFlush(obj);

    var duration = System.currentTimeMillis() - startTime;
    log.atInfo()
        .setMessage("added {}:{}")
        .addArgument(objectClassName)
        .addArgument(obj.getId())
        .addKeyValue("payload", jsonPayload)
        .addKeyValue("duration", duration)
        .log();
    insertCounter.increment();

    eventPublisher.publishEvent(new InsertEvent(this, dto));

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
    var jsonPayload = gson.toJson(dto);
    var startTime = System.currentTimeMillis();
    log.atDebug()
        .setMessage("update {}:{}")
        .addArgument(objectClassName)
        .addArgument(obj.getId())
        .addKeyValue("payload", jsonPayload)
        .log();

    // Generate database object from JSON
    obj = fromDTO(dto, obj);
    log.atTrace()
        .setMessage("updateEntity saveAndFlush {}:{}")
        .addArgument(objectClassName)
        .addArgument(obj.getId())
        .addKeyValue("payload", jsonPayload)
        .log();
    repository.saveAndFlush(obj);

    var duration = System.currentTimeMillis() - startTime;
    log.atInfo()
        .setMessage("updated {}:{}")
        .addArgument(objectClassName)
        .addArgument(obj.getId())
        .addKeyValue("payload", jsonPayload)
        .addKeyValue("duration", duration)
        .log();
    updateCounter.increment();

    eventPublisher.publishEvent(new UpdateEvent(this, dto));

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
      throw new InternalServerErrorException(
          "Could not delete " + objectClassName + " object with id " + obj.getId(), e);
    }

    var duration = System.currentTimeMillis() - startTime;
    log.atInfo()
        .setMessage("deleted {}:{}")
        .addArgument(objectClassName)
        .addArgument(obj.getId())
        .addKeyValue("duration", duration)
        .log();
    deleteCounter.increment();

    eventPublisher.publishEvent(new DeleteEvent(this, toDTO(obj)));
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

    log.atTrace()
        .setMessage("createOrReturnExisting {}")
        .addArgument(id == null ? objectClassName : objectClassName + ":" + id)
        .addKeyValue("payload", gson.toJson(dtoField))
        .log();

    // If an ID is given, return the object
    var obj = id != null ? getProxy().findById(id) : getProxy().findByDTO(dto);

    // Verify that we're allowed to modify the found object
    if (obj != null) {
      try {
        getProxy().authorizeUpdate(obj.getId(), dto);
      } catch (AuthorizationException e) {
        throw new AuthorizationException(
            "Not authorized to relate to " + objectClassName + ":" + obj.getId());
      }

      // Update the object with the new DTO
      if (dto != null) {
        obj = updateEntity(obj, dto);
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

    log.atTrace()
        .setMessage("createOrThrow {}")
        .addArgument(objectClassName)
        .addKeyValue("payload", gson.toJson(dtoField))
        .log();

    if (dtoField.getId() != null) {
      throw new BadRequestException("Cannot create an object with an ID set: " + dtoField.getId());
    }

    // Make sure the object doesn't already exist
    var dto = dtoField.getExpandedObject();
    // Make sure the object doesn't already exist
    var existingObjectPair = getProxy().findPropertyAndObjectByDTO(dto);
    if (existingObjectPair != null) {
      var property = existingObjectPair.getFirst();
      var existingObject = existingObjectPair.getSecond();
      var cause = new Exception(gsonPretty.toJson(dto));
      throw new ConflictException(
          "A conflicting object ("
              + existingObject.getId()
              + ") already exists. Duplicate value in the field `"
              + property
              + "`.",
          cause);
    }

    return addEntity(dto);
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

    log.atTrace()
        .setMessage("returnExistingOrThrow {}")
        .addArgument(id == null ? objectClassName : objectClassName + ":" + id)
        .addKeyValue("payload", gson.toJson(dtoField))
        .log();

    var obj = id != null ? getProxy().findById(id) : getProxy().findByDTO(dto);

    if (obj == null) {
      throw new BadRequestException("Cannot return a new object");
    }

    return obj;
  }

  /**
   * Schedule a (re)index of a given object. The object will be indexed at the end of the current
   * request.
   *
   * @param id ID of the entity object to index
   */
  public void scheduleIndex(String id) {
    scheduleIndex(id, 0);
  }

  /**
   * Schedule a (re)index of a given object. The object will be indexed at the end of the current
   * request.
   *
   * @param id ID of the entity object to index
   * @param recurseDirection -1 for parents, 1 for children, 0 for both
   */
  public boolean scheduleIndex(String id, int recurseDirection) {

    // Only access esQueue when we're in a request scope. This guard applies to any execution
    // outside of a request context, including scheduled tasks and service tests.
    if (RequestContextHolder.getRequestAttributes() == null) {
      return false;
    }

    if (esQueue.isScheduled(id, recurseDirection)) {
      return true;
    }

    if (Indexable.class.isAssignableFrom(objectClass)) {
      esQueue.add(id, recurseDirection);
    }

    return false;
  }

  /**
   * Execute a scheduled reindex of an object. This method is called by the ElasticSearchIndexQueue
   * after the object has been added to the queue.
   *
   * @param id the ID of the object to index
   * @param timestamp the timestamp when the indexing was scheduled
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public void index(String id, Instant timestamp) {
    var proxy = getProxy();
    var object = proxy.findById(id);
    var esParent = proxy.getESParent(object, id);

    // Insert / update document if the object exists
    if (object instanceof Indexable indexable) {
      // Do nothing if the object has been indexed after `scheduledTimestamp`
      if (indexable.getLastIndexed() != null && indexable.getLastIndexed().isAfter(timestamp)) {
        log.debug(
            "Not indexing {} : {} , it has already been indexed after {}",
            objectClassName,
            id,
            timestamp);
        return;
      }

      var isInsert = false;
      var esDocument = proxy.toLegacyES(object);

      // If esDocument is null, we don't index it, but we still need to update lastIndexed
      if (esDocument == null) {
        log.debug("Not indexing {} : {} , ES document is null", objectClassName, id);
        var repository = getRepository();
        if (repository instanceof IndexableRepository<?> indexableRepository) {
          indexableRepository.updateLastIndexed(id, Instant.now());
        }
        return;
      }

      var lastIndexed = indexable.getLastIndexed();
      var accessibleAfter = esDocument.getAccessibleAfter();
      log.debug(
          "index {} : {} routing: {} lastIndexed: {}", objectClassName, id, esParent, lastIndexed);
      try {
        esClient.index(
            i -> i.index(elasticsearchIndex).id(id).document(esDocument).routing(esParent));

        // Mark as insert if the object has never been indexed before
        if (lastIndexed == null) {
          isInsert = true;
        }

        // Mark as insert if the object has been made accessible after the last index
        else if (accessibleAfter != null) {
          // TODO: We should consider adding logic that checks if the object has been marked as an
          // insert before, to avoid sending subscription email multiple times for the same document
          isInsert = lastIndexed.isBefore(Instant.parse(accessibleAfter));
        }
      } catch (Exception e) {
        // Don't throw in Async
        log.error("Could not index {} : {} to ElasticSearch", objectClassName, id, e);
        if (e instanceof ElasticsearchException elasticsearchException) {
          log.error(elasticsearchException.response().toString());
        }
        return;
      }

      // Update lastIndexed timestamp in the database
      try {
        var repository = getRepository();
        if (repository instanceof IndexableRepository<?> indexableRepository) {
          indexableRepository.updateLastIndexed(id, Instant.now());
        }
        eventPublisher.publishEvent(new IndexEvent(this, esDocument, isInsert));
        log.info(
            "indexed {} : {} routing: {} lastIndexed: {}",
            objectClassName,
            id,
            esParent,
            Instant.now());
      } catch (Exception e) {
        // Don't throw in Async
        log.error(
            "Could not update lastIndexed for {} : {} : {}",
            objectClassName,
            id,
            e.getMessage(),
            e);
      }
    }

    // Delete ES document if the object doesn't exist
    else {
      log.debug("delete from index {} : {}", objectClassName, id);
      try {
        esClient.delete(d -> d.index(elasticsearchIndex).id(id).routing(esParent));
        log.info("deleted {} : {} routing: {}", objectClassName, id, esParent);
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
   * Get the "parent" of an ES document, in case it is a child. By default, the document is not a
   * child and we return null.
   *
   * @param id object ID
   * @return ID of the parent, or null
   */
  @Transactional(readOnly = true)
  public String getESParent(O object, String id) {
    return null;
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
    if (dto.getAccessibleAfter() != null) {
      object.setAccessibleAfter(TimeConverter.timestampToInstant(dto.getAccessibleAfter()));
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

    // Only expose accessibleAfter if it's in the future
    if (object.getAccessibleAfter() != null && object.getAccessibleAfter().isAfter(Instant.now())) {
      dto.setAccessibleAfter(TimeConverter.instantToTimestamp(object.getAccessibleAfter()));
    }

    return dto;
  }

  /**
   * Wrapper that creates a BaseES object for toLegacyES()
   *
   * @param object the entity object to convert
   * @return the legacy ElasticSearch document
   */
  protected BaseES toLegacyES(O object) {
    return toLegacyES(object, new BaseES());
  }

  /**
   * Converts an entity object to a legacy ElasticSearch document. This format is used by the old
   * API and front-end, and should likely be replaced by an extended version of the DTO model in the
   * future.
   *
   * @param object the entity object to convert
   * @param es the BaseES object to populate
   * @return the populated legacy ElasticSearch document
   */
  protected BaseES toLegacyES(O object, BaseES es) {
    es.setId(object.getId());
    es.setExternalId(object.getExternalId());
    es.setType(List.of(object.getClass().getSimpleName()));
    es.setCreated(TimeConverter.instantToTimestamp(object.getCreated()));
    es.setUpdated(TimeConverter.instantToTimestamp(object.getUpdated()));
    if (object.getAccessibleAfter() != null) {
      es.setAccessibleAfter(TimeConverter.instantToTimestamp(object.getAccessibleAfter()));
    }
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
   * @return a ListResponse containing DTOs that match the query criteria
   */
  @Transactional(readOnly = true)
  @SuppressWarnings("java:S3776") // Allow complexity of 19
  public PaginatedList<D> list(ListParameters params) throws EInnsynException {
    log.atDebug()
        .setMessage("list {}")
        .addArgument(objectClassName)
        .addKeyValue("payload", gson.toJson(params))
        .log();

    authorizeList(params);

    var response = new PaginatedList<D>();
    var startingAfter = params.getStartingAfter();
    var endingBefore = params.getEndingBefore();
    var limit = params.getLimit();
    var hasNext = false;
    var hasPrevious = false;
    var uri = request.getRequestURI();
    var queryString = request.getQueryString();
    var uriBuilder = UriComponentsBuilder.fromUriString(uri).query(queryString);

    // Ask for 2 more, so we can check if there is a next / previous page
    var responseList = listEntity(params, limit + 2);
    if (responseList.isEmpty()) {
      return response;
    }

    if (params.getIds() != null || params.getExternalIds() != null) {
      // If we have a list of IDs, we don't need to check for next / previous
      hasNext = false;
      hasPrevious = false;
      startingAfter = null;
      endingBefore = null;
      limit = 0;
      if (params.getIds() != null) {
        limit += params.getIds().size();
      }
      if (params.getExternalIds() != null) {
        limit += params.getExternalIds().size();
      }
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
  protected Paginators<O> getPaginators(ListParameters params) throws EInnsynException {
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
  protected List<O> listEntity(ListParameters params, int limit) throws EInnsynException {
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
    if (pivot != null && pivot.isEmpty()) {
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

  protected void authorizeList(ListParameters params) throws EInnsynException {
    throw new AuthorizationException("Not authorized to list " + objectClassName);
  }

  protected void authorizeGet(String id) throws EInnsynException {
    throw new AuthorizationException("Not authorized to get " + objectClassName + " with id " + id);
  }

  protected void authorizeAdd(D dto) throws EInnsynException {
    throw new AuthorizationException("Not authorized to add " + objectClassName);
  }

  protected void authorizeUpdate(String id, D dto) throws EInnsynException {
    throw new AuthorizationException(
        "Not authorized to update " + objectClassName + " with id " + id);
  }

  protected void authorizeDelete(String id) throws EInnsynException {
    throw new AuthorizationException(
        "Not authorized to delete " + objectClassName + " with id " + id);
  }
}
