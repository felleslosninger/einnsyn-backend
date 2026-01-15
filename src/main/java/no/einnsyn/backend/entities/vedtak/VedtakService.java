package no.einnsyn.backend.entities.vedtak;

import java.time.LocalDate;
import java.util.Set;
import lombok.Getter;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.moetesak.MoetesakRepository;
import no.einnsyn.backend.entities.vedtak.models.ListByVedtakParameters;
import no.einnsyn.backend.entities.vedtak.models.Vedtak;
import no.einnsyn.backend.entities.vedtak.models.VedtakDTO;
import no.einnsyn.backend.entities.votering.models.VoteringDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VedtakService extends ArkivBaseService<Vedtak, VedtakDTO> {

  @Getter private final VedtakRepository repository;

  private final MoetesakRepository moetesakRepository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private VedtakService proxy;

  public VedtakService(VedtakRepository repository, MoetesakRepository moetesakRepository) {
    this.repository = repository;
    this.moetesakRepository = moetesakRepository;
  }

  public Vedtak newObject() {
    return new Vedtak();
  }

  public VedtakDTO newDTO() {
    return new VedtakDTO();
  }

  /**
   * Override scheduleIndex to also reindex the parent moetesak.
   *
   * @param utredning
   * @param recurseDirection -1 for parents, 1 for children, 0 for both
   */
  @Override
  public boolean scheduleIndex(String vedtakId, int recurseDirection) {
    var isScheduled = super.scheduleIndex(vedtakId, recurseDirection);

    // Index moetesak
    if (recurseDirection <= 0 && !isScheduled) {
      var moetesakId = moetesakRepository.findIdByVedtakId(vedtakId);
      if (moetesakId != null) {
        moetesakService.scheduleIndex(moetesakId, -1);
      }
    }

    return true;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
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
  @Transactional(propagation = Propagation.MANDATORY)
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

  public PaginatedList<VoteringDTO> listVotering(String vedtakId, ListByVedtakParameters query)
      throws EInnsynException {
    query.setVedtakId(vedtakId);
    return voteringService.list(query);
  }

  @Transactional(rollbackFor = Exception.class)
  @Retryable
  public VoteringDTO addVotering(String vedtakId, VoteringDTO voteringField)
      throws EInnsynException {
    var votering = voteringService.createOrThrow(new ExpandableField<>(voteringField));
    var vedtak = vedtakService.findByIdOrThrow(vedtakId);
    vedtak.addVotering(votering);
    vedtakService.scheduleIndex(vedtakId, -1);

    return voteringService.get(votering.getId());
  }

  public PaginatedList<DokumentbeskrivelseDTO> listVedtaksdokument(
      String vedtakId, ListByVedtakParameters query) throws EInnsynException {
    query.setVedtakId(vedtakId);
    return dokumentbeskrivelseService.list(query);
  }

  @Transactional(rollbackFor = Exception.class)
  @Retryable
  public DokumentbeskrivelseDTO addVedtaksdokument(
      String vedtakId, ExpandableField<DokumentbeskrivelseDTO> dokumentbeskrivelseField)
      throws EInnsynException {
    var dokumentbeskrivelse =
        dokumentbeskrivelseService.createOrReturnExisting(dokumentbeskrivelseField);
    var vedtak = vedtakService.findByIdOrThrow(vedtakId);
    vedtak.addVedtaksdokument(dokumentbeskrivelse);
    vedtakService.scheduleIndex(vedtakId, -1);
    return dokumentbeskrivelseService.get(dokumentbeskrivelse.getId());
  }

  @Transactional(rollbackFor = Exception.class)
  @Retryable
  public DokumentbeskrivelseDTO deleteVedtaksdokument(String vedtakId, String vedtaksdokumentId)
      throws EInnsynException {
    var vedtak = vedtakService.findByIdOrThrow(vedtakId);
    var dokumentbeskrivelse = dokumentbeskrivelseService.findByIdOrThrow(vedtaksdokumentId);
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
  @Transactional(propagation = Propagation.MANDATORY)
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

    // Remove link from moetesak
    var moetesak = moetesakRepository.findByVedtak(vedtak);
    if (moetesak != null) {
      moetesak.setVedtak(null);
    }

    super.deleteEntity(vedtak);
  }
}
