// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.journalpost.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.apiv3.entities.skjerming.models.SkjermingDTO;
import no.einnsyn.apiv3.validation.isodatetime.IsoDateTime;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;

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
  @NoSSN
  @NotNull(groups = {Insert.class})
  String journalposttype;

  @Size(max = 500)
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
  @NotNull(groups = {Insert.class})
  String journaldato;

  @Size(max = 500)
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
  String dokumentetsDato;

  @Size(max = 500)
  @NoSSN
  String administrativEnhet;

  ExpandableField<EnhetDTO> administrativEnhetObjekt;

  @Size(max = 500)
  @NoSSN
  @Null(groups = {Insert.class, Update.class})
  String sorteringstype;

  @Valid ExpandableField<SaksmappeDTO> saksmappe;

  @Valid ExpandableField<SkjermingDTO> skjerming;
}
