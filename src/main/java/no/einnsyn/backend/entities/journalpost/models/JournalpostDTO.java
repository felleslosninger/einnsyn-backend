// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.journalpost.models;

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

/** Journalpost */
@Getter
@Setter
public class JournalpostDTO extends RegistreringDTO {
  protected final String entity = "Journalpost";

  @Min(1700)
  @NotNull(groups = {Insert.class})
  protected Integer journalaar;

  @Min(0)
  @NotNull(groups = {Insert.class})
  protected Integer journalsekvensnummer;

  @Min(0)
  @NotNull(groups = {Insert.class})
  protected Integer journalpostnummer;

  @ValidEnum(enumClass = JournalposttypeEnum.class)
  @NotNull(groups = {Insert.class})
  protected String journalposttype;

  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
  @NotNull(groups = {Insert.class})
  protected String journaldato;

  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
  protected String dokumentetsDato;

  @ExpandableObject(
      service = SkjermingService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected ExpandableField<SkjermingDTO> skjerming;

  @NoSSN
  @Size(max = 500)
  protected String legacyJournalposttype;

  protected List<String> legacyFoelgsakenReferanse;

  @NoSSN
  @Size(max = 500)
  protected String administrativEnhet;

  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected ExpandableField<EnhetDTO> administrativEnhetObjekt;

  @ExpandableObject(
      service = SaksmappeService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @Null(groups = {Insert.class, Update.class})
  protected ExpandableField<SaksmappeDTO> saksmappe;

  public enum JournalposttypeEnum {
    INNGAAENDE_DOKUMENT("inngaaende_dokument"),
    UTGAAENDE_DOKUMENT("utgaaende_dokument"),
    ORGANINTERNT_DOKUMENT_UTEN_OPPFOELGING("organinternt_dokument_uten_oppfoelging"),
    ORGANINTERNT_DOKUMENT_FOR_OPPFOELGING("organinternt_dokument_for_oppfoelging"),
    SAKSFRAMLEGG("saksframlegg"),
    SAKSKART("sakskart"),
    MOETEPROTOKOLL("moeteprotokoll"),
    MOETEBOK("moetebok"),
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
