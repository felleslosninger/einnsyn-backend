// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.entities.mappe.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

@Getter
@Setter
public class MappeDTO extends ArkivBaseDTO {
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  String offentligTittel;

  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  String offentligTittelSensitiv;

  @NoSSN
  @Size(max = 500)
  String beskrivelse;

  @NoSSN
  @Size(max = 500)
  String noekkelord;

  /**
   * The date the resource was published. This field is updated automatically, but can be set
   * manually by admins.
   */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  String publisertDato;

  /**
   * The date the resource was last updated. This field is updated automatically, but can be set
   * manually by admins.
   */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  String oppdatertDato;

  @Null(groups = {Insert.class, Update.class})
  MappeParent parent;
}
