package no.einnsyn.apiv3.entities.saksmappe.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostJSON;
import no.einnsyn.apiv3.entities.mappe.models.MappeJSON;
import no.einnsyn.apiv3.features.validation.NewObject.NewObject;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;
import no.einnsyn.apiv3.features.validation.validationGroups.Update;

@Getter
@Setter
public class SaksmappeJSON extends MappeJSON {

  @NotNull(groups = {Insert.class})
  @Min(value = 1800, message = "Saksaar must be greater than 1800",
      groups = {Insert.class, Update.class})
  @Max(value = 2100, message = "Saksaar must be less than 2100",
      groups = {Insert.class, Update.class})
  private Integer saksaar;

  @NotNull(groups = {Insert.class})
  private Integer sakssekvensnummer;

  private LocalDate saksdato;

  @NewObject(groups = {Insert.class, Update.class})
  @Valid
  private List<ExpandableField<JournalpostJSON>> journalpost =
      new ArrayList<ExpandableField<JournalpostJSON>>();


  // Saksnummer will be autogenerated
  @Null(groups = {Insert.class, Update.class})
  private String saksnummer;

  // SaksnummerGenerert will be autogenerated, used for ElasticSearch only
  private List<String> saksnummerGenerert;

}
