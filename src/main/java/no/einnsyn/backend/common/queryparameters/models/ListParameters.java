// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.common.queryparameters.models;

import com.google.gson.annotations.SerializedName;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.validation.validenum.ValidEnum;

@Getter
@Setter
public class ListParameters extends QueryParameters {
  /** Specifies which fields in the response should be expanded. */
  protected List<String> expand;

  /**
   * A limit on the number of objects to be returned. Limit can range between 1 and 100, and the
   * default is 10.
   */
  @Min(1)
  @Max(100)
  protected Integer limit = 25;

  /** The sort order of the result set. The default is ascending. */
  @ValidEnum(enumClass = SortOrderEnum.class)
  protected String sortOrder = "desc";

  /**
   * A cursor for use in pagination. StartingAfter is a resource ID that defines your place in the
   * list.
   */
  protected String startingAfter;

  /**
   * A cursor for use in pagination. EndingBefore is a resource ID that defines your place in the
   * list.
   */
  protected String endingBefore;

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
}
