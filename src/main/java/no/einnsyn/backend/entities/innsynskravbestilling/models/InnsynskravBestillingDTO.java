// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.innsynskravbestilling.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.bruker.BrukerService;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import no.einnsyn.backend.entities.innsynskrav.InnsynskravService;
import no.einnsyn.backend.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;
import no.einnsyn.backend.validation.validenum.ValidEnum;

@Getter
@Setter
public class InnsynskravBestillingDTO extends BaseDTO {

  @Size(max = 500)
  final String entity = "InnsynskravBestilling";

  @Size(max = 500)
  @Email
  @NotBlank(groups = {Insert.class})
  String email;

  @ExpandableObject(
      service = InnsynskravService.class,
      groups = {Insert.class, Update.class})
  @NotNull(groups = {Insert.class})
  @Valid
  List<ExpandableField<InnsynskravDTO>> innsynskrav;

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
