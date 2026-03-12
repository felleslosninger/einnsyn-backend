package no.einnsyn.backend.entities.dokumentobjekt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.responses.models.DownloadFileResponse;
import no.einnsyn.backend.common.responses.models.DownloadRedirectResponse;
import no.einnsyn.backend.entities.dokumentobjekt.models.DokumentobjektDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DokumentobjektDownloadController {
  private final DokumentobjektService service;

  public DokumentobjektDownloadController(DokumentobjektService service) {
    this.service = service;
  }

  /** Download the file represented by a Dokumentobjekt. */
  @GetMapping("/dokumentobjekt/{id}/download")
  public ResponseEntity<?> download(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = DokumentobjektService.class, mustExist = true)
          ExpandableField<DokumentobjektDTO> id)
      throws EInnsynException {
    var responseBody = service.download(id.getId());

    // Direct file download
    if (responseBody instanceof DownloadFileResponse typedResponseBody) {
      var response = ResponseEntity.status(200);
      if (typedResponseBody.getContentType() != null) {
        var contentType = String.valueOf(typedResponseBody.getContentType());
        try {
          var mediaType = MediaType.parseMediaType(contentType);
          response.contentType(mediaType);
        } catch (InvalidMediaTypeException e) {
          response.contentType(MediaType.APPLICATION_OCTET_STREAM);
        }
      }
      if (typedResponseBody.getContentDisposition() != null) {
        var headerValue = String.valueOf(typedResponseBody.getContentDisposition());
        response.header("Content-Disposition", headerValue);
      }
      return response.body(typedResponseBody.getBody());
    }

    // Redirect to external URL
    if (responseBody instanceof DownloadRedirectResponse typedResponseBody) {
      var response = ResponseEntity.status(302);
      if (typedResponseBody.getLocation() != null) {
        var headerValue = String.valueOf(typedResponseBody.getLocation());
        response.header("Location", headerValue);
      }
      return response.build();
    }

    throw new IllegalStateException(
        "Unsupported response type: "
            + (responseBody == null ? "null" : responseBody.getClass().getName()));
  }
}
