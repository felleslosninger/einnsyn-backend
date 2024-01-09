// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.moetesak.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetesaksbeskrivelse.models.MoetesaksbeskrivelseDTO;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringDTO;
import no.einnsyn.apiv3.entities.utredning.models.UtredningDTO;
import no.einnsyn.apiv3.entities.vedtak.models.VedtakDTO;
import no.einnsyn.apiv3.features.validation.NoSSN;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;
import no.einnsyn.apiv3.features.validation.validationGroups.Update;

@Getter
@Setter
public class MoetesakDTO extends RegistreringDTO {

  @Size(max = 500)
  @Null(groups = { Insert.class, Update.class })
  private final String entity = "Moetesak";

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = { Insert.class })
  private String moetesakstype;

  @NotNull(groups = { Insert.class })
  private Long moetesaksaar;

  @NotNull(groups = { Insert.class })
  private Long moetesakssekvensnummer;

  @Size(max = 500)
  @NoSSN
  private String administrativEnhet;

  @Valid
  private ExpandableField<EnhetDTO> administrativEnhetObjekt;

  @Size(max = 500)
  @NoSSN
  private String videoLink;

  @Valid
  private ExpandableField<UtredningDTO> utredning;

  @Valid
  private ExpandableField<MoetesaksbeskrivelseDTO> innstilling;

  @Valid
  private ExpandableField<VedtakDTO> vedtak;

  @Valid
  private ExpandableField<MoetemappeDTO> moetemappe;
}
