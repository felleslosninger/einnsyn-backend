package no.einnsyn.apiv3.features.search.models;

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
  private List<String> journalenhet;
  private List<String> journalenhetIri;
  private List<String> journalenhetTransitive;
  private List<String> journalenhetIriTransitive;
  private String sortBy = "publisertDato";
  private String sortOrder = "Desc";

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
