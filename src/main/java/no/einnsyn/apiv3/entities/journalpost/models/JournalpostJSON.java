package no.einnsyn.apiv3.entities.journalpost.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartJSON;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringJSON;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeJSON;
import no.einnsyn.apiv3.entities.skjerming.models.SkjermingJSON;
import no.einnsyn.apiv3.features.validation.ExistingObject.ExistingObject;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;
import no.einnsyn.apiv3.features.validation.validationGroups.Update;

@Getter
@Setter
public class JournalpostJSON extends RegistreringJSON {

  @Min(value = 1800, message = "Journalaar must be greater than 1800",
      groups = {Insert.class, Update.class})
  @Max(value = 2100, message = "Journalaar must be less than 2100",
      groups = {Insert.class, Update.class})
  @NotNull(groups = {Insert.class})
  private Integer journalaar;

  @NotNull(groups = {Insert.class})
  private Integer journalsekvensnummer;

  @NotNull(groups = {Insert.class})
  private Integer journalpostnummer;

  @NotNull(groups = {Insert.class})
  private String journalposttype;

  @NotNull(groups = {Insert.class})
  private LocalDate journaldato;

  private LocalDate dokumentdato;

  private String sorteringstype;

  // @ElementCollection
  // @JoinTable(name = "journalpost_følgsakenreferanse",
  // joinColumns = @JoinColumn(name = "journalpost_fra_id"))
  // @Column(name = "journalpost_til_iri")
  // private List<String> følgsakenReferanse = new ArrayList<>();
  // private List<ExpandableField<JournalpostJSON>> følgsakenReferanse = new ArrayList<>();

  @Valid
  @ExistingObject(type = Saksmappe.class)
  private ExpandableField<SaksmappeJSON> saksmappe;

  @Valid
  private ExpandableField<SkjermingJSON> skjerming;

  @Valid
  private List<ExpandableField<KorrespondansepartJSON>> korrespondansepart = new ArrayList<>();

  @Valid
  private List<ExpandableField<DokumentbeskrivelseJSON>> dokumentbeskrivelse = new ArrayList<>();
}
