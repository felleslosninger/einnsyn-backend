// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.common.search.models;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.queryparameters.models.FilterParameters;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validenum.ValidEnum;

/** Search parameters */
@Getter
@Setter
public class SearchParameters extends FilterParameters {
  /** Specifies which fields in the response should be expanded. */
  List<String> expand;

  /**
   * A limit on the number of objects to be returned. Limit can range between 1 and 100, and the
   * default is 10.
   */
  Integer limit = 25;

  /** The sort order of the result set. The default is ascending. */
  @ValidEnum(enumClass = SortOrderEnum.class)
  String sortOrder = "asc";

  /**
   * A cursor for use in pagination. This is a list of size two, the sortBy property and the unique
   * id.
   */
  List<String> startingAfter;

  /**
   * A cursor for use in pagination. This is a list of size two, the sortBy property and the unique
   * id.
   */
  List<String> endingBefore;

  /** The field to sort results by. The default is "score". */
  @ValidEnum(enumClass = SortByEnum.class)
  @NotNull(groups = {Insert.class})
  String sortBy = "score";

  public enum SortOrderEnum {
    ASC("asc"),
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
    SCORE("score"),
    ID("id"),
    ENTITY("entity"),
    PUBLISERTDATO("publisertDato"),
    OPPDATERTDATO("oppdatertDato"),
    MOETEDATO("moetedato"),
    FULLTEKST("fulltekst"),
    TYPE("type");

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
}
