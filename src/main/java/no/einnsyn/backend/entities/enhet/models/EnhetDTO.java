// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.enhet.models;

import com.google.gson.annotations.SerializedName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;
import no.einnsyn.backend.validation.validenum.ValidEnum;

/** eInnsyn Enhet */
@Getter
@Setter
public class EnhetDTO extends BaseDTO {
  protected final String entity = "Enhet";

  /** A URL-friendly unique slug for the resource. */
  @Pattern(regexp = "^[a-z0-9\\-]+$")
  protected String slug;

  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String navn;

  @NoSSN
  @Size(max = 500)
  protected String navnNynorsk;

  @NoSSN
  @Size(max = 500)
  protected String navnEngelsk;

  @NoSSN
  @Size(max = 500)
  protected String navnSami;

  @Pattern(regexp = "^[0-9]{9}$")
  @NotBlank(groups = {Insert.class})
  protected String orgnummer;

  @NoSSN
  @Size(max = 500)
  protected String enhetskode;

  @NoSSN
  @Size(max = 500)
  protected String kontaktpunktAdresse;

  @Email
  @NotBlank(groups = {Insert.class})
  protected String kontaktpunktEpost;

  @NoSSN
  @Size(max = 500)
  protected String kontaktpunktTelefon;

  @Email
  @NotBlank(groups = {Insert.class})
  protected String innsynskravEpost;

  @ValidEnum(enumClass = EnhetstypeEnum.class)
  @NotNull(groups = {Insert.class})
  protected String enhetstype;

  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
  protected String avsluttetDato;

  protected Boolean skjult;

  protected Boolean eFormidling;

  protected Boolean teknisk;

  protected Boolean skalKonvertereId;

  protected Boolean skalMottaKvittering;

  protected Boolean visToppnode;

  protected Integer orderXmlVersjon;

  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected List<ExpandableField<EnhetDTO>> underenhet;

  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected ExpandableField<EnhetDTO> handteresAv;

  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected ExpandableField<EnhetDTO> parent;

  public enum EnhetstypeEnum {
    @SerializedName("ADMINISTRATIVENHET")
    ADMINISTRATIVENHET("ADMINISTRATIVENHET"),
    @SerializedName("AVDELING")
    AVDELING("AVDELING"),
    @SerializedName("BYDEL")
    BYDEL("BYDEL"),
    @SerializedName("DUMMYENHET")
    DUMMYENHET("DUMMYENHET"),
    @SerializedName("FYLKE")
    FYLKE("FYLKE"),
    @SerializedName("KOMMUNE")
    KOMMUNE("KOMMUNE"),
    @SerializedName("ORGAN")
    ORGAN("ORGAN"),
    @SerializedName("SEKSJON")
    SEKSJON("SEKSJON"),
    @SerializedName("UTVALG")
    UTVALG("UTVALG"),
    @SerializedName("VIRKSOMHET")
    VIRKSOMHET("VIRKSOMHET");

    private final String value;

    EnhetstypeEnum(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }

    public String toJson() {
      return value;
    }

    public static EnhetstypeEnum fromValue(String value) {
      value = value.trim().toLowerCase();
      for (EnhetstypeEnum val : EnhetstypeEnum.values()) {
        if (val.value.toLowerCase().equals(value)) {
          return val;
        }
      }
      throw new IllegalArgumentException("Unknown value: " + value);
    }
  }
}
