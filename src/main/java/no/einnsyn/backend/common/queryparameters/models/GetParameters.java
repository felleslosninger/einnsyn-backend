// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.common.queryparameters.models;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetParameters extends QueryParameters {
  /** Specifies which fields in the response should be expanded. */
  protected List<String> expand;
}
