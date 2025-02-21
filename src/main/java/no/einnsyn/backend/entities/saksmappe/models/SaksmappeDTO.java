// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.saksmappe.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.entities.journalpost.JournalpostService;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.mappe.models.MappeDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

/** Saksmappe */
@Getter
@Setter
public class SaksmappeDTO extends MappeDTO {
  protected final String entity = "Saksmappe";

  @Min(1700)
  @NotNull(groups = {Insert.class})
  protected Integer saksaar;

  @Min(0)
  @NotNull(groups = {Insert.class})
  protected Integer sakssekvensnummer;

  @NoSSN
  @Size(max = 500)
  protected String saksnummer;

  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
  protected String saksdato;

  @ExpandableObject(
      service = JournalpostService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected List<ExpandableField<JournalpostDTO>> journalpost;

  /** A code for the administrative Enhet associated with this Saksmappe. */
  @NoSSN
  @Size(max = 500)
  protected String administrativEnhet;

  /**
   * The administrative Enhet associated with this Saksmappe. This is derived from the code given in
   * `administrativEnhet`. If no `administrativEnhet` is given, or the code is not found, the
   * `journalenhet` of the authenticated user will be used.
   */
  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @Null(groups = {Insert.class, Update.class})
  protected ExpandableField<EnhetDTO> administrativEnhetObjekt;
}
