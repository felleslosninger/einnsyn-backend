// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.innsynskrav.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
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

@Getter
@Setter
public class InnsynskravDTO extends BaseDTO {

  @Size(max = 500)
  final String entity = "Innsynskrav";

  @ExpandableObject(
      service = InnsynskravBestillingService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<InnsynskravBestillingDTO> innsynskravBestilling;

  @ExpandableObject(
      service = JournalpostService.class,
      groups = {Insert.class, Update.class})
  @NotNull(groups = {Insert.class})
  @Valid
  ExpandableField<JournalpostDTO> journalpost;

  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Null(groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<EnhetDTO> enhet;

  @Size(max = 500)
  @Email
  @Null(groups = {Insert.class, Update.class})
  String email;

  @Size(max = 500)
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  @Null(groups = {Insert.class, Update.class})
  String sent;

  @Null(groups = {Insert.class, Update.class})
  Integer retryCount;

  @Size(max = 500)
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  @Null(groups = {Insert.class, Update.class})
  String retryTimestamp;
}
