// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.votering.models;

import com.google.gson.annotations.SerializedName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.entities.identifikator.IdentifikatorService;
import no.einnsyn.backend.entities.identifikator.models.IdentifikatorDTO;
import no.einnsyn.backend.entities.moetedeltaker.MoetedeltakerService;
import no.einnsyn.backend.entities.moetedeltaker.models.MoetedeltakerDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;
import no.einnsyn.backend.validation.validenum.ValidEnum;

/** Represents a vote cast by a participant in a meeting. */
@Getter
@Setter
public class VoteringDTO extends ArkivBaseDTO {
  protected final String entity = "Votering";

  /** The meeting participant who cast the vote. */
  @ExpandableObject(
      service = MoetedeltakerService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @NotNull(groups = {Insert.class})
  protected ExpandableField<MoetedeltakerDTO> moetedeltaker;

  /** The vote cast ('Ja' for yes, 'Nei' for no, 'Blankt' for blank). */
  @ValidEnum(enumClass = StemmeEnum.class)
  @NotNull(groups = {Insert.class})
  protected String stemme;

  /** The person or party the participant is representing, if applicable. */
  @ExpandableObject(
      service = IdentifikatorService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected ExpandableField<IdentifikatorDTO> representerer;

  public enum StemmeEnum {
    @SerializedName("Ja")
    JA("Ja"),
    @SerializedName("Nei")
    NEI("Nei"),
    @SerializedName("Blankt")
    BLANKT("Blankt");

    private final String value;

    StemmeEnum(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }

    public String toJson() {
      return value;
    }

    public static StemmeEnum fromValue(String value) {
      value = value.trim().toLowerCase();
      for (StemmeEnum val : StemmeEnum.values()) {
        if (val.value.toLowerCase().equals(value)) {
          return val;
        }
      }
      throw new IllegalArgumentException("Unknown value: " + value);
    }
  }
}
