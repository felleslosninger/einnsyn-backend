// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.entities.korrespondansepart.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.entities.journalpost.JournalpostService;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.moetedokument.MoetedokumentService;
import no.einnsyn.backend.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.backend.entities.moetesak.MoetesakService;
import no.einnsyn.backend.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

/** Korrespondansepart */
@Getter
@Setter
public class KorrespondansepartDTO extends ArkivBaseDTO {
  final String entity = "Korrespondansepart";

  /** The name of the Korrespondansepart, with sensitive parts redacted. */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  String korrespondansepartNavn;

  /** The name of the Korrespondansepart, with all parts included. */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  String korrespondansepartNavnSensitiv;

  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  String korrespondanseparttype;

  @NoSSN
  @Size(max = 500)
  String saksbehandler;

  @NoSSN
  @Size(max = 500)
  String epostadresse;

  @NoSSN
  @Size(max = 500)
  String postnummer;

  Boolean erBehandlingsansvarlig;

  /** The code for the administrative Enhet associated with this Korrespondansepart. */
  @NoSSN
  @Size(max = 500)
  String administrativEnhet;

  /** The Journalpost this Korrespondansepart is associated with, if any. */
  @ExpandableObject(
      service = JournalpostService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @Null(groups = {Insert.class, Update.class})
  ExpandableField<JournalpostDTO> journalpost;

  /** The Moetedokument this Korrespondansepart is associated with, if any. */
  @ExpandableObject(
      service = MoetedokumentService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @Null(groups = {Insert.class, Update.class})
  ExpandableField<MoetedokumentDTO> moetedokument;

  /** The Moetesak this Korrespondansepart is associated with, if any. */
  @ExpandableObject(
      service = MoetesakService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @Null(groups = {Insert.class, Update.class})
  ExpandableField<MoetesakDTO> moetesak;
}
