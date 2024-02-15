package no.einnsyn.apiv3.entities.moetedokument;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.paginators.Paginators;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseListQueryDTO;
import no.einnsyn.apiv3.entities.moetedokument.models.Moetedokument;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentListQueryDTO;
import no.einnsyn.apiv3.entities.registrering.RegistreringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MoetedokumentService extends RegistreringService<Moetedokument, MoetedokumentDTO> {

  @Getter private final MoetedokumentRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private MoetedokumentService proxy;

  public MoetedokumentService(MoetedokumentRepository repository) {
    this.repository = repository;
  }

  public Moetedokument newObject() {
    return new Moetedokument();
  }

  public MoetedokumentDTO newDTO() {
    return new MoetedokumentDTO();
  }

  @Override
  public Moetedokument fromDTO(
      MoetedokumentDTO dto, Moetedokument moetedokument, Set<String> paths, String currentPath)
      throws EInnsynException {
    super.fromDTO(dto, moetedokument, paths, currentPath);

    if (dto.getMoetedokumenttype() != null) {
      moetedokument.setMoetedokumentregistreringstype(dto.getMoetedokumenttype());
    }

    if (dto.getSaksbehandler() != null) {
      moetedokument.setSaksbehandler(dto.getSaksbehandler());
    }

    if (dto.getSaksbehandlerSensitiv() != null) {
      moetedokument.setSaksbehandlerSensitiv(dto.getSaksbehandlerSensitiv());
    }

    if (dto.getMoetemappe() != null) {
      moetedokument.setMoetemappe(moetemappeService.returnExistingOrThrow(dto.getMoetemappe()));
    }

    // Workaround since legacy IDs are used for relations. OneToMany relations fails if the ID is
    // not set.
    if (moetedokument.getId() == null) {
      moetedokument = repository.saveAndFlush(moetedokument);
    }

    if (dto.getKorrespondansepart() != null) {
      for (var korrespondansepart : dto.getKorrespondansepart()) {
        moetedokument.addKorrespondansepart(
            korrespondansepartService.insertOrReturnExisting(
                korrespondansepart, "korrespondansepart", paths, currentPath));
      }
    }

    if (dto.getDokumentbeskrivelse() != null) {
      for (var dokumentbeskrivelse : dto.getDokumentbeskrivelse()) {
        moetedokument.addDokumentbeskrivelse(
            dokumentbeskrivelseService.insertOrReturnExisting(
                dokumentbeskrivelse, "dokumentbeskrivelse", paths, currentPath));
      }
    }

    return moetedokument;
  }

  @Override
  public MoetedokumentDTO toDTO(
      Moetedokument moetedokument,
      MoetedokumentDTO dto,
      Set<String> expandPaths,
      String currentPath) {
    super.toDTO(moetedokument, dto, expandPaths, currentPath);

    dto.setMoetedokumenttype(moetedokument.getMoetedokumentregistreringstype());
    dto.setSaksbehandler(moetedokument.getSaksbehandler());
    dto.setSaksbehandlerSensitiv(moetedokument.getSaksbehandlerSensitiv());

    if (moetedokument.getMoetemappe() != null) {
      dto.setMoetemappe(
          moetemappeService.maybeExpand(
              moetedokument.getMoetemappe(), "moetemappe", expandPaths, currentPath));
    }

    if (moetedokument.getKorrespondansepart() != null) {
      var korrespondansepartList = moetedokument.getKorrespondansepart();
      dto.setKorrespondansepart(
          korrespondansepartList.stream()
              .map(
                  korrespondansepart ->
                      korrespondansepartService.maybeExpand(
                          korrespondansepart, "korrespondansepart", expandPaths, currentPath))
              .toList());
    }

    if (moetedokument.getDokumentbeskrivelse() != null) {
      var dokumentbeskrivelseList = moetedokument.getDokumentbeskrivelse();
      dto.setDokumentbeskrivelse(
          dokumentbeskrivelseList.stream()
              .map(
                  dokumentbeskrivelse ->
                      dokumentbeskrivelseService.maybeExpand(
                          dokumentbeskrivelse, "dokumentbeskrivelse", expandPaths, currentPath))
              .toList());
    }

    return dto;
  }

  public ResultList<DokumentbeskrivelseDTO> getDokumentbeskrivelseList(
      String moetedokumentId, DokumentbeskrivelseListQueryDTO query) {
    query.setMoetedokumentId(moetedokumentId);
    return dokumentbeskrivelseService.list(query);
  }

  @Transactional
  public DokumentbeskrivelseDTO addDokumentbeskrivelse(
      String moetedokumentId, DokumentbeskrivelseDTO dokumentbeskrivelseDTO)
      throws EInnsynException {
    dokumentbeskrivelseDTO = dokumentbeskrivelseService.add(dokumentbeskrivelseDTO);
    var dokumentbeskrivelse = dokumentbeskrivelseService.findById(dokumentbeskrivelseDTO.getId());
    var moetedokument = moetedokumentService.findById(moetedokumentId);
    moetedokument.addDokumentbeskrivelse(dokumentbeskrivelse);
    return dokumentbeskrivelseDTO;
  }

  @Override
  public Paginators<Moetedokument> getPaginators(BaseListQueryDTO params) {
    if (params instanceof MoetedokumentListQueryDTO p && p.getMoetemappeId() != null) {
      var moetemappe = moetemappeService.findById(p.getMoetemappeId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(moetemappe, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(moetemappe, pivot, pageRequest));
    }
    return super.getPaginators(params);
  }

  @Transactional
  @Override
  public MoetedokumentDTO delete(Moetedokument moetedokument) throws EInnsynException {
    // Dokumentbeskrivelse
    var dokumentbeskrivelseList = moetedokument.getDokumentbeskrivelse();
    if (dokumentbeskrivelseList != null) {
      moetedokument.setDokumentbeskrivelse(null);
      for (var dokumentbeskrivelse : dokumentbeskrivelseList) {
        dokumentbeskrivelseService.deleteIfOrphan(dokumentbeskrivelse);
      }
    }

    // Korrespondansepart
    var korrespondansepartList = moetedokument.getKorrespondansepart();
    if (korrespondansepartList != null) {
      moetedokument.setKorrespondansepart(null);
      for (var korrespondansepart : korrespondansepartList) {
        korrespondansepartService.delete(korrespondansepart);
      }
    }

    return super.delete(moetedokument);
  }
}
