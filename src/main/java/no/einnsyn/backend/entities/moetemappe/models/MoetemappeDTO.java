// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.moetemappe.models;

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

/** Moetemappe */
@Getter
@Setter
public class MoetemappeDTO extends MappeDTO {
  protected final String entity = "Moetemappe";

  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String moetenummer;

  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String utvalg;

  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @Null(groups = {Insert.class, Update.class})
  protected ExpandableField<EnhetDTO> utvalgObjekt;

  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  @NotNull(groups = {Insert.class})
  protected String moetedato;

  @NoSSN
  @Size(max = 500)
  protected String moetested;

  @Size(max = 5000)
  @NoSSN
  protected String videoLink;

  @ExpandableObject(
      service = MoetemappeService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected ExpandableField<MoetemappeDTO> referanseForrigeMoete;

  @ExpandableObject(
      service = MoetemappeService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected ExpandableField<MoetemappeDTO> referanseNesteMoete;

  @ExpandableObject(
      service = MoetedokumentService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected List<ExpandableField<MoetedokumentDTO>> moetedokument;

  @ExpandableObject(
      service = MoetesakService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected List<ExpandableField<MoetesakDTO>> moetesak;
}
