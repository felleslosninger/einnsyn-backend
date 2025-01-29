// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.common.queryparameters.models;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetParameters {
  /** Specifies which fields in the response should be expanded. */
  List<String> expand;
}
