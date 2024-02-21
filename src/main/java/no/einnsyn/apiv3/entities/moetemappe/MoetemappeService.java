package no.einnsyn.apiv3.entities.moetemappe;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.mappe.MappeService;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentListQueryDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.Moetemappe;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakListQueryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MoetemappeService extends MappeService<Moetemappe, MoetemappeDTO> {

  @Getter private final MoetemappeRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private MoetemappeService proxy;

  public MoetemappeService(MoetemappeRepository repository) {
    this.repository = repository;
  }

  public Moetemappe newObject() {
    return new Moetemappe();
  }

  public MoetemappeDTO newDTO() {
    return new MoetemappeDTO();
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Moetemappe fromDTO(
      MoetemappeDTO dto, Moetemappe moetemappe, Set<String> paths, String currentPath)
      throws EInnsynException {
    super.fromDTO(dto, moetemappe, paths, currentPath);

    if (dto.getMoetenummer() != null) {
      moetemappe.setMoetenummer(dto.getMoetenummer());
    }

    if (dto.getMoetedato() != null) {
      moetemappe.setMoetedato(Instant.parse(dto.getMoetedato()));
    }

    if (dto.getMoetested() != null) {
      moetemappe.setMoetested(dto.getMoetested());
    }

    // Look up Enhet for "utvalg", if given
    var utvalgKode = dto.getUtvalg();
    if (utvalgKode != null) {
      moetemappe.setUtvalg(utvalgKode);
      var journalenhet = moetemappe.getJournalenhet();
      var utvalg = enhetService.findByEnhetskode(utvalgKode, journalenhet);
      if (utvalg != null) {
        moetemappe.setUtvalgObjekt(utvalg);
      }
    }

    // Fallback to journalenhet for "utvalg"
    if (moetemappe.getUtvalgObjekt() == null) {
      moetemappe.setUtvalgObjekt(moetemappe.getJournalenhet());
    }

    if (dto.getVideoLink() != null) {
      moetemappe.setVideolink(dto.getVideoLink());
    }

    // Workaround since legacy IDs are used for relations. OneToMany relations fails if the ID is
    // not set.
    if (moetemappe.getId() == null) {
      moetemappe = repository.saveAndFlush(moetemappe);
    }

    // Add Moetesak
    var moetesakFieldList = dto.getMoetesak();
    if (moetesakFieldList != null) {
      for (var moetesakField : moetesakFieldList) {
        var moetesak =
            moetesakService.insertOrReturnExisting(moetesakField, "moetesak", paths, currentPath);
        moetesak.setMoetemappe(moetemappe);
        moetemappe.addMoetesak(moetesak);
      }
    }

    // Add Moetedokument
    var moetedokumentFieldList = dto.getMoetedokument();
    if (moetedokumentFieldList != null) {
      for (var moetedokumentField : moetedokumentFieldList) {
        var moetedokument =
            moetedokumentService.insertOrReturnExisting(
                moetedokumentField, "moetedokument", paths, currentPath);
        moetedokument.setMoetemappe(moetemappe);
        moetemappe.addMoetedokument(moetedokument);
      }
    }

    // Add referanseForrigeMoete
    var referanseForrigeMoeteField = dto.getReferanseForrigeMoete();
    if (referanseForrigeMoeteField != null) {
      var forrigeMoete = moetemappeService.findById(referanseForrigeMoeteField.getId());
      moetemappe.setReferanseForrigeMoete(forrigeMoete);
      forrigeMoete.setReferanseNesteMoete(moetemappe);
    }

    // Add referanseNesteMoete
    var referanseNesteMoeteField = dto.getReferanseNesteMoete();
    if (referanseNesteMoeteField != null) {
      var nesteMoete = moetemappeService.findById(referanseNesteMoeteField.getId());
      moetemappe.setReferanseNesteMoete(nesteMoete);
      nesteMoete.setReferanseForrigeMoete(moetemappe);
    }

    return moetemappe;
  }

  @Override
  public MoetemappeDTO toDTO(
      Moetemappe object, MoetemappeDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(object, dto, expandPaths, currentPath);

    dto.setMoetenummer(object.getMoetenummer());
    dto.setMoetested(object.getMoetested());
    dto.setVideoLink(object.getVideolink());
    dto.setUtvalg(object.getUtvalg());

    if (object.getMoetedato() != null) {
      dto.setMoetedato(object.getMoetedato().toString());
    }

    // Utvalg
    var utvalgObjekt = object.getUtvalgObjekt();
    if (utvalgObjekt != null) {
      dto.setUtvalgObjekt(
          enhetService.maybeExpand(
              utvalgObjekt, "administrativEnhetObjekt", expandPaths, currentPath));
    }

    // Moetesak
    var moetesakListDTO = dto.getMoetesak();
    if (moetesakListDTO == null) {
      moetesakListDTO = new ArrayList<>();
      dto.setMoetesak(moetesakListDTO);
    }
    var moetesakList = object.getMoetesak();
    if (moetesakList != null) {
      for (var moetesak : moetesakList) {
        moetesakListDTO.add(
            moetesakService.maybeExpand(moetesak, "moetesak", expandPaths, currentPath));
      }
    }

    // Moetedokument
    var moetedokumentListDTO = dto.getMoetedokument();
    if (moetedokumentListDTO == null) {
      moetedokumentListDTO = new ArrayList<>();
      dto.setMoetedokument(moetedokumentListDTO);
    }
    var moetedokumentList = object.getMoetedokument();
    if (moetedokumentList != null) {
      for (var moetedokument : moetedokumentList) {
        moetedokumentListDTO.add(
            moetedokumentService.maybeExpand(
                moetedokument, "moetedokument", expandPaths, currentPath));
      }
    }

    // ReferanseForrigeMoete
    var referanseForrigeMoete = object.getReferanseForrigeMoete();
    if (referanseForrigeMoete != null) {
      dto.setReferanseForrigeMoete(
          moetemappeService.maybeExpand(
              referanseForrigeMoete, "referanseForrigeMoete", expandPaths, currentPath));
    }

    // ReferanseNesteMoete
    var referanseNesteMoete = object.getReferanseNesteMoete();
    if (referanseNesteMoete != null) {
      dto.setReferanseNesteMoete(
          moetemappeService.maybeExpand(
              referanseNesteMoete, "referanseNesteMoete", expandPaths, currentPath));
    }

    return dto;
  }

  @Override
  protected MoetemappeDTO delete(Moetemappe moetemappe) throws EInnsynException {
    // Delete Moetesak
    var moetesakList = moetemappe.getMoetesak();
    if (moetesakList != null) {
      moetemappe.setMoetesak(null);
      for (var moetesak : moetesakList) {
        moetesakService.delete(moetesak.getId());
      }
    }

    // Delete Moetedokument
    var moetedokumentList = moetemappe.getMoetedokument();
    if (moetedokumentList != null) {
      moetemappe.setMoetedokument(null);
      for (var moetedokument : moetedokumentList) {
        moetedokumentService.delete(moetedokument.getId());
      }
    }

    // Remove referanseForrigeMoete
    var referanseForrigeMoete = moetemappe.getReferanseForrigeMoete();
    if (referanseForrigeMoete != null) {
      referanseForrigeMoete.setReferanseNesteMoete(null);
    }

    // Remove referanseNesteMoete
    var referanseNesteMoete = moetemappe.getReferanseNesteMoete();
    if (referanseNesteMoete != null) {
      referanseNesteMoete.setReferanseForrigeMoete(null);
    }

    return super.delete(moetemappe);
  }

  // Moetedokument
  public ResultList<MoetedokumentDTO> getMoetedokumentList(
      String moetemappeId, MoetedokumentListQueryDTO query) {
    query.setMoetemappeId(moetemappeId);
    return moetedokumentService.list(query);
  }

  public MoetedokumentDTO addMoetedokument(String moetemappeId, MoetedokumentDTO dto)
      throws EInnsynException {
    dto.setMoetemappe(new ExpandableField<>(moetemappeId));
    return moetedokumentService.add(dto);
  }

  // Moetesak
  public ResultList<MoetesakDTO> getMoetesakList(String moetemappeId, MoetesakListQueryDTO query) {
    query.setMoetemappeId(moetemappeId);
    return moetesakService.list(query);
  }

  public MoetesakDTO addMoetesak(String moetemappeId, MoetesakDTO dto) throws EInnsynException {
    dto.setMoetemappe(new ExpandableField<>(moetemappeId));
    return moetesakService.add(dto);
  }
}
