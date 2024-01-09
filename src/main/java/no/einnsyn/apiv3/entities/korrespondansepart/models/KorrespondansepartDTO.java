// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.korrespondansepart.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.features.validation.NoSSN;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;
import no.einnsyn.apiv3.features.validation.validationGroups.Update;

@Getter
@Setter
public class KorrespondansepartDTO extends ArkivBaseDTO {

  @Size(max = 500)
  @Null(groups = { Insert.class, Update.class })
  private final String entity = "Korrespondansepart";

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = { Insert.class })
  private String korrespondansepartNavn;

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = { Insert.class })
  private String korrespondansepartNavnSensitiv;

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = { Insert.class })
  private String korrespondanseparttype;

  @NotNull(groups = { Insert.class })
  @Valid
  private ExpandableField<JournalpostDTO> journalpost;
}
