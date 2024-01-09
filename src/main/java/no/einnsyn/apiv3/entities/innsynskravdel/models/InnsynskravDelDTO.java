// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.innsynskravdel.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.features.validation.nossn.NoSSN;
import no.einnsyn.apiv3.features.validation.validationgroups.Insert;
import no.einnsyn.apiv3.features.validation.validationgroups.Update;

@Getter
@Setter
public class InnsynskravDelDTO extends BaseDTO {

  @Size(max = 500)
  @Null(groups = {Insert.class, Update.class})
  private final String entity = "InnsynskravDel";

  @NotNull(groups = {Insert.class})
  @Valid
  private ExpandableField<InnsynskravDTO> innsynskrav;

  @NotNull(groups = {Insert.class})
  @Valid
  private ExpandableField<JournalpostDTO> journalpost;
}
