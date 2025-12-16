// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.innsynskrav.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.entities.innsynskravbestilling.InnsynskravBestillingService;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.backend.entities.journalpost.JournalpostService;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

/** Represents a request for access to a specific registry entry (Journalpost). */
@Getter
@Setter
public class InnsynskravDTO extends BaseDTO {
  protected final String entity = "Innsynskrav";

  /** The order containing this access request. */
  @ExpandableObject(
      service = InnsynskravBestillingService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected ExpandableField<InnsynskravBestillingDTO> innsynskravBestilling;

  /** The registry entry being requested. */
  @ExpandableObject(
      service = JournalpostService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @NotNull(groups = {Insert.class})
  protected ExpandableField<JournalpostDTO> journalpost;

  /** The public authority responsible for handling the request. */
  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected ExpandableField<EnhetDTO> enhet;

  /** The email address of the requester. */
  @Email
  @Null(groups = {Insert.class, Update.class})
  protected String email;

  /** The timestamp when the request was sent to the public authority. */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  protected String sent;
}
