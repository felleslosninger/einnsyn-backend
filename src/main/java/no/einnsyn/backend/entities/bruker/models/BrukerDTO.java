// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.bruker.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.validation.password.Password;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;
import no.einnsyn.backend.validation.validenum.ValidEnum;

@Getter
@Setter
public class BrukerDTO extends BaseDTO {

  @Size(max = 500)
  final String entity = "Bruker";

  @Size(max = 500)
  @Email
  @NotBlank(groups = {Insert.class})
  String email;

  @Size(max = 500)
  @Password
  @NotBlank(groups = {Insert.class})
  String password;

  @Null(groups = {Insert.class, Update.class})
  Boolean active;

  @Size(max = 500)
  @ValidEnum(enumClass = LanguageEnum.class)
  String language = "nb";
}
