// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.common.responses.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import org.springframework.core.io.InputStreamResource;

@Getter
@Setter
public class DownloadFileResponse extends DownloadResponseBase {
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String contentType;

  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String contentDisposition;

  @NotNull(groups = {Insert.class})
  protected InputStreamResource body;
}
