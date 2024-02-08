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
  public Vedtak fromDTO(VedtakDTO dto, Vedtak vedtak, Set<String> expandPaths, String currentPath)
      throws EInnsynException {
    super.fromDTO(dto, vedtak, expandPaths, currentPath);

    if (dto.getDato() != null) {
      vedtak.setDato(LocalDate.parse(dto.getDato()));
    }

    // Workaround since legacy IDs are used for relations. OneToMany relations fails if the ID is
    // not set.
    if (vedtak.getId() == null) {
      System.err.println("Save and flush vedtak:");
      System.err.println(vedtak.getJournalenhet().getId());
      System.err.println(vedtak.getExternalId());
      System.err.println(vedtak.getId());
      System.err.println(vedtak.getBehandlingsprotokoll());
      System.err.println(vedtak.getVedtakstekst());
      System.err.println(vedtak.getVotering());
      System.err.println(vedtak.getVedtaksdokument());
      vedtak = repository.saveAndFlush(vedtak);
    }

    // Vedtakstekst
    if (dto.getVedtakstekst() != null) {
      vedtak.setVedtakstekst(
          moetesaksbeskrivelseService.insertOrReturnExisting(
              dto.getVedtakstekst(), "vedtakstekst", expandPaths, currentPath));
    }

    // Behandlingsprotokoll
    if (dto.getBehandlingsprotokoll() != null) {
      vedtak.setBehandlingsprotokoll(
          behandlingsprotokollService.insertOrReturnExisting(
              dto.getBehandlingsprotokoll(), "behandlingsprotokoll", expandPaths, currentPath));
    }

    // Votering
    var voteringFieldList = dto.getVotering();
    if (voteringFieldList != null) {
      for (var voteringField : voteringFieldList) {
        vedtak.addVotering(
            voteringService.insertOrReturnExisting(
                voteringField, "votering", expandPaths, currentPath));
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

  @Transactional
  public VedtakDTO delete(Vedtak object) {
    var dto = proxy.toDTO(object);
    dto.setDeleted(true);
    repository.delete(object);
    return dto;
  }
}
