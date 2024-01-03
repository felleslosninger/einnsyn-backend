package no.einnsyn.apiv3.entities.bruker.models;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.features.validation.password.Password;

@Getter
@Setter
public class SetPasswordWithOldPasswordRequestBody {
  private String oldPassword;

  @Password @NotNull private String newPassword;
}
