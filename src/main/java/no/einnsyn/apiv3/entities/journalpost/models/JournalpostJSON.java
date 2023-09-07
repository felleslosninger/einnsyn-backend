package no.einnsyn.apiv3.entities.journalpost.models;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringJSON;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeJSON;

@Getter
@Setter
public class JournalpostJSON extends RegistreringJSON {
  // private ExpandableField<VirksomhetJSON> arkivskaper;
  private Integer journalaar; // Can we get this from journaldato?
  private Integer journalsekvensnummer;
  private Integer journalpostnummer;
  private String journalposttype;
  private LocalDate journaldato;
  private LocalDate dokumentdato;
  private String journalenhet; // ?
  private String sorteringstype;

  // @ElementCollection
  // @JoinTable(name = "journalpost_følgsakenreferanse",
  // joinColumns = @JoinColumn(name = "journalpost_fra_id"))
  // @Column(name = "journalpost_til_iri")
  // private List<String> følgsakenReferanse = new ArrayList<>();
  // private List<ExpandableField<JournalpostJSON>> følgsakenReferanse = new ArrayList<>();

  private ExpandableField<SaksmappeJSON> saksmappe;

  // private ExpandableField<SkjermingJSON> skjerming;

  // private List<ExpandableField<KorrespondansepartJSON>> korrespondansepart = new ArrayList<>();

  // private List<ExpandableField<DokumentbeskrivelseJSON>> dokumentbeskrivelse = new ArrayList<>();
}
