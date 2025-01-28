// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.registrering.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.entities.dokumentbeskrivelse.DokumentbeskrivelseService;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.entities.korrespondansepart.KorrespondansepartService;
import no.einnsyn.backend.entities.korrespondansepart.models.KorrespondansepartDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

@Getter
@Setter
public abstract class RegistreringDTO extends ArkivBaseDTO {

  @Size(max = 500)
  @NoSSN
  @NotBlank(groups = {Insert.class})
  String offentligTittel;

  @Size(max = 500)
  @NoSSN
  @NotBlank(groups = {Insert.class})
  String offentligTittelSensitiv;

  @Size(max = 500)
  @NoSSN
  String beskrivelse;

  @Size(max = 500)
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  String publisertDato;

  @Size(max = 500)
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  String oppdatertDato;

  @ExpandableObject(
      service = KorrespondansepartService.class,
      groups = {Insert.class, Update.class})
  @Valid
  List<ExpandableField<KorrespondansepartDTO>> korrespondansepart;

  @ExpandableObject(
      service = DokumentbeskrivelseService.class,
      groups = {Insert.class, Update.class})
  @Valid
  List<ExpandableField<DokumentbeskrivelseDTO>> dokumentbeskrivelse;

  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<EnhetDTO> avhendetTil;
}
