// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.journalpost.models;

import com.google.gson.annotations.SerializedName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.entities.registrering.models.RegistreringDTO;
import no.einnsyn.backend.entities.saksmappe.SaksmappeService;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.entities.skjerming.SkjermingService;
import no.einnsyn.backend.entities.skjerming.models.SkjermingDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;
import no.einnsyn.backend.validation.validenum.ValidEnum;

/**
 * Represents a registry entry for a document, corresponding to the Journalpost in the Noark 5
 * standard. It is a record of an incoming, outgoing, or internal document.
 */
@Getter
@Setter
public class JournalpostDTO extends RegistreringDTO {
  protected final String entity = "Journalpost";

  /** The year the registry entry was created. */
  @Min(1700)
  @NotNull(groups = {Insert.class})
  protected Integer journalaar;

  /** The sequence number of the registry entry within the journal year. */
  @Min(0)
  @NotNull(groups = {Insert.class})
  protected Integer journalsekvensnummer;

  /** The post number within the journal. */
  @Min(0)
  @NotNull(groups = {Insert.class})
  protected Integer journalpostnummer;

  /** The type of registry entry. */
  @ValidEnum(enumClass = JournalposttypeEnum.class)
  @NotNull(groups = {Insert.class})
  protected String journalposttype;

  /** The date the registry entry was recorded. */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
  @NotNull(groups = {Insert.class})
  protected String journaldato;

  /** The date of the document itself. */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
  protected String dokumentetsDato;

  /** Access control information for the registry entry. */
  @ExpandableObject(
      service = SkjermingService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected ExpandableField<SkjermingDTO> skjerming;

  /** Legacy field for the journal post type. */
  @NoSSN
  @Size(max = 500)
  protected String legacyJournalposttype;

  /** Legacy field for references to related cases. */
  protected List<String> legacyFoelgsakenReferanse;

  /** The administrative unit responsible for the registry entry. */
  @NoSSN
  @Size(max = 500)
  protected String administrativEnhet;

  /** The administrative unit responsible for the registry entry. */
  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected ExpandableField<EnhetDTO> administrativEnhetObjekt;

  /** The case this registry entry belongs to. */
  @ExpandableObject(
      service = SaksmappeService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @Null(groups = {Insert.class})
  protected ExpandableField<SaksmappeDTO> saksmappe;

  public enum JournalposttypeEnum {
    @SerializedName("inngaaende_dokument")
    INNGAAENDE_DOKUMENT("inngaaende_dokument"),
    @SerializedName("utgaaende_dokument")
    UTGAAENDE_DOKUMENT("utgaaende_dokument"),
    @SerializedName("organinternt_dokument_uten_oppfoelging")
    ORGANINTERNT_DOKUMENT_UTEN_OPPFOELGING("organinternt_dokument_uten_oppfoelging"),
    @SerializedName("organinternt_dokument_for_oppfoelging")
    ORGANINTERNT_DOKUMENT_FOR_OPPFOELGING("organinternt_dokument_for_oppfoelging"),
    @SerializedName("saksframlegg")
    SAKSFRAMLEGG("saksframlegg"),
    @SerializedName("sakskart")
    SAKSKART("sakskart"),
    @SerializedName("moeteprotokoll")
    MOETEPROTOKOLL("moeteprotokoll"),
    @SerializedName("moetebok")
    MOETEBOK("moetebok"),
    @SerializedName("ukjent")
    UKJENT("ukjent");

    private final String value;

    JournalposttypeEnum(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }

    public String toJson() {
      return value;
    }

    public static JournalposttypeEnum fromValue(String value) {
      value = value.trim().toLowerCase();
      for (JournalposttypeEnum val : JournalposttypeEnum.values()) {
        if (val.value.toLowerCase().equals(value)) {
          return val;
        }
      }
      throw new IllegalArgumentException("Unknown value: " + value);
    }
  }
}
