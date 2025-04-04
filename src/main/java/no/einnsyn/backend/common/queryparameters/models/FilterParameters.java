// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.common.queryparameters.models;

import com.google.gson.annotations.SerializedName;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validenum.ValidEnum;

@Getter
@Setter
public class FilterParameters extends QueryParameters {
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

  /** Filter by the published date of the document. */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  protected String publisertDatoBefore;

  /** Filter by the published date of the document. */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  protected String publisertDatoAfter;

  /** Filter by the updated date of the document. */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  protected String oppdatertDatoBefore;

  /** Filter by the updated date of the document. */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  protected String oppdatertDatoAfter;

  /** Filter by the date of a meeting. */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  protected String moetedatoBefore;

  /** Filter by the date of a meeting. */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  protected String moetedatoAfter;

  /** Filter by the entity type. */
  @ValidEnum(enumClass = EntityEnum.class)
  protected List<String> entity;

  /**
   * A list of resource IDs to be returned. If this parameter is used, the other parameters will be
   * ignored.
   */
  protected List<String> ids;

  /**
   * A list of external IDs to be returned. If this parameter is used, the other parameters will be
   * ignored.
   */
  protected List<String> externalIds;

  /** The Journalenhet to filter the result set by. */
  protected String journalenhet;

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
