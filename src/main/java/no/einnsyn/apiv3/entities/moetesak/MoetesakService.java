package no.einnsyn.apiv3.entities.moetesak;

import java.util.ArrayList;
import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.paginators.Paginators;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseListQueryDTO;
import no.einnsyn.apiv3.entities.moetesak.models.Moetesak;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakListQueryDTO;
import no.einnsyn.apiv3.entities.registrering.RegistreringService;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
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

  @Override
  protected Moetesak fromDTO(MoetesakDTO dto, Moetesak moetesak) throws EInnsynException {
    super.fromDTO(dto, moetesak);

    if (dto.getMoetesakstype() != null) {
      moetesak.setMoetesakstype(dto.getMoetesakstype());
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

    var enhetskode = dto.getUtvalg();
    if (enhetskode == null && dto.getMoetemappe() != null) {
      var moetemappe = moetemappeService.findById(dto.getMoetemappe().getId());
      moetesak.setUtvalg(moetemappe.getUtvalg());
      moetesak.setUtvalgObjekt(moetemappe.getUtvalgObjekt());
    } else if (enhetskode != null) {
      moetesak.setUtvalg(enhetskode);
      var journalenhet = moetesak.getJournalenhet();
      var enhet = enhetService.findByEnhetskode(enhetskode, journalenhet);
      if (enhet != null) {
        moetesak.setUtvalgObjekt(enhet);
      }
    }

    if (dto.getMoetemappe() != null) {
      moetesak.setMoetemappe(moetemappeService.returnExistingOrThrow(dto.getMoetemappe()));
    }

    // Workaround since legacy IDs are used for relations. OneToMany relations fails if the ID is
    // not set.
    if (moetesak.getId() == null) {
      moetesak = repository.saveAndFlush(moetesak);
    }

    // Utredning
    var utredningField = dto.getUtredning();
    if (utredningField != null) {
      // Replace?
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
      // Replace?
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
    dto.setMoetesaksaar(moetesak.getMoetesaksaar());
    dto.setMoetesakssekvensnummer(moetesak.getMoetesakssekvensnummer());
    dto.setVideoLink(moetesak.getVideoLink());
    dto.setUtvalg(moetesak.getUtvalg());

    // AdministrativEnhetObjekt
    var enhet = moetesak.getUtvalgObjekt();
    if (enhet != null) {
      dto.setUtvalgObjekt(
          enhetService.maybeExpand(enhet, "administrativEnhetObjekt", paths, currentPath));
    }

    // Utredning
    var utredning = moetesak.getUtredning();
    if (utredning != null) {
      dto.setUtredning(utredningService.maybeExpand(utredning, "utredning", paths, currentPath));
    }

    // Innstilling
    var innstilling = moetesak.getInnstilling();
    if (innstilling != null) {
      dto.setInnstilling(
          moetesaksbeskrivelseService.maybeExpand(innstilling, "innstilling", paths, currentPath));
    }

    // Vedtak
    var vedtak = moetesak.getVedtak();
    if (vedtak != null) {
      dto.setVedtak(vedtakService.maybeExpand(vedtak, "vedtak", paths, currentPath));
    }

    // Dokumentbeskrivelse
    var dokumentbeskrivelseListDTO = dto.getDokumentbeskrivelse();
    if (dokumentbeskrivelseListDTO == null) {
      dokumentbeskrivelseListDTO = new ArrayList<>();
      dto.setDokumentbeskrivelse(dokumentbeskrivelseListDTO);
    }
    var dokumentbeskrivelseList = moetesak.getDokumentbeskrivelse();
    if (dokumentbeskrivelseList != null) {
      for (var dokumentbeskrivelse : dokumentbeskrivelseList) {
        dokumentbeskrivelseListDTO.add(
            dokumentbeskrivelseService.maybeExpand(
                dokumentbeskrivelse, "dokumentbeskrivelse", paths, currentPath));
      }
    }

    return dto;
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
  @Transactional
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
