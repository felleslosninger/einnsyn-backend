// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.journalpost.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.entities.registrering.models.RegistreringDTO;
import no.einnsyn.backend.entities.saksmappe.SaksmappeService;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.entities.skjerming.SkjermingService;
import no.einnsyn.backend.entities.skjerming.models.SkjermingDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;
import no.einnsyn.backend.validation.validenum.ValidEnum;

@Getter
@Setter
public class JournalpostDTO extends RegistreringDTO {

  @Size(max = 500)
  final String entity = "Journalpost";

  @NotNull(groups = {Insert.class})
  Integer journalaar;

  @NotNull(groups = {Insert.class})
  Integer journalsekvensnummer;

  @NotNull(groups = {Insert.class})
  Integer journalpostnummer;

  @Size(max = 500)
  @ValidEnum(enumClass = JournalposttypeEnum.class)
  @NotBlank(groups = {Insert.class})
  String journalposttype;

  @Size(max = 500)
  @NoSSN
  String legacyJournalposttype;

  @Size(max = 500)
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
  @NotBlank(groups = {Insert.class})
  String journaldato;

  @Size(max = 500)
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
  String dokumentetsDato;

  @Size(max = 500)
  @NoSSN
  @Null(groups = {Insert.class, Update.class})
  String administrativEnhet;

  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<EnhetDTO> administrativEnhetObjekt;

  @ExpandableObject(
      service = SaksmappeService.class,
      groups = {Insert.class, Update.class})
  @Null(groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<SaksmappeDTO> saksmappe;

  @ExpandableObject(
      service = SkjermingService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<SkjermingDTO> skjerming;

  List<String> legacyFoelgsakenReferanse;
}
