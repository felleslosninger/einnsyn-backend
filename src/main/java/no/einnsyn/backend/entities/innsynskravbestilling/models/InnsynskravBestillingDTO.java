// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.innsynskravbestilling.models;

import com.google.gson.annotations.SerializedName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
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

/** Innsynskrav */
@Getter
@Setter
public class InnsynskravBestillingDTO extends BaseDTO {
  protected final String entity = "InnsynskravBestilling";

  @Email
  @NotBlank(groups = {Insert.class})
  protected String email;

  @ExpandableObject(
      service = InnsynskravService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @NotNull(groups = {Insert.class})
  protected List<ExpandableField<InnsynskravDTO>> innsynskrav;

  @Null(groups = {Insert.class, Update.class})
  protected Boolean verified;

  @ExpandableObject(
      service = BrukerService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @Null(groups = {Insert.class, Update.class})
  protected ExpandableField<BrukerDTO> bruker;

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
