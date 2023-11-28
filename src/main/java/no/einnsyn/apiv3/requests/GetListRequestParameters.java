package no.einnsyn.apiv3.requests;

import java.util.List;
import java.util.Set;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetListRequestParameters {

  @Max(value = 50)
  @Min(value = 1)
  private Integer limit = 25;

  private String sortOrder = "desc"; // TODO: Enum Asc | Desc

  private List<String> ids;

  private String endingBefore;

  private String startingAfter;

  private Set<String> expand;

}
