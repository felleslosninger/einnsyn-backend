// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.common.responses.models;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.validation.validationgroups.Insert;
import org.hibernate.validator.constraints.URL;

@Getter
@Setter
public class DownloadRedirectResponse extends DownloadResponseBase {
  @URL
  @NotBlank(groups = {Insert.class})
  protected String location;
}
