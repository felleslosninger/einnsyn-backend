package no.einnsyn.apiv3.entities.vedtak;

import java.time.LocalDate;
import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseListQueryDTO;
import no.einnsyn.apiv3.entities.vedtak.models.Vedtak;
import no.einnsyn.apiv3.entities.vedtak.models.VedtakDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VedtakService extends ArkivBaseService<Vedtak, VedtakDTO> {

  @Getter private final VedtakRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private VedtakService proxy;

  public VedtakService(VedtakRepository repository) {
    this.repository = repository;
  }

  public Vedtak newObject() {
    return new Vedtak();
  }

  public VedtakDTO newDTO() {
    return new VedtakDTO();
  }

  @Override
  protected Vedtak fromDTO(VedtakDTO dto, Vedtak vedtak) throws EInnsynException {
    super.fromDTO(dto, vedtak);

    if (dto.getDato() != null) {
      vedtak.setDato(LocalDate.parse(dto.getDato()));
    }

    // Workaround since legacy IDs are used for relations. OneToMany relations fails if the ID is
    // not set.
    if (vedtak.getId() == null) {
      vedtak = repository.saveAndFlush(vedtak);
    }

    // Vedtakstekst
    if (dto.getVedtakstekst() != null) {
      var vedtakstekst = moetesaksbeskrivelseService.createOrReturnExisting(dto.getVedtakstekst());
      // Replace?
      var oldVedtakstekst = vedtak.getVedtakstekst();
      if (oldVedtakstekst != null) {
        vedtak.setVedtakstekst(null);
        moetesaksbeskrivelseService.delete(oldVedtakstekst.getId());
      }
      vedtak.setVedtakstekst(vedtakstekst);
    }

    // Behandlingsprotokoll
    if (dto.getBehandlingsprotokoll() != null) {
      var behandlingsprotokoll =
          behandlingsprotokollService.createOrThrow(dto.getBehandlingsprotokoll());
      // Replace?
      var oldBehandlingsprotokoll = vedtak.getBehandlingsprotokoll();
      if (oldBehandlingsprotokoll != null) {
        vedtak.setBehandlingsprotokoll(null);
        behandlingsprotokollService.delete(oldBehandlingsprotokoll.getId());
      }
      vedtak.setBehandlingsprotokoll(behandlingsprotokoll);
    }

    // Votering
    var voteringFieldList = dto.getVotering();
    if (voteringFieldList != null) {
      for (var voteringField : voteringFieldList) {
        var votering = voteringService.createOrThrow(voteringField);
        vedtak.addVotering(votering);
        votering.setVedtak(vedtak);
      }
    }

    // Vedtaksdokument
    var vedtaksdokumentFieldList = dto.getVedtaksdokument();
    if (vedtaksdokumentFieldList != null) {
      for (var vedtaksdokumentField : vedtaksdokumentFieldList) {
        vedtak.addVedtaksdokument(
            dokumentbeskrivelseService.createOrReturnExisting(vedtaksdokumentField));
      }
    }

    return vedtak;
  }

  @Override
  protected VedtakDTO toDTO(Vedtak vedtak, VedtakDTO dto, Set<String> paths, String currentPath) {
    super.toDTO(vedtak, dto, paths, currentPath);

    dto.setDato(vedtak.getDato().toString());

    var vedtakstekst = vedtak.getVedtakstekst();
    if (vedtakstekst != null) {
      dto.setVedtakstekst(
          moetesaksbeskrivelseService.maybeExpand(
              vedtakstekst, "vedtakstekst", paths, currentPath));
    }

    var behandlingsprotokoll = vedtak.getBehandlingsprotokoll();
    if (behandlingsprotokoll != null) {
      dto.setBehandlingsprotokoll(
          behandlingsprotokollService.maybeExpand(
              behandlingsprotokoll, "behandlingsprotokoll", paths, currentPath));
    }

    var voteringList = vedtak.getVotering();
    if (voteringList != null) {
      dto.setVotering(
          voteringList.stream()
              .map(
                  votering -> voteringService.maybeExpand(votering, "votering", paths, currentPath))
              .toList());
    }

    var vedtaksdokumentList = vedtak.getVedtaksdokument();
    if (vedtaksdokumentList != null) {
      dto.setVedtaksdokument(
          vedtaksdokumentList.stream()
              .map(
                  vedtaksdokument ->
                      dokumentbeskrivelseService.maybeExpand(
                          vedtaksdokument, "vedtaksdokument", paths, currentPath))
              .toList());
    }

    return dto;
  }

  public ResultList<DokumentbeskrivelseDTO> getVedtaksdokumentList(
      String vedtakId, DokumentbeskrivelseListQueryDTO query) throws EInnsynException {
    query.setVedtakId(vedtakId);
    return dokumentbeskrivelseService.list(query);
  }

  @Transactional(rollbackFor = EInnsynException.class)
  public DokumentbeskrivelseDTO addVedtaksdokument(
      String vedtakId, DokumentbeskrivelseDTO dokumentbeskrivelseDTO) throws EInnsynException {
    dokumentbeskrivelseDTO = dokumentbeskrivelseService.add(dokumentbeskrivelseDTO);
    var dokumentbeskrivelse = dokumentbeskrivelseService.findById(dokumentbeskrivelseDTO.getId());
    var vedtak = vedtakService.findById(vedtakId);
    vedtak.addVedtaksdokument(dokumentbeskrivelse);
    return dokumentbeskrivelseDTO;
  }

  @Transactional(rollbackFor = EInnsynException.class)
  public DokumentbeskrivelseDTO deleteVedtaksdokument(String vedtakId, String vedtaksdokumentId)
      throws EInnsynException {
    var vedtak = vedtakService.findById(vedtakId);
    var dokumentbeskrivelse = dokumentbeskrivelseService.findById(vedtaksdokumentId);
    var vedtaksdokumentList = vedtak.getVedtaksdokument();
    if (vedtaksdokumentList != null) {
      vedtak.setVedtaksdokument(
          vedtaksdokumentList.stream()
              .filter(dokument -> !dokument.getId().equals(vedtaksdokumentId))
              .toList());
    }
    return dokumentbeskrivelseService.deleteIfOrphan(dokumentbeskrivelse);
  }

  @Override
  protected void deleteEntity(Vedtak vedtak) throws EInnsynException {
    var vedtakstekst = vedtak.getVedtakstekst();
    if (vedtakstekst != null) {
      vedtak.setVedtakstekst(null);
      moetesaksbeskrivelseService.delete(vedtakstekst.getId());
    }

    var behandlingsprotokoll = vedtak.getBehandlingsprotokoll();
    if (behandlingsprotokoll != null) {
      vedtak.setBehandlingsprotokoll(null);
      behandlingsprotokollService.delete(behandlingsprotokoll.getId());
    }

    var voteringList = vedtak.getVotering();
    if (voteringList != null) {
      vedtak.setVotering(null);
      for (var votering : voteringList) {
        voteringService.delete(votering.getId());
      }
    }

    var vedtaksdokumentList = vedtak.getVedtaksdokument();
    if (vedtaksdokumentList != null) {
      vedtak.setVedtaksdokument(null);
      for (var vedtaksdokument : vedtaksdokumentList) {
        dokumentbeskrivelseService.delete(vedtaksdokument.getId());
      }
    }

    super.deleteEntity(vedtak);
  }
}
