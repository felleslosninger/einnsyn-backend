// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.mappe.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;

@Getter
@Setter
public abstract class MappeDTO extends ArkivBaseDTO {

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
  @NoSSN
  String noekkelord;

  @Size(max = 500)
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  String publisertDato;

  @Size(max = 500)
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  String oppdatertDato;

  MappeParentDTO parent;
}
