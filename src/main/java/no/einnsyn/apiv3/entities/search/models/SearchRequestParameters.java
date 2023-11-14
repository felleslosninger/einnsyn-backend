package no.einnsyn.apiv3.entities.search.models;

import java.util.List;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.requests.GetListRequestParameters;

@Getter
@Setter
public class SearchRequestParameters extends GetListRequestParameters {
  private String query;
  private List<Field> field;
  private List<String> administrativEnhetId;
  private List<String> administrativEnhetIri;
  private List<String> administrativEnhetIdTransitive;
  private List<String> administrativEnhetIriTransitive;
  private String sortBy = "publisertDato"; // TODO: Enum

  @Min(1)
  @Max(50)
  private Integer limit = 25;

  @Getter
  @Setter
  private class Field {
    private String fieldName;
    private String comparison;
    private String value;
  }

}
