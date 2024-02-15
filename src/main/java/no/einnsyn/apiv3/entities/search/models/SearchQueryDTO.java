// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.search.models;

import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validenum.ValidEnum;

@Getter
@Setter
public class SearchQueryDTO {

  List<String> expand;

  Integer limit = 25;

  @Size(max = 500)
  @ValidEnum(enumClass = SortOrderEnum.class)
  String sortOrder;

  @Size(max = 500)
  @ValidEnum(enumClass = SortByEnum.class)
  String sortBy;

  @Size(max = 500)
  @NoSSN
  String query;

  @Size(max = 500)
  @ValidEnum(enumClass = ResourceEnum.class)
  String resource;

  List<String> administrativEnhetId;

  List<String> administrativEnhetTransitiveId;

  List<String> administrativEnhetIri;

  List<String> administrativEnhetTransitiveIri;

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

  public enum SortByEnum {
    CREATED("created"),
    UPDATED("updated"),
    OFFENTLIGTITTEL("offentligTittel"),
    OFFENTLIGTITTELSENSITIV("offentligTittelSensitiv");

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
      for (SortByEnum val : values()) {
        if (val.value.equals(value)) {
          return val;
        }
      }
      throw new IllegalArgumentException("No enum constant for value: " + value);
    }
  }

  public enum ResourceEnum {
    JOURNALPOST("Journalpost"),
    MOETEMAPPE("Moetemappe"),
    MOETESAK("Moetesak"),
    SAKSMAPPE("Saksmappe");

    private final String value;

    ResourceEnum(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }

    public String toJson() {
      return value;
    }

    public static ResourceEnum fromValue(String value) {
      for (ResourceEnum val : values()) {
        if (val.value.equals(value)) {
          return val;
        }
      }
      throw new IllegalArgumentException("No enum constant for value: " + value);
    }
  }
}
