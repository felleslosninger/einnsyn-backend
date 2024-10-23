// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.innsynskravdel.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.validation.isodatetime.IsoDateTime;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;

@Getter
@Setter
public class InnsynskravDelDTO extends BaseDTO {

  @Size(max = 500)
  final String entity = "InnsynskravDel";

  @Valid ExpandableField<InnsynskravDTO> innsynskrav;

  @NotNull(groups = {Insert.class})
  @Valid
  ExpandableField<JournalpostDTO> journalpost;

  @Null(groups = {Insert.class, Update.class})
  ExpandableField<EnhetDTO> enhet;

  @Size(max = 500)
  @Email
  @Null(groups = {Insert.class, Update.class})
  String email;

  @Size(max = 500)
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  @Null(groups = {Insert.class, Update.class})
  String sent;

  @Null(groups = {Insert.class, Update.class})
  Integer retryCount;

  @Size(max = 500)
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  @Null(groups = {Insert.class, Update.class})
  String retryTimestamp;
}
