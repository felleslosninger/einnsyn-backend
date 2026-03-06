// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.dokumentobjekt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.DownloadFileResponse;
import no.einnsyn.backend.common.responses.models.DownloadRedirectResponse;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.dokumentobjekt.models.DokumentobjektDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.validationgroups.Update;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DokumentobjektController {
  private final DokumentobjektService service;

  public DokumentobjektController(DokumentobjektService service) {
    this.service = service;
  }

  /** List all objects. */
  @GetMapping("/dokumentobjekt")
  public ResponseEntity<PaginatedList<DokumentobjektDTO>> list(@Valid ListParameters query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Delete an object. */
  @DeleteMapping("/dokumentobjekt/{id}")
  public ResponseEntity<DokumentobjektDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = DokumentobjektService.class, mustExist = true)
          ExpandableField<DokumentobjektDTO> id)
      throws EInnsynException {
    var responseBody = service.delete(id.getId());
    return ResponseEntity.ok().body(responseBody);
  }

  /** Get an object. */
  @GetMapping("/dokumentobjekt/{id}")
  public ResponseEntity<DokumentobjektDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = DokumentobjektService.class, mustExist = true)
          ExpandableField<DokumentobjektDTO> id,
      @Valid GetParameters query)
      throws EInnsynException {
    var responseBody = service.get(id.getId(), query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Update an object. */
  @PatchMapping("/dokumentobjekt/{id}")
  public ResponseEntity<DokumentobjektDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = DokumentobjektService.class, mustExist = true)
          ExpandableField<DokumentobjektDTO> id,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = DokumentobjektService.class)
          @NotNull
          DokumentobjektDTO body)
      throws EInnsynException {
    var responseBody = service.update(id.getId(), body);
    return ResponseEntity.ok().body(responseBody);
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
      var inputStreamResource = typedResponseBody.getBody();
      return response.body(inputStreamResource);
    }
    if (responseBody instanceof DownloadRedirectResponse typedResponseBody) {
      var response = ResponseEntity.status(302);
      if (typedResponseBody.getLocation() != null) {
        var headerValue = String.valueOf(typedResponseBody.getLocation());
        response.header("Location", headerValue);
      }
      return response.body(typedResponseBody);
    }
    throw new IllegalStateException(
        "Unsupported response type: "
            + (responseBody == null ? "null" : responseBody.getClass().getName()));
  }
}
