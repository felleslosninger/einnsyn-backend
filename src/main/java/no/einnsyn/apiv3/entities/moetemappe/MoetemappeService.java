package no.einnsyn.apiv3.entities.moetemappe;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseES;
import no.einnsyn.apiv3.entities.mappe.MappeService;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentListQueryDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.Moetemappe;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeES;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeES.MoetemappeWithoutChildrenES;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakES;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakListQueryDTO;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringES;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.utils.TimestampConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

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
  protected Moetemappe fromDTO(MoetemappeDTO dto, Moetemappe moetemappe) throws EInnsynException {
    super.fromDTO(dto, moetemappe);

    if (dto.getMoetenummer() != null) {
      moetemappe.setMoetenummer(dto.getMoetenummer());
    }

    if (dto.getMoetedato() != null) {
      moetemappe.setMoetedato(TimestampConverter.timestampToInstant(dto.getMoetedato()));
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
        var moetesak = moetesakService.createOrReturnExisting(moetesakField);
        moetemappe.addMoetesak(moetesak);
      }
    }

    // Add Moetedokument
    var moetedokumentFieldList = dto.getMoetedokument();
    if (moetedokumentFieldList != null) {
      for (var moetedokumentField : moetedokumentFieldList) {
        var moetedokument = moetedokumentService.createOrReturnExisting(moetedokumentField);
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
  protected MoetemappeDTO toDTO(
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
    dto.setUtvalgObjekt(
        enhetService.maybeExpand(
            object.getUtvalgObjekt(), "administrativEnhetObjekt", expandPaths, currentPath));

    // Moetesak
    dto.setMoetesak(
        moetesakService.maybeExpand(object.getMoetesak(), "moetesak", expandPaths, currentPath));

    // Moetedokument
    dto.setMoetedokument(
        moetedokumentService.maybeExpand(
            object.getMoetedokument(), "moetedokument", expandPaths, currentPath));

    // ReferanseForrigeMoete
    dto.setReferanseForrigeMoete(
        moetemappeService.maybeExpand(
            object.getReferanseForrigeMoete(), "referanseForrigeMoete", expandPaths, currentPath));

    // ReferanseNesteMoete
    dto.setReferanseNesteMoete(
        moetemappeService.maybeExpand(
            object.getReferanseNesteMoete(), "referanseNesteMoete", expandPaths, currentPath));

    return dto;
  }

  // Build a legacy ElasticSearch document, used by the old API / frontend
  @Override
  public BaseES toLegacyES(Moetemappe object, BaseES es) {
    super.toLegacyES(object, es);
    if (es instanceof MoetemappeES moetemappeES) {
      moetemappeES.setUtvalg(object.getUtvalg());
      moetemappeES.setMoetested(object.getMoetested());
      moetemappeES.setSorteringstype("møtemappe");
      if (object.getMoetedato() != null) {
        moetemappeES.setMoetedato(object.getMoetedato().toString());
        // TODO: setStandardDato
      }

      // Add children if not a MoetemappeWithoutChildrenES
      if (!(moetemappeES instanceof MoetemappeWithoutChildrenES)) {
        var children = object.getMoetesak();
        if (children != null) {
          moetemappeES.setChild(
              children.stream()
                  .map(ms -> (RegistreringES) moetesakService.toLegacyES(ms, new MoetesakES()))
                  .toList());
        }
      }
    }
    return es;
  }

  @Override
  protected void deleteEntity(Moetemappe moetemappe) throws EInnsynException {
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

    super.deleteEntity(moetemappe);
  }

  // Moetedokument
  public ResultList<MoetedokumentDTO> getMoetedokumentList(
      String moetemappeId, MoetedokumentListQueryDTO query) throws EInnsynException {
    query.setMoetemappeId(moetemappeId);
    return moetedokumentService.list(query);
  }

  public MoetedokumentDTO addMoetedokument(String moetemappeId, MoetedokumentDTO dto)
      throws EInnsynException {
    dto.setMoetemappe(new ExpandableField<>(moetemappeId));
    return moetedokumentService.add(dto);
  }

  // Moetesak
  public ResultList<MoetesakDTO> getMoetesakList(String moetemappeId, MoetesakListQueryDTO query)
      throws EInnsynException {
    query.setMoetemappeId(moetemappeId);
    return moetesakService.list(query);
  }

  public MoetesakDTO addMoetesak(String moetemappeId, MoetesakDTO dto) throws EInnsynException {
    dto.setMoetemappe(new ExpandableField<>(moetemappeId));
    return moetesakService.add(dto);
  }
}
