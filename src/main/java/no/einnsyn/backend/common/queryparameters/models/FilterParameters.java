// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.common.queryparameters.models;

import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validenum.ValidEnum;

@Getter
@Setter
public class FilterParameters {
  /**
   * A query string to filter by. Quotes can be used to search for exact matches or phrases. Words
   * can be excluded by prefixing them with a minus sign.
   */
  @NoSSN
  @Size(max = 500)
  String query;

  /** A list of enhet IDs to filter by. This will also match subenhets. */
  List<String> administrativEnhet;

  /** A list of enhet IDs to filter by. This will only match the specified enhets, not subenhets. */
  List<String> administrativEnhetExact;

  /** A list of enhet IDs to exclude from the result set. This will also exclude subenhets. */
  List<String> excludeAdministrativEnhet;

  /**
   * A list of enhet IDs to exclude from the result set. This will only exclude the specified
   * enhets, not subenhets.
   */
  List<String> excludeAdministrativEnhetExact;

  /** Filter by the published date of the document. */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  String publisertDatoBefore;

  /** Filter by the published date of the document. */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  String publisertDatoAfter;

  /** Filter by the updated date of the document. */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  String oppdatertDatoBefore;

  /** Filter by the updated date of the document. */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  String oppdatertDatoAfter;

  /** Filter by the date of a meeting. */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  String moetedatoBefore;

  /** Filter by the date of a meeting. */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  String moetedatoAfter;

  /** Filter by the entity type. */
  @ValidEnum(enumClass = EntityEnum.class)
  String entity;

  public enum EntityEnum {
    JOURNALPOST("Journalpost"),
    MOETEMAPPE("Moetemappe"),
    MOETESAK("Moetesak"),
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
