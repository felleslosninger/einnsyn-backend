// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.entities.lagretsoek.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.search.models.SearchParameters;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.bruker.BrukerService;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

/** LagretSoek */
@Getter
@Setter
public class LagretSoekDTO extends BaseDTO {
  final String entity = "LagretSoek";

  @ExpandableObject(
      service = BrukerService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<BrukerDTO> bruker;

  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  String label;

  Boolean subscribe;

  SearchParameters searchParameters;

  @NoSSN
  @Size(max = 500)
  String legacyQuery;
}
