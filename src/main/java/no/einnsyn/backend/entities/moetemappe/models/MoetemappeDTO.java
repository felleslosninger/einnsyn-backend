// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.moetemappe.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.entities.mappe.models.MappeDTO;
import no.einnsyn.backend.entities.moetedokument.MoetedokumentService;
import no.einnsyn.backend.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.backend.entities.moetemappe.MoetemappeService;
import no.einnsyn.backend.entities.moetesak.MoetesakService;
import no.einnsyn.backend.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

@Getter
@Setter
public class MoetemappeDTO extends MappeDTO {

  @Size(max = 500)
  final String entity = "Moetemappe";

  @Size(max = 500)
  @NoSSN
  @NotBlank(groups = {Insert.class})
  String moetenummer;

  @Size(max = 500)
  @NoSSN
  @NotBlank(groups = {Insert.class})
  String utvalg;

  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<EnhetDTO> utvalgObjekt;

  @Size(max = 500)
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  @NotBlank(groups = {Insert.class})
  String moetedato;

  @Size(max = 500)
  @NoSSN
  String moetested;

  @Size(max = 5000)
  @NoSSN
  String videoLink;

  @ExpandableObject(
      service = MoetemappeService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<MoetemappeDTO> referanseForrigeMoete;

  @ExpandableObject(
      service = MoetemappeService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<MoetemappeDTO> referanseNesteMoete;

  @ExpandableObject(
      service = MoetedokumentService.class,
      groups = {Insert.class, Update.class})
  @Valid
  List<ExpandableField<MoetedokumentDTO>> moetedokument;

  @ExpandableObject(
      service = MoetesakService.class,
      groups = {Insert.class, Update.class})
  @Valid
  List<ExpandableField<MoetesakDTO>> moetesak;
}
