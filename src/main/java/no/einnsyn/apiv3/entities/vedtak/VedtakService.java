package no.einnsyn.apiv3.entities.vedtak;

import java.time.LocalDate;
import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.vedtak.models.Vedtak;
import no.einnsyn.apiv3.entities.vedtak.models.VedtakDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
  @Transactional(propagation = Propagation.MANDATORY)
  public Vedtak fromDTO(VedtakDTO dto, Vedtak vedtak, Set<String> expandPaths, String currentPath)
      throws EInnsynException {
    super.fromDTO(dto, vedtak, expandPaths, currentPath);

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
      var vedtakstekst =
          moetesaksbeskrivelseService.insertOrReturnExisting(
              dto.getVedtakstekst(), "vedtakstekst", expandPaths, currentPath);
      // Replace?
      var oldVedtakstekst = vedtak.getVedtakstekst();
      if (oldVedtakstekst != null) {
        vedtak.setVedtakstekst(null);
        moetesaksbeskrivelseService.delete(oldVedtakstekst);
      }
      vedtak.setVedtakstekst(vedtakstekst);
    }

    // Behandlingsprotokoll
    if (dto.getBehandlingsprotokoll() != null) {
      var behandlingsprotokoll =
          behandlingsprotokollService.insertOrThrow(
              dto.getBehandlingsprotokoll(), "behandlingsprotokoll", expandPaths, currentPath);
      // Replace?
      var oldBehandlingsprotokoll = vedtak.getBehandlingsprotokoll();
      if (oldBehandlingsprotokoll != null) {
        vedtak.setBehandlingsprotokoll(null);
        behandlingsprotokollService.delete(oldBehandlingsprotokoll);
      }
      vedtak.setBehandlingsprotokoll(behandlingsprotokoll);
    }

    // Votering
    var voteringFieldList = dto.getVotering();
    if (voteringFieldList != null) {
      for (var voteringField : voteringFieldList) {
        var votering =
            voteringService.insertOrThrow(voteringField, "votering", expandPaths, currentPath);
        vedtak.addVotering(votering);
        votering.setVedtak(vedtak);
      }
    }

    // Vedtaksdokumenter
    var vedtaksdokumenterFieldList = dto.getVedtaksdokument();
    if (vedtaksdokumenterFieldList != null) {
      for (var vedtaksdokumenterField : vedtaksdokumenterFieldList) {
        vedtak.addVedtaksdokument(
            dokumentbeskrivelseService.insertOrReturnExisting(
                vedtaksdokumenterField, "vedtaksdokumenter", expandPaths, currentPath));
      }
    }

    return vedtak;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public VedtakDTO toDTO(Vedtak vedtak, VedtakDTO dto, Set<String> paths, String currentPath) {
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

  @Transactional(propagation = Propagation.MANDATORY)
  public VedtakDTO delete(Vedtak vedtak) {
    var dto = proxy.toDTO(vedtak);

    var vedtakstekst = vedtak.getVedtakstekst();
    if (vedtakstekst != null) {
      vedtak.setVedtakstekst(null);
      moetesaksbeskrivelseService.delete(vedtakstekst);
    }

    var behandlingsprotokoll = vedtak.getBehandlingsprotokoll();
    if (behandlingsprotokoll != null) {
      vedtak.setBehandlingsprotokoll(null);
      behandlingsprotokollService.delete(behandlingsprotokoll);
    }

    if (vedtak.getVotering() != null) {
      for (var votering : vedtak.getVotering()) {
        voteringService.delete(votering);
      }
    }

    if (vedtak.getVedtaksdokument() != null) {
      for (var vedtaksdokument : vedtak.getVedtaksdokument()) {
        dokumentbeskrivelseService.delete(vedtaksdokument);
      }
    }

    dto.setDeleted(true);
    repository.delete(vedtak);
    return dto;
  }
}
