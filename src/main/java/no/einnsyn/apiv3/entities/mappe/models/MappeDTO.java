// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.mappe.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.validation.isodatetime.IsoDateTime;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;

@Getter
@Setter
public abstract class MappeDTO extends ArkivBaseDTO {

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = {Insert.class})
  String offentligTittel;

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = {Insert.class})
  String offentligTittelSensitiv;

  @Size(max = 500)
  @NoSSN
  String beskrivelse;

  @Size(max = 500)
  @NoSSN
  String noekkelord;

  @Size(max = 500)
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
  @Null(groups = {Insert.class, Update.class})
  String publisertDato;

  @Valid MappeParentDTO parent;
}
