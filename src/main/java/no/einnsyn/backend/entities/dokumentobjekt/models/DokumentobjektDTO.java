// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.dokumentobjekt.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.entities.dokumentbeskrivelse.DokumentbeskrivelseService;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;
import org.hibernate.validator.constraints.URL;

/**
 * Represents an electronic document or file. It contains information needed to locate and render
 * the document.
 */
@Getter
@Setter
public class DokumentobjektDTO extends ArkivBaseDTO {
  protected final String entity = "Dokumentobjekt";

  /** A reference (URL) to the document file. */
  @URL
  @NotBlank(groups = {Insert.class})
  protected String referanseDokumentfil;

  /** The file format of the document (e.g., 'PDF/A'). */
  @NoSSN
  @Size(max = 500)
  protected String format;

  /** The checksum of the document file, for integrity verification. */
  @NoSSN
  @Size(max = 500)
  protected String sjekksum;

  /** The algorithm used to calculate the checksum (e.g., 'SHA-256'). */
  @NoSSN
  @Size(max = 500)
  protected String sjekksumAlgoritme;

  /** The document description this object belongs to. */
  @ExpandableObject(
      service = DokumentbeskrivelseService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected ExpandableField<DokumentbeskrivelseDTO> dokumentbeskrivelse;
}
