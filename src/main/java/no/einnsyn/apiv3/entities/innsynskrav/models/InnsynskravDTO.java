// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.innsynskrav.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import no.einnsyn.apiv3.entities.bruker.BrukerService;
import no.einnsyn.apiv3.entities.bruker.models.BrukerDTO;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelService;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelDTO;
import no.einnsyn.apiv3.validation.expandableobject.ExpandableObject;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;
import no.einnsyn.apiv3.validation.validenum.ValidEnum;

@Getter
@Setter
public class InnsynskravDTO extends BaseDTO {

  @Size(max = 500)
  final String entity = "Innsynskrav";

  @Size(max = 500)
  @Email
  @NotBlank(groups = {Insert.class})
  String email;

  @ExpandableObject(
      service = InnsynskravDelService.class,
      groups = {Insert.class, Update.class})
  @NotNull(groups = {Insert.class})
  @Valid
  List<ExpandableField<InnsynskravDelDTO>> innsynskravDel;

  @Null(groups = {Insert.class, Update.class})
  Boolean verified;

  @ExpandableObject(
      service = BrukerService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<BrukerDTO> bruker;

  @Size(max = 500)
  @ValidEnum(enumClass = LanguageEnum.class)
  String language;
}
