// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.common.search.models;

import com.google.gson.annotations.SerializedName;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validenum.ValidEnum;

/** A set of search parameters stored as plain data, e.g. on a saved search. */
@Getter
@Setter
public class SavedSearchParameters {
  /** Specifies which fields in the response should be expanded. */
  protected List<String> expand;

  /**
   * A limit on the number of objects to be returned. Limit can range between 1 and 100, and the
   * default is 25.
   */
  @Min(1)
  @Max(100)
  protected Integer limit = 25;

  /** The sort order of the result set. The default is ascending. */
  @ValidEnum(enumClass = SortOrderEnum.class)
  protected String sortOrder = "desc";

  /**
   * A cursor for use in pagination. This is a list of size two, the value of the sortBy property
   * and the unique id.
   */
  @Size(min = 2, max = 2)
  protected List<String> startingAfter;

  /**
   * A cursor for use in pagination. This is a list of size two, the value of the sortBy property
   * and the unique id.
   */
  @Size(min = 2, max = 2)
  protected List<String> endingBefore;

  /** The field to sort results by. The default is "score". */
  @ValidEnum(enumClass = SortByEnum.class)
  protected String sortBy = "score";

  /**
   * A query string to filter by. Quotes can be used to search for exact matches or phrases. Words
   * can be excluded by prefixing them with a minus sign.
   */
  @NoSSN
  @Size(max = 500)
  protected String query;

  /** A list of enhet IDs to filter by. This will also match subenhets. */
  protected List<String> administrativEnhet;

  /** A list of enhet IDs to filter by. This will only match the specified enhets, not subenhets. */
  protected List<String> administrativEnhetExact;

  /** A list of enhet IDs to exclude from the result set. This will also exclude subenhets. */
  protected List<String> excludeAdministrativEnhet;

  /**
   * A list of enhet IDs to exclude from the result set. This will only exclude the specified
   * enhets, not subenhets.
   */
  protected List<String> excludeAdministrativEnhetExact;

  /** Filter by title. This is a free text search. */
  protected List<String> tittel;

  /** Filter by sender/recipient name. This is a free text search. */
  protected List<String> korrespondansepartNavn;

  /** Filter by legal basis for exemption. This is a free text search. */
  protected List<String> skjermingshjemmel;

  /** Filter by the published date of the document. */
  @IsoDateTime(allowRelative = true, format = IsoDateTime.Format.ISO_DATE_OR_DATE_TIME)
  protected String publisertDatoFrom;

  /** Filter by the published date of the document. */
  @IsoDateTime(allowRelative = true, format = IsoDateTime.Format.ISO_DATE_OR_DATE_TIME)
  protected String publisertDatoTo;

  /** Filter by the updated date of the document. */
  @IsoDateTime(allowRelative = true, format = IsoDateTime.Format.ISO_DATE_OR_DATE_TIME)
  protected String oppdatertDatoFrom;

  /** Filter by the updated date of the document. */
  @IsoDateTime(allowRelative = true, format = IsoDateTime.Format.ISO_DATE_OR_DATE_TIME)
  protected String oppdatertDatoTo;

  /** Filter by journal date. */
  @IsoDateTime(allowRelative = true, format = IsoDateTime.Format.ISO_DATE_OR_DATE_TIME)
  protected String journaldatoFrom;

  /** Filter by journal date. */
  @IsoDateTime(allowRelative = true, format = IsoDateTime.Format.ISO_DATE_OR_DATE_TIME)
  protected String journaldatoTo;

  /** Filter by document date. */
  @IsoDateTime(allowRelative = true, format = IsoDateTime.Format.ISO_DATE_OR_DATE_TIME)
  protected String dokumentetsDatoFrom;

  /** Filter by document date. */
  @IsoDateTime(allowRelative = true, format = IsoDateTime.Format.ISO_DATE_OR_DATE_TIME)
  protected String dokumentetsDatoTo;

  /** Filter by the date of a meeting. */
  @IsoDateTime(allowRelative = true, format = IsoDateTime.Format.ISO_DATE_OR_DATE_TIME)
  protected String moetedatoFrom;

  /** Filter by the date of a meeting. */
  @IsoDateTime(allowRelative = true, format = IsoDateTime.Format.ISO_DATE_OR_DATE_TIME)
  protected String moetedatoTo;

  /**
   * Filter by the legacy "standardDato". This is the default date for each entity type. For
   * instance, for Moetemappe this would be "moetedato", for Journalpost this would be
   * "journaldato".
   */
  @IsoDateTime(allowRelative = true, format = IsoDateTime.Format.ISO_DATE_OR_DATE_TIME)
  protected String standardDatoFrom;

  /**
   * Filter by the legacy "standardDato". This is the default date for each entity type. For
   * instance, for Moetemappe this would be "moetedato", for Journalpost this would be
   * "journaldato".
   */
  @IsoDateTime(allowRelative = true, format = IsoDateTime.Format.ISO_DATE_OR_DATE_TIME)
  protected String standardDatoTo;

  /** Filter by saksaar */
  protected List<String> saksaar;

  /** Filter by sakssekvensnummer */
  protected List<String> sakssekvensnummer;

  /** Filter by saksnummer */
  protected List<String> saksnummer;

  /** Filter by journalpostnummer */
  protected List<String> journalpostnummer;

  /** Filter by journalsekvensnummer */
  protected List<String> journalsekvensnummer;

  /** Filter by moetesaksaar */
  protected List<String> moetesaksaar;

  /** Filter by moetesakssekvensnummer */
  protected List<String> moetesakssekvensnummer;

  /** Filter by journalposttype */
  @ValidEnum(enumClass = JournalposttypeEnum.class)
  protected List<String> journalposttype;

  /** Filter by the entity type. */
  @ValidEnum(enumClass = EntityEnum.class)
  protected List<String> entity;

  /**
   * A list of resource IDs to be returned. Maximum 100 values. If this parameter is used, the other
   * parameters will be ignored.
   */
  @Size(max = 100)
  protected List<String> ids;

  /**
   * A list of external IDs to be returned. Maximum 100 values. If this parameter is used, the other
   * parameters will be ignored.
   */
  @Size(max = 100)
  protected List<String> externalIds;

  /** The Journalenhet to filter the result set by. */
  protected String journalenhet;

  /** Match documents with (or without) fulltext. */
  protected Boolean fulltext;

  public enum SortOrderEnum {
    @SerializedName("asc")
    ASC("asc"),
    @SerializedName("desc")
    DESC("desc");

    private final String value;

    SortOrderEnum(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }

    public String toJson() {
      return value;
    }

    public static SortOrderEnum fromValue(String value) {
      value = value.trim().toLowerCase();
      for (SortOrderEnum val : SortOrderEnum.values()) {
        if (val.value.toLowerCase().equals(value)) {
          return val;
        }
      }
      throw new IllegalArgumentException("Unknown value: " + value);
    }
  }

  public enum SortByEnum {
    @SerializedName("administrativEnhetNavn")
    ADMINISTRATIVENHETNAVN("administrativEnhetNavn"),
    @SerializedName("dokumentetsDato")
    DOKUMENTETSDATO("dokumentetsDato"),
    @SerializedName("entity")
    ENTITY("entity"),
    @SerializedName("fulltekst")
    FULLTEKST("fulltekst"),
    @SerializedName("id")
    ID("id"),
    @SerializedName("journaldato")
    JOURNALDATO("journaldato"),
    @SerializedName("journalpostnummer")
    JOURNALPOSTNUMMER("journalpostnummer"),
    @SerializedName("journalposttype")
    JOURNALPOSTTYPE("journalposttype"),
    @SerializedName("korrespondansepartNavn")
    KORRESPONDANSEPARTNAVN("korrespondansepartNavn"),
    @SerializedName("moetedato")
    MOETEDATO("moetedato"),
    @SerializedName("oppdatertDato")
    OPPDATERTDATO("oppdatertDato"),
    @SerializedName("publisertDato")
    PUBLISERTDATO("publisertDato"),
    @SerializedName("standardDato")
    STANDARDDATO("standardDato"),
    @SerializedName("sakssekvensnummer")
    SAKSSEKVENSNUMMER("sakssekvensnummer"),
    @SerializedName("score")
    SCORE("score"),
    @SerializedName("tittel")
    TITTEL("tittel");

    private final String value;

    SortByEnum(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }

    public String toJson() {
      return value;
    }

    public static SortByEnum fromValue(String value) {
      value = value.trim().toLowerCase();
      for (SortByEnum val : SortByEnum.values()) {
        if (val.value.toLowerCase().equals(value)) {
          return val;
        }
      }
      throw new IllegalArgumentException("Unknown value: " + value);
    }
  }

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

  public enum EntityEnum {
    @SerializedName("Journalpost")
    JOURNALPOST("Journalpost"),
    @SerializedName("Moetemappe")
    MOETEMAPPE("Moetemappe"),
    @SerializedName("Moetesak")
    MOETESAK("Moetesak"),
    @SerializedName("Saksmappe")
    SAKSMAPPE("Saksmappe");

    private final String value;

    EntityEnum(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }

    public String toJson() {
      return value;
    }

    public static EntityEnum fromValue(String value) {
      value = value.trim().toLowerCase();
      for (EntityEnum val : EntityEnum.values()) {
        if (val.value.toLowerCase().equals(value)) {
          return val;
        }
      }
      throw new IllegalArgumentException("Unknown value: " + value);
    }
  }
}
