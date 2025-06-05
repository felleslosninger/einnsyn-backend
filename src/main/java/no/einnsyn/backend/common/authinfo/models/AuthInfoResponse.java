// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.common.authinfo.models;

import com.google.gson.annotations.SerializedName;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;
import no.einnsyn.backend.validation.validenum.ValidEnum;

@Getter
@Setter
public class AuthInfoResponse {
  protected final String entity = "AuthInfo";

  @ValidEnum(enumClass = AuthTypeEnum.class)
  @Null(groups = {Insert.class, Update.class})
  protected String authType;

  @ValidEnum(enumClass = TypeEnum.class)
  @Null(groups = {Insert.class, Update.class})
  protected String type;

  @Null(groups = {Insert.class, Update.class})
  protected String id;

  @NoSSN
  @Size(max = 500)
  @Null(groups = {Insert.class, Update.class})
  protected String orgnummer;

  @NoSSN
  @Size(max = 500)
  @Null(groups = {Insert.class, Update.class})
  protected String email;

  public enum AuthTypeEnum {
    @SerializedName("Ansattporten")
    ANSATTPORTEN("Ansattporten"),
    @SerializedName("ApiKey")
    APIKEY("ApiKey"),
    @SerializedName("Bruker")
    BRUKER("Bruker");

    private final String value;

    AuthTypeEnum(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }

    public String toJson() {
      return value;
    }

    public static AuthTypeEnum fromValue(String value) {
      value = value.trim().toLowerCase();
      for (AuthTypeEnum val : AuthTypeEnum.values()) {
        if (val.value.toLowerCase().equals(value)) {
          return val;
        }
      }
      throw new IllegalArgumentException("Unknown value: " + value);
    }
  }

  public enum TypeEnum {
    @SerializedName("Bruker")
    BRUKER("Bruker"),
    @SerializedName("Enhet")
    ENHET("Enhet");

    private final String value;

    TypeEnum(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }

    public String toJson() {
      return value;
    }

    public static TypeEnum fromValue(String value) {
      value = value.trim().toLowerCase();
      for (TypeEnum val : TypeEnum.values()) {
        if (val.value.toLowerCase().equals(value)) {
          return val;
        }
      }
      throw new IllegalArgumentException("Unknown value: " + value);
    }
  }
}
