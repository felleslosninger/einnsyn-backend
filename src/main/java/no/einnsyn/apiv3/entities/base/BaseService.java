package no.einnsyn.apiv3.entities.base;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.arkiv.ArkivService;
import no.einnsyn.apiv3.entities.arkivdel.ArkivdelService;
import no.einnsyn.apiv3.entities.base.models.*;
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
import no.einnsyn.apiv3.utils.IdGenerator;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// By design, we want all entity services to be able to access all other entity services. This
// requires lazy loading of service beans.
@SuppressWarnings("java:S6813")
public abstract class BaseService<O extends Base, D extends BaseDTO> {

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
  @Lazy @Autowired protected KorrespondansepartService korrespondansepartService;
  @Lazy @Autowired protected LagretSakService lagretSakService;
  @Lazy @Autowired protected LagretSoekService lagretSoekService;
  @Lazy @Autowired protected MoetedeltakerService moetedeltakerService;
  @Lazy @Autowired protected MoetedokumentService moetedokumentService;
  @Lazy @Autowired protected MoetemappeService moetemappeService;
  @Lazy @Autowired protected MoetesakService moetesakService;
  @Lazy @Autowired protected MoetesaksbeskrivelseService moetesakbeskrivelseService;
  @Lazy @Autowired protected SaksmappeService saksmappeService;
  @Lazy @Autowired protected SkjermingService skjermingService;
  @Lazy @Autowired protected TilbakemeldingService tilbakemeldingService;
  @Lazy @Autowired protected UtredningService utredningService;
  @Lazy @Autowired protected VedtakService vedtakService;
  @Lazy @Autowired protected VoteringService voteringService;

  protected final String idPrefix = IdGenerator.getPrefix(this.newObject().getClass());

  protected abstract BaseRepository<O> getRepository();

  protected abstract BaseService<O, D> getProxy();

  /**
   * A way for superclasses to create a new DTO of the correct type
   *
   * @return
   */
  public abstract D newDTO();

  /**
   * A way for superclasses to create a new object of the correct type
   *
   * @return
   */
  public abstract O newObject();

  /**
   * @param id
   * @return
   */
  @Transactional
  public O findById(String id) {
    var repository = this.getRepository();
    // If the ID doesn't start with our prefix, it is an external ID or a system ID
    if (!id.startsWith(idPrefix)) {
      var object = repository.findByExternalId(id);
      if (object != null) {
        return object;
      }
    }

    return repository.findById(id).orElse(null);
  }

  /**
   * @param id
   * @return
   */
  @Transactional
  public boolean existsById(String id) {
    var repository = this.getRepository();
    // If the ID doesn't start with our prefix, it is an external ID or a system ID
    if (!id.startsWith(idPrefix)) {
      var exists = repository.existsByExternalId(id);
      if (exists) {
        return true;
      }
    }

    return repository.existsById(id);
  }

  public D get(String id) {
    return getProxy().get(id, new BaseGetQueryDTO());
  }

  /**
   * @param json
   * @return
   */
  @Transactional
  public D get(String id, BaseGetQueryDTO query) {
    var proxy = getProxy();
    var obj = proxy.findById(id);
    var expandList = query.getExpand();
    if (expandList == null) {
      expandList = new ArrayList<>();
    }
    var expandSet = new HashSet<String>(expandList);
    return proxy.toDTO(obj, expandSet);
  }

  /**
   * @param dto
   * @return
   */
  public D add(D dto) {
    return getProxy().update(null, dto);
  }

  /**
   * Insert a new object from a JSON object, persist/index it to all relevant databases.
   *
   * @param json
   * @return
   */
  public D update(D json) {
    return getProxy().update(null, json);
  }

  /**
   * Update a Dokumentbeskrivelse from a JSON object, persist/index it to all relevant databases. If
   * no ID is given, a new Dokumentbeskrivelse will be created.
   *
   * @param id
   * @param dto
   * @return
   */
  @Transactional
  public D update(String id, D dto) {
    O obj = null;
    var proxy = this.getProxy();
    var repository = this.getRepository();

    // If ID is given, get the existing saksmappe from DB
    if (id != null) {
      obj = proxy.findById(id);
    } else {
      obj = newObject();
    }

    // Generate database object from JSON
    var paths = new HashSet<String>();
    obj = proxy.fromDTO(dto, obj, paths, "");
    repository.saveAndFlush(obj);

    // Add / update ElasticSearch document
    proxy.index(obj, true);

    // Generate a DTO containing all inserted objects
    return proxy.toDTO(obj, newDTO(), paths, "");
  }

  /**
   * Index the object to ElasticSearch. Dummy placeholder for entities that shouldn't be indexed.
   *
   * @param obj
   */
  public void index(O obj) {
    this.index(obj, false);
  }

  public void index(O obj, boolean shouldUpdateRelatives) {}

  /**
   * @param dto
   * @return
   */
  public O fromDTO(D dto) {
    return getProxy().fromDTO(dto, this.newObject(), new HashSet<>(), "");
  }

  public O fromDTO(D dto, O object) {
    return getProxy().fromDTO(dto, object, new HashSet<>(), "");
  }

  /**
   * @param dto
   * @param paths
   * @param currentPath
   * @return
   */
  public O fromDTO(D dto, Set<String> paths, String currentPath) {
    return getProxy().fromDTO(dto, this.newObject(), paths, currentPath);
  }

  /**
   * Create a EinnsynObject object from a JSON description
   *
   * @param object
   * @param dto
   */
  @Transactional
  public O fromDTO(D dto, O object, Set<String> paths, String currentPath) {
    if (dto.getExternalId() != null) {
      // TODO: Make sure external IDs don't have our ID prefix. This will make it fail on lookup
      object.setExternalId(dto.getExternalId());
    }

    return object;
  }

  public D toDTO(O object) {
    return getProxy().toDTO(object, newDTO(), new HashSet<>(), "");
  }

  public D toDTO(O object, Set<String> expandPaths) {
    return getProxy().toDTO(object, newDTO(), expandPaths, "");
  }

  public D toDTO(O object, Set<String> expandPaths, String currentPath) {
    return getProxy().toDTO(object, newDTO(), expandPaths, currentPath);
  }

  public D toDTO(O object, D dto) {
    return getProxy().toDTO(object, dto, new HashSet<>(), "");
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public D toDTO(O object, D dto, Set<String> expandPaths, String currentPath) {

    dto.setId(object.getId());
    dto.setExternalId(object.getExternalId());
    dto.setCreated(object.getCreated().toString());
    dto.setUpdated(object.getUpdated().toString());

    return dto;
  }

  public O fromES(JSONObject source) {
    // TODO: Check version number in the ES document, parse accordingly

    var version = (Integer) source.get("_version");
    if (version == null) {
      version = 1;
    }

    // This is a legacy document
    if (version == 1) {
      var id = (String) source.get("id");
      return getProxy().findById(id);
    }

    return null;
  }

  /**
   * @param params
   * @return
   */
  public ResultList<D> list(BaseListQueryDTO params) {
    return getProxy().list(params, null);
  }

  /** Allows a parentId string that subclasses can use to filter the list */
  public ResultList<D> list(BaseListQueryDTO params, Page<O> responsePage) {
    var response = new ResultList<D>();

    // Fetch the requested list page
    if (responsePage == null) {
      responsePage = getProxy().getPage(params);
    }

    var responseList = new LinkedList<>(responsePage.getContent());

    // If there is one more item than requested, set hasMore and remove the last item
    if (responseList.size() > params.getLimit()) {
      response.setHasMore(true);
      responseList.remove(responseList.size() - 1);
    }

    // Convert to JSON
    var expandPaths = new HashSet<String>(params.getExpand());
    var responseDtoList = new ArrayList<D>();
    responseList.forEach(
        responseObject -> responseDtoList.add(getProxy().toDTO(responseObject, expandPaths)));

    response.setItems(responseDtoList);

    return response;
  }

  /**
   * Get a single page of a paginated list of objects. This can be overridden by subclasses to allow
   * entity-specific filtering.
   *
   * @param params
   * @return
   */
  @Transactional
  public Page<O> getPage(BaseListQueryDTO params) {
    Page<O> responsePage = null;
    var repository = this.getRepository();

    if (params.getStartingAfter() != null) {
      responsePage =
          repository.findByIdGreaterThanOrderByIdDesc(
              params.getStartingAfter(), PageRequest.of(0, params.getLimit() + 1));
    } else if (params.getEndingBefore() != null) {
      responsePage =
          repository.findByIdLessThanOrderByIdDesc(
              params.getEndingBefore(), PageRequest.of(0, params.getLimit() + 1));
    } else {
      responsePage = repository.findAllByOrderByIdDesc(PageRequest.of(0, params.getLimit() + 1));
    }

    return responsePage;
  }

  /**
   * Creates an ExpandableField object. If propertyName is in the expandPaths list, the object will
   * be expanded, if not, it will only contain the ID.
   *
   * @param obj
   * @param propertyName Name of the property to expand, appended to currentPath for deeper steps
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
  public ExpandableField<D> maybeExpand(
      O obj, String propertyName, Set<String> expandPaths, String currentPath) {
    if (currentPath == null) currentPath = "";
    String updatedPath = currentPath.isEmpty() ? propertyName : currentPath + "." + propertyName;
    if (expandPaths != null && expandPaths.contains(updatedPath)) {
      return new ExpandableField<>(
          obj.getId(), getProxy().toDTO(obj, newDTO(), expandPaths, updatedPath));
    } else {
      return new ExpandableField<>(obj.getId(), null);
    }
  }

  /**
   * Delete object by ID
   *
   * @param id
   * @return
   */
  @Transactional
  public D delete(String id) {
    var proxy = getProxy();
    var obj = proxy.findById(id);
    return proxy.delete(obj);
  }

  /** Delete object */
  @Transactional
  public abstract D delete(O obj);
}
