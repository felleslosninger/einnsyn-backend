// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.moetesak.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.enhet.EnhetService;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.entities.moetemappe.MoetemappeService;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetesaksbeskrivelse.MoetesaksbeskrivelseService;
import no.einnsyn.apiv3.entities.moetesaksbeskrivelse.models.MoetesaksbeskrivelseDTO;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringDTO;
import no.einnsyn.apiv3.entities.utredning.UtredningService;
import no.einnsyn.apiv3.entities.utredning.models.UtredningDTO;
import no.einnsyn.apiv3.entities.vedtak.VedtakService;
import no.einnsyn.apiv3.entities.vedtak.models.VedtakDTO;
import no.einnsyn.apiv3.validation.expandableobject.ExpandableObject;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;
import no.einnsyn.apiv3.validation.validenum.ValidEnum;

@Getter
@Setter
public class MoetesakDTO extends RegistreringDTO {

  @Size(max = 500)
  final String entity = "Moetesak";

  @Size(max = 500)
  @ValidEnum(enumClass = MoetesakstypeEnum.class)
  @NotBlank(groups = {Insert.class})
  String moetesakstype;

  @Size(max = 500)
  @NoSSN
  String legacyMoetesakstype;

  Integer moetesaksaar;

  @NotNull(groups = {Insert.class})
  Integer moetesakssekvensnummer;

  @Size(max = 500)
  @NoSSN
  String utvalg;

  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<EnhetDTO> utvalgObjekt;

  @Size(max = 500)
  @NoSSN
  String videoLink;

  @ExpandableObject(
      service = UtredningService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<UtredningDTO> utredning;

  @ExpandableObject(
      service = MoetesaksbeskrivelseService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<MoetesaksbeskrivelseDTO> innstilling;

  @ExpandableObject(
      service = VedtakService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<VedtakDTO> vedtak;

  @ExpandableObject(
      service = MoetemappeService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<MoetemappeDTO> moetemappe;

  @Size(max = 500)
  @NoSSN
  String legacyReferanseTilMoetesak;
}
