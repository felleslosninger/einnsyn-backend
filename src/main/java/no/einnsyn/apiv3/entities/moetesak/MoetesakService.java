package no.einnsyn.apiv3.entities.moetesak;

import java.util.List;
import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.paginators.Paginators;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseES;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseES;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseListQueryDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeES.MoetemappeWithoutChildrenES;
import no.einnsyn.apiv3.entities.moetesak.models.Moetesak;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakES;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakListQueryDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakstypeResolver;
import no.einnsyn.apiv3.entities.registrering.RegistreringService;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.utils.TimeConverter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MoetesakService extends RegistreringService<Moetesak, MoetesakDTO> {

  @Getter private final MoetesakRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private MoetesakService proxy;

  public MoetesakService(MoetesakRepository repository) {
    this.repository = repository;
  }

  public Moetesak newObject() {
    return new Moetesak();
  }

  public MoetesakDTO newDTO() {
    return new MoetesakDTO();
  }

  /**
   * Override scheduleReindex to reindex the parent Moetemappe.
   *
   * @param moetesak
   * @param recurseDirection -1 for parents, 1 for children, 0 for both
   */
  @Override
  public void scheduleReindex(Moetesak moetesak, int recurseDirection) {
    super.scheduleReindex(moetesak, recurseDirection);

    // Index moetemappe
    if (recurseDirection <= 0) {
      moetemappeService.scheduleReindex(moetesak.getMoetemappe(), -1);
    }
  }

  @Override
  protected Moetesak fromDTO(MoetesakDTO dto, Moetesak moetesak) throws EInnsynException {
    super.fromDTO(dto, moetesak);

    if (dto.getMoetesakstype() != null) {
      moetesak.setMoetesakstype(dto.getMoetesakstype());
    }

    if (dto.getLegacyMoetesakstype() != null) {
      moetesak.setLegacyMoetesakstype(dto.getLegacyMoetesakstype());
      moetesak.setMoetesakstype(
          MoetesakstypeResolver.resolve(dto.getLegacyMoetesakstype()).toString());
    }

    if (dto.getLegacyReferanseTilMoetesak() != null) {
      moetesak.setJournalpostIri(StringUtils.trimToNull(dto.getLegacyReferanseTilMoetesak()));
    }

    // TODO: Remove this when the old API isn't used anymore
    if (moetesak.getLegacyMoetesakstype() == null) {
      moetesak.setLegacyMoetesakstype(moetesak.getMoetesakstype());
    }

    if (dto.getMoetesaksaar() != null) {
      moetesak.setMoetesaksaar(dto.getMoetesaksaar());
    }

    if (dto.getMoetesakssekvensnummer() != null) {
      moetesak.setMoetesakssekvensnummer(dto.getMoetesakssekvensnummer());
    }

    if (dto.getVideoLink() != null) {
      moetesak.setVideoLink(dto.getVideoLink());
    }

    if (dto.getMoetemappe() != null) {
      moetesak.setMoetemappe(moetemappeService.returnExistingOrThrow(dto.getMoetemappe()));
    }

    var moetemappe = moetesak.getMoetemappe();
    var utvalgKode = dto.getUtvalg();
    if (utvalgKode == null
        && moetesak.getUtvalg() == null
        && moetemappe != null
        && moetemappe.getUtvalg() != null) {
      utvalgKode = moetesak.getMoetemappe().getUtvalg();
    }
    if (utvalgKode != null) {
      moetesak.setUtvalg(utvalgKode);
      var journalenhet = moetesak.getJournalenhet();
      var utvalg = enhetService.findByEnhetskode(utvalgKode, journalenhet);
      if (utvalg != null) {
        moetesak.setUtvalgObjekt(utvalg);
      }
    }

    if (moetesak.getUtvalgObjekt() == null) {
      if (moetemappe != null) {
        moetesak.setUtvalgObjekt(moetemappe.getUtvalgObjekt());
      } else {
        moetesak.setUtvalgObjekt(moetesak.getJournalenhet());
      }
    }

    // Workaround since legacy IDs are used for relations. OneToMany relations fails if the ID is
    // not set.
    if (moetesak.getId() == null) {
      moetesak = repository.saveAndFlush(moetesak);
    }

    // Utredning
    var utredningField = dto.getUtredning();
    if (utredningField != null) {
      // Replace
      var replacedObject = moetesak.getUtredning();
      if (replacedObject != null) {
        // JPA won't delete Utredning if it's still referenced
        moetesak.setUtredning(null);
        utredningService.delete(replacedObject.getId());
      }
      moetesak.setUtredning(utredningService.createOrReturnExisting(utredningField));
    }

    // Innstilling
    var innstillingField = dto.getInnstilling();
    if (innstillingField != null) {
      // Replace
      var replacedObject = moetesak.getInnstilling();
      if (replacedObject != null) {
        // JPA won't delete Innstilling if it's still referenced
        moetesak.setInnstilling(null);
        moetesaksbeskrivelseService.delete(replacedObject.getId());
      }
      moetesak.setInnstilling(moetesaksbeskrivelseService.createOrReturnExisting(innstillingField));
    }

    // Vedtak
    var vedtakField = dto.getVedtak();
    if (vedtakField != null) {
      // Replace?
      var replacedObject = moetesak.getVedtak();
      if (replacedObject != null) {
        // JPA won't delete Vedtak if it's still referenced
        moetesak.setVedtak(null);
        vedtakService.delete(replacedObject.getId());
      }
      moetesak.setVedtak(vedtakService.createOrReturnExisting(vedtakField));
    }

    // Dokumentbeskrivelse
    var dokumentbeskrivelseFieldList = dto.getDokumentbeskrivelse();
    if (dokumentbeskrivelseFieldList != null) {
      for (var dokumentbeskrivelseField : dokumentbeskrivelseFieldList) {
        moetesak.addDokumentbeskrivelse(
            dokumentbeskrivelseService.createOrReturnExisting(dokumentbeskrivelseField));
      }
    }

    return moetesak;
  }

  @Override
  protected MoetesakDTO toDTO(
      Moetesak moetesak, MoetesakDTO dto, Set<String> paths, String currentPath) {
    super.toDTO(moetesak, dto, paths, currentPath);

    dto.setMoetesakstype(moetesak.getMoetesakstype());
    dto.setLegacyMoetesakstype(moetesak.getLegacyMoetesakstype());
    dto.setMoetesaksaar(moetesak.getMoetesaksaar());
    dto.setMoetesakssekvensnummer(moetesak.getMoetesakssekvensnummer());
    dto.setVideoLink(moetesak.getVideoLink());
    dto.setUtvalg(moetesak.getUtvalg());
    dto.setLegacyReferanseTilMoetesak(moetesak.getJournalpostIri());

    // AdministrativEnhetObjekt
    dto.setUtvalgObjekt(
        enhetService.maybeExpand(
            moetesak.getUtvalgObjekt(), "administrativEnhetObjekt", paths, currentPath));

    // Utredning
    dto.setUtredning(
        utredningService.maybeExpand(moetesak.getUtredning(), "utredning", paths, currentPath));

    // Innstilling
    dto.setInnstilling(
        moetesaksbeskrivelseService.maybeExpand(
            moetesak.getInnstilling(), "innstilling", paths, currentPath));

    // Vedtak
    dto.setVedtak(vedtakService.maybeExpand(moetesak.getVedtak(), "vedtak", paths, currentPath));

    // Dokumentbeskrivelse
    dto.setDokumentbeskrivelse(
        dokumentbeskrivelseService.maybeExpand(
            moetesak.getDokumentbeskrivelse(), "dokumentbeskrivelse", paths, currentPath));

    // Moetemappe
    dto.setMoetemappe(
        moetemappeService.maybeExpand(moetesak.getMoetemappe(), "moetemappe", paths, currentPath));

    return dto;
  }

  @Override
  public BaseES toLegacyES(Moetesak moetesak) {
    return toLegacyES(moetesak, new MoetesakES());
  }

  @Override
  public BaseES toLegacyES(Moetesak moetesak, BaseES es) {
    super.toLegacyES(moetesak, es);

    if (es instanceof MoetesakES moetesakES) {
      if (moetesak.getMoetemappe() == null || moetesak.getMoetemappe().getMoetedato() == null) {
        moetesakES.setType(List.of("KommerTilBehandlingMøtesaksregistrering"));
      } else {
        moetesakES.setType(List.of("Møtesaksregistrering"));
      }
      moetesakES.setSorteringstype("politisk sak");
      moetesakES.setMøtesaksår(String.valueOf(moetesak.getMoetesaksaar()));
      moetesakES.setMøtesakssekvensnummer(String.valueOf(moetesak.getMoetesakssekvensnummer()));

      var saksaar = String.valueOf(moetesak.getMoetesaksaar());
      var saksaarShort = saksaar.substring(2);
      var sakssekvensnummer = String.valueOf(moetesak.getMoetesakssekvensnummer());
      moetesakES.setSaksnummer(saksaar + "/" + sakssekvensnummer);
      moetesakES.setSaksnummerGenerert(
          List.of(
              saksaar + "/" + sakssekvensnummer,
              saksaarShort + "/" + sakssekvensnummer,
              sakssekvensnummer + "/" + saksaar,
              sakssekvensnummer + "/" + saksaarShort));

      // Parent Moetemappe
      var parent = moetesak.getMoetemappe();
      if (parent != null) {
        var parentES =
            (MoetemappeWithoutChildrenES)
                moetemappeService.toLegacyES(parent, new MoetemappeWithoutChildrenES());
        moetesakES.setParent(parentES);
      }

      // ReferanseTilMoetesak TODO? Is this set in the old import?

      // StandardDato
      moetesakES.setStandardDato(
          TimeConverter.generateStandardDato(
              parent != null ? parent.getMoetedato() : null, moetesak.getPublisertDato()));

      // Dokumentbeskrivelses
      var dokumentbeskrivelse = moetesak.getDokumentbeskrivelse();
      if (dokumentbeskrivelse != null) {
        var dokumentbeskrivelseES =
            dokumentbeskrivelse.stream()
                .map(
                    d ->
                        (DokumentbeskrivelseES)
                            dokumentbeskrivelseService.toLegacyES(d, new DokumentbeskrivelseES()))
                .toList();
        moetesakES.setDokumentbeskrivelse(dokumentbeskrivelseES);
      }
    }

    return es;
  }

  // Dokumentbeskrivelse
  public ResultList<DokumentbeskrivelseDTO> getDokumentbeskrivelseList(
      String moetesakId, DokumentbeskrivelseListQueryDTO query) throws EInnsynException {
    query.setMoetesakId(moetesakId);
    return dokumentbeskrivelseService.list(query);
  }

  /**
   * Add a new dokumentbeskrivelse, or relate an existing one
   *
   * @param moetesakId
   * @param dokumentbeskrivelseDTO
   * @return
   * @throws EInnsynException
   */
  @Transactional(rollbackFor = EInnsynException.class)
  public DokumentbeskrivelseDTO addDokumentbeskrivelse(
      String moetesakId, ExpandableField<DokumentbeskrivelseDTO> dokumentbeskrivelseField)
      throws EInnsynException {

    var dokumentbeskrivelseDTO =
        dokumentbeskrivelseField.getId() == null
            ? dokumentbeskrivelseService.add(dokumentbeskrivelseField.getExpandedObject())
            : dokumentbeskrivelseService.get(dokumentbeskrivelseField.getId());

    var dokumentbeskrivelse = dokumentbeskrivelseService.findById(dokumentbeskrivelseDTO.getId());
    var moetesak = moetesakService.findById(moetesakId);
    moetesak.addDokumentbeskrivelse(dokumentbeskrivelse);

    return dokumentbeskrivelseDTO;
  }

  @Override
  protected Paginators<Moetesak> getPaginators(BaseListQueryDTO params) {
    if (params instanceof MoetesakListQueryDTO p && p.getMoetemappeId() != null) {
      var moetemappe = moetemappeService.findById(p.getMoetemappeId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(moetemappe, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(moetemappe, pivot, pageRequest));
    }
    return super.getPaginators(params);
  }

  @Override
  protected void deleteEntity(Moetesak moetesak) throws EInnsynException {
    // Delete utredning
    var utredning = moetesak.getUtredning();
    if (utredning != null) {
      moetesak.setUtredning(null);
      utredningService.delete(utredning.getId());
    }

    // Delete innstilling
    var innstilling = moetesak.getInnstilling();
    if (innstilling != null) {
      moetesak.setInnstilling(null);
      moetesaksbeskrivelseService.delete(innstilling.getId());
    }

    // Delete vedtak
    var vedtak = moetesak.getVedtak();
    if (vedtak != null) {
      moetesak.setVedtak(null);
      vedtakService.delete(vedtak.getId());
    }

    // Delete all dokumentbeskrivelses
    var dokumentbeskrivelseList = moetesak.getDokumentbeskrivelse();
    if (dokumentbeskrivelseList != null) {
      moetesak.setDokumentbeskrivelse(null);
      for (var dokumentbeskrivelse : dokumentbeskrivelseList) {
        dokumentbeskrivelseService.deleteIfOrphan(dokumentbeskrivelse);
      }
    }

    super.deleteEntity(moetesak);
  }
}
