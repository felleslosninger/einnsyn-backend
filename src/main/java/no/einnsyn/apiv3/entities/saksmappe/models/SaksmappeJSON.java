package no.einnsyn.apiv3.entities.saksmappe.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostJSON;
import no.einnsyn.apiv3.entities.mappe.models.MappeJSON;
import no.einnsyn.apiv3.validationGroups.InsertValidationGroup;

@Getter
@Setter
public class SaksmappeJSON extends MappeJSON {

  // Could we get this from `saksdato`?
  @NotNull(groups = {InsertValidationGroup.class})
  private Integer saksaar;

  @NotNull(groups = {InsertValidationGroup.class})
  private Integer sakssekvensnummer;

  @NotNull(groups = {InsertValidationGroup.class})
  private LocalDate saksdato;

  // private ExpandableField<Virksomhet> administrativEnhet;

  @Valid
  private List<ExpandableField<JournalpostJSON>> journalpost =
      new ArrayList<ExpandableField<JournalpostJSON>>();
}
