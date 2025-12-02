// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.bruker.models;

import com.google.gson.annotations.SerializedName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.validation.password.Password;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;
import no.einnsyn.backend.validation.validenum.ValidEnum;

/** eInnsyn bruker */
@Getter
@Setter
public class BrukerDTO extends BaseDTO {
  protected final String entity = "Bruker";

  @Email
  @NotBlank(groups = {Insert.class})
  protected String email;

  @Null(groups = {Insert.class, Update.class})
  protected Boolean active;

  @Password
  @NotBlank(groups = {Insert.class})
  protected String password;

  @ValidEnum(enumClass = LanguageEnum.class)
  protected String language = "nb";

  public enum LanguageEnum {
    @SerializedName("nb")
    NB("nb"),
    @SerializedName("nn")
    NN("nn"),
    @SerializedName("en")
    EN("en"),
    @SerializedName("se")
    SE("se");

    private final String value;

    LanguageEnum(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }

    public String toJson() {
      return value;
    }

    public static LanguageEnum fromValue(String value) {
      value = value.trim().toLowerCase();
      for (LanguageEnum val : LanguageEnum.values()) {
        if (val.value.toLowerCase().equals(value)) {
          return val;
        }
      }
      throw new IllegalArgumentException("Unknown value: " + value);
    }
  }
}
