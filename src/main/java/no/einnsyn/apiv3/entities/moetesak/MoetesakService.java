package no.einnsyn.apiv3.entities.moetesak;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseListQueryDTO;
import no.einnsyn.apiv3.entities.moetesak.models.Moetesak;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.entities.registrering.RegistreringService;
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
  public Moetesak fromDTO(MoetesakDTO dto, Moetesak moetesak, Set<String> paths, String currentPath)
      throws EInnsynException {
    super.fromDTO(dto, moetesak, paths, currentPath);

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

    var enhetskode = dto.getAdministrativEnhet();
    if (enhetskode == null && dto.getMoetemappe() != null) {
      var moetemappe = moetemappeService.findById(dto.getMoetemappe().getId());
      moetesak.setAdministrativEnhet(moetemappe.getUtvalg());
      moetesak.setAdministrativEnhetObjekt(moetemappe.getUtvalgObjekt());
    } else if (enhetskode != null) {
      moetesak.setAdministrativEnhet(enhetskode);
      var journalenhet = moetesak.getJournalenhet();
      var enhet = enhetService.findByEnhetskode(enhetskode, journalenhet);
      if (enhet != null) {
        moetesak.setAdministrativEnhetObjekt(enhet);
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
      moetesak.setUtredning(
          utredningService.insertOrReturnExisting(utredningField, "utredning", paths, currentPath));
    }

    // Innstilling
    var innstillingField = dto.getInnstilling();
    if (innstillingField != null) {
      moetesak.setInnstilling(
          moetesaksbeskrivelseService.insertOrReturnExisting(
              innstillingField, "innstilling", paths, currentPath));
    }

    // Vedtak
    var vedtakField = dto.getVedtak();
    if (vedtakField != null) {
      moetesak.setVedtak(
          vedtakService.insertOrReturnExisting(vedtakField, "vedtak", paths, currentPath));
    }

    // Dokumentbeskrivelse
    var dokumentbeskrivelseFieldList = dto.getDokumentbeskrivelse();
    if (dokumentbeskrivelseFieldList != null) {
      for (var dokumentbeskrivelseField : dokumentbeskrivelseFieldList) {
        moetesak.addDokumentbeskrivelse(
            dokumentbeskrivelseService.insertOrReturnExisting(
                dokumentbeskrivelseField, "dokumentbeskrivelse", paths, currentPath));
      }
    }

    return moetesak;
  }

  @Override
  public MoetesakDTO toDTO(
      Moetesak moetesak, MoetesakDTO dto, Set<String> paths, String currentPath) {
    super.toDTO(moetesak, dto, paths, currentPath);

    dto.setMoetesakstype(moetesak.getMoetesakstype());
    dto.setMoetesaksaar(moetesak.getMoetesaksaar());
    dto.setMoetesakssekvensnummer(moetesak.getMoetesakssekvensnummer());
    dto.setVideoLink(moetesak.getVideoLink());
    dto.setAdministrativEnhet(moetesak.getAdministrativEnhet());

    // AdministrativEnhetObjekt
    var enhet = moetesak.getAdministrativEnhetObjekt();
    if (enhet != null) {
      dto.setAdministrativEnhetObjekt(
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
      String moetesakId, DokumentbeskrivelseListQueryDTO query) {
    query.setMoetesakId(moetesakId);
    return dokumentbeskrivelseService.list(query);
  }

  public DokumentbeskrivelseDTO addDokumentbeskrivelse(
      String moetesakId, DokumentbeskrivelseDTO dokumentbeskrivelseDTO) throws EInnsynException {
    dokumentbeskrivelseDTO = dokumentbeskrivelseService.add(dokumentbeskrivelseDTO);
    var moetesakDTO = newDTO();
    moetesakDTO.setDokumentbeskrivelse(List.of(new ExpandableField<>(dokumentbeskrivelseDTO)));
    moetesakDTO = moetesakService.update(moetesakId, moetesakDTO);
    System.err.println(moetesakDTO.getDokumentbeskrivelse().size());
    return dokumentbeskrivelseService.get(dokumentbeskrivelseDTO.getId());
  }

  @Transactional
  public MoetesakDTO delete(Moetesak object) {
    var dto = proxy.toDTO(object);

    // Delete utredning
    var utredning = object.getUtredning();
    if (utredning != null) {
      utredningService.delete(utredning);
    }

    // Delete innstilling
    var innstilling = object.getInnstilling();
    if (innstilling != null) {
      moetesaksbeskrivelseService.delete(innstilling);
    }

    // Delete vedtak
    var vedtak = object.getVedtak();
    if (vedtak != null) {
      vedtakService.delete(vedtak);
    }

    // Delete all dokumentbeskrivelses
    var dokumentbeskrivelseList = object.getDokumentbeskrivelse();
    if (dokumentbeskrivelseList != null) {
      for (var dokumentbeskrivelse : dokumentbeskrivelseList) {
        dokumentbeskrivelseService.delete(dokumentbeskrivelse);
      }
    }

    dto.setDeleted(true);
    repository.delete(object);
    return dto;
  }
}
