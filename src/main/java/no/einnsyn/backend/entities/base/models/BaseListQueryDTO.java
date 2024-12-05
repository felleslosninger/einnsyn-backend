// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.base.models;

import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validenum.ValidEnum;

@Getter
@Setter
public class BaseListQueryDTO {

  List<String> expand;

  Integer limit = 25;

  @Size(max = 500)
  @ValidEnum(enumClass = SortOrderEnum.class)
  String sortOrder;

  @Size(max = 500)
  @NoSSN
  String startingAfter;

  @Size(max = 500)
  @NoSSN
  String endingBefore;

  List<String> ids;

  List<String> externalIds;

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
      for (SortOrderEnum val : values()) {
        if (val.value.equals(value)) {
          return val;
        }
      }
      throw new IllegalArgumentException("No enum constant for value: " + value);
    }
  }
}
