// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.dokumentbeskrivelse.models;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.DokumentobjektDTO;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.features.validation.NoSSN;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;
import no.einnsyn.apiv3.features.validation.validationGroups.Update;

@Getter
@Setter
public class DokumentbeskrivelseDTO extends ArkivBaseDTO {

  @Size(max = 500)
  @Null(groups = { Insert.class, Update.class })
  private final String entity = "Dokumentbeskrivelse";

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = { Insert.class })
  private String tittel;

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = { Insert.class })
  private String tittelSensitiv;

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = { Insert.class })
  private String dokumenttype;

  @NotNull(groups = { Insert.class })
  private Long dokumentnummer;

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = { Insert.class })
  private String tilknyttetRegistreringSom;

  private ExpandableField<DokumentobjektDTO> dokumentobjekt;
}
