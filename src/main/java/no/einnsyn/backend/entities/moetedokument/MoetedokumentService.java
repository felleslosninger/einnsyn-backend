package no.einnsyn.backend.entities.moetedokument;

import java.util.List;
import java.util.Set;
import lombok.Getter;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.paginators.Paginators;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.base.models.BaseES;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseES;
import no.einnsyn.backend.entities.moetedokument.models.ListByMoetedokumentParameters;
import no.einnsyn.backend.entities.moetedokument.models.Moetedokument;
import no.einnsyn.backend.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.backend.entities.moetedokument.models.MoetedokumentES;
import no.einnsyn.backend.entities.moetemappe.models.ListByMoetemappeParameters;
import no.einnsyn.backend.entities.registrering.RegistreringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.retry.annotation.Retryable;
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

  /**
   * Override scheduleIndex to reindex the parent Moetemappe.
   *
   * @param moetedokument
   * @param recurseDirection -1 for parents, 1 for children, 0 for both
   */
  @Override
  public void scheduleIndex(Moetedokument moetedokument, int recurseDirection) {
    super.scheduleIndex(moetedokument, recurseDirection);

    // Reindex parent
    if (recurseDirection <= 0 && moetedokument.getMoetemappe() != null) {
      moetemappeService.scheduleIndex(moetedokument.getMoetemappe(), -1);
    }
  }

  @Override
  protected Moetedokument fromDTO(MoetedokumentDTO dto, Moetedokument moetedokument)
      throws EInnsynException {
    super.fromDTO(dto, moetedokument);

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

    if (moetedokument.getId() == null) {
      moetedokument = repository.saveAndFlush(moetedokument);
    }

    if (dto.getKorrespondansepart() != null) {
      for (var korrespondansepart : dto.getKorrespondansepart()) {
        moetedokument.addKorrespondansepart(
            korrespondansepartService.createOrReturnExisting(korrespondansepart));
      }
    }

    if (dto.getDokumentbeskrivelse() != null) {
      for (var dokumentbeskrivelse : dto.getDokumentbeskrivelse()) {
        moetedokument.addDokumentbeskrivelse(
            dokumentbeskrivelseService.createOrReturnExisting(dokumentbeskrivelse));
      }
    }

    return moetedokument;
  }

  @Override
  protected MoetedokumentDTO toDTO(
      Moetedokument moetedokument,
      MoetedokumentDTO dto,
      Set<String> expandPaths,
      String currentPath) {
    super.toDTO(moetedokument, dto, expandPaths, currentPath);

    dto.setMoetedokumenttype(moetedokument.getMoetedokumentregistreringstype());
    dto.setSaksbehandler(moetedokument.getSaksbehandler());
    dto.setSaksbehandlerSensitiv(moetedokument.getSaksbehandlerSensitiv());

    // Moetemappe
    dto.setMoetemappe(
        moetemappeService.maybeExpand(
            moetedokument.getMoetemappe(), "moetemappe", expandPaths, currentPath));

    // Korrespondansepart
    dto.setKorrespondansepart(
        korrespondansepartService.maybeExpand(
            moetedokument.getKorrespondansepart(), "korrespondansepart", expandPaths, currentPath));

    // Dokumentbeskrivelse
    dto.setDokumentbeskrivelse(
        dokumentbeskrivelseService.maybeExpand(
            moetedokument.getDokumentbeskrivelse(),
            "dokumentbeskrivelse",
            expandPaths,
            currentPath));

    return dto;
  }

  @Override
  public BaseES toLegacyES(Moetedokument moetedokument, BaseES es) {
    super.toLegacyES(moetedokument, es);
    if (es instanceof MoetedokumentES moetedokumentES) {
      moetedokumentES.setType(List.of("Møtedokumentregistrering"));
      moetedokumentES.setMøtedokumentregistreringstype(
          moetedokument.getMoetedokumentregistreringstype());

      var dokumentbeskrivelseList = moetedokument.getDokumentbeskrivelse();
      if (dokumentbeskrivelseList != null) {
        var dokumentbeskrivelseES =
            dokumentbeskrivelseList.stream()
                .map(
                    dokumentbeskrivelse ->
                        (DokumentbeskrivelseES)
                            dokumentbeskrivelseService.toLegacyES(
                                dokumentbeskrivelse, new DokumentbeskrivelseES()))
                .toList();
        moetedokumentES.setDokumentbeskrivelse(dokumentbeskrivelseES);
        for (var dokument : dokumentbeskrivelseES) {
          // ReferanseDokumentfil is mandatory for dokumentobjekt.
          if (dokument.getDokumentobjekt() != null && !dokument.getDokumentobjekt().isEmpty()) {
            moetedokumentES.setFulltext(true);
          }
        }
      } else {
        moetedokumentES.setDokumentbeskrivelse(List.of());
        moetedokumentES.setFulltext(false);
      }
    }
    return es;
  }

  public PaginatedList<DokumentbeskrivelseDTO> listDokumentbeskrivelse(
      String moetedokumentId, ListByMoetedokumentParameters query) throws EInnsynException {
    query.setMoetedokumentId(moetedokumentId);
    return dokumentbeskrivelseService.list(query);
  }

  /**
   * Add a new dokumentbeskrivelse
   *
   * @param moetedokumentId
   * @param dokumentbeskrivelseId
   * @return
   * @throws EInnsynException
   */
  @Transactional(rollbackFor = Exception.class)
  @Retryable
  public DokumentbeskrivelseDTO addDokumentbeskrivelse(
      String moetedokumentId, ExpandableField<DokumentbeskrivelseDTO> dokumentbeskrivelseField)
      throws EInnsynException {

    var dokumentbeskrivelseDTO =
        dokumentbeskrivelseField.getId() == null
            ? dokumentbeskrivelseService.add(dokumentbeskrivelseField.getExpandedObject())
            : dokumentbeskrivelseService.get(dokumentbeskrivelseField.getId());

    var dokumentbeskrivelse = dokumentbeskrivelseService.findById(dokumentbeskrivelseDTO.getId());
    var moetedokument = moetedokumentService.findById(moetedokumentId);
    moetedokument.addDokumentbeskrivelse(dokumentbeskrivelse);
    moetedokumentService.scheduleIndex(moetedokument, -1);

    return dokumentbeskrivelseDTO;
  }

  @Override
  protected Paginators<Moetedokument> getPaginators(ListParameters params) {
    if (params instanceof ListByMoetemappeParameters p && p.getMoetemappeId() != null) {
      var moetemappe = moetemappeService.findById(p.getMoetemappeId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(moetemappe, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(moetemappe, pivot, pageRequest));
    }
    return super.getPaginators(params);
  }

  @Override
  protected void deleteEntity(Moetedokument moetedokument) throws EInnsynException {
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
        korrespondansepartService.delete(korrespondansepart.getId());
      }
    }

    super.deleteEntity(moetedokument);
  }
}
