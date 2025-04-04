// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.common.search.models;

import com.google.gson.annotations.SerializedName;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.queryparameters.models.FilterParameters;
import no.einnsyn.backend.validation.validenum.ValidEnum;

/** Search parameters */
@Getter
@Setter
public class SearchParameters extends FilterParameters {
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
  protected List<String> startingAfter;

  /**
   * A cursor for use in pagination. This is a list of size two, the value of the sortBy property
   * and the unique id.
   */
  protected List<String> endingBefore;

  /** The field to sort results by. The default is "score". */
  @ValidEnum(enumClass = SortByEnum.class)
  protected String sortBy = "score";

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
    @SerializedName("score")
    SCORE("score"),
    @SerializedName("id")
    ID("id"),
    @SerializedName("entity")
    ENTITY("entity"),
    @SerializedName("publisertDato")
    PUBLISERTDATO("publisertDato"),
    @SerializedName("oppdatertDato")
    OPPDATERTDATO("oppdatertDato"),
    @SerializedName("moetedato")
    MOETEDATO("moetedato"),
    @SerializedName("fulltekst")
    FULLTEKST("fulltekst"),
    @SerializedName("type")
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
