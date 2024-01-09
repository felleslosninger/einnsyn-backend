// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.listqueryparameters.models;

import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.getqueryparameters.models.GetQueryParametersDTO;
import no.einnsyn.apiv3.features.validation.NoSSN;

@Getter
@Setter
public class ListQueryParametersDTO extends GetQueryParametersDTO {

  private List<String> expand;

  private Long limit;

  @Size(max = 500)
  private SortOrderEnum sortOrder;

  @Size(max = 500)
  @NoSSN
  private String startingAfter;

  @Size(max = 500)
  @NoSSN
  private String endingBefore;

  private List<String> ids;

  public enum SortOrderEnum {
    asc,
    desc,
  }
}
