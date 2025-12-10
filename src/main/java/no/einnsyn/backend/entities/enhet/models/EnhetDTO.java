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

/**
 * Represents an organizational unit within the public sector, such as a municipality, a government
 * agency, or a department. This is a central model for identifying the public entities that own and
 * manage the information in eInnsyn.
 */
@Getter
@Setter
public class EnhetDTO extends BaseDTO {
  protected final String entity = "Enhet";

  /** A URL-friendly unique slug for the resource. */
  @Pattern(regexp = "^[a-z0-9\\-]+$")
  protected String slug;

  /** The official name of the unit in Norwegian bokmål. */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String navn;

  /** The name of the unit in Norwegian nynorsk. */
  @NoSSN
  @Size(max = 500)
  protected String navnNynorsk;

  /** The name of the unit in English. */
  @NoSSN
  @Size(max = 500)
  protected String navnEngelsk;

  /** The name of the unit in Sami. */
  @NoSSN
  @Size(max = 500)
  protected String navnSami;

  /** The 9-digit organization number from the Brønnøysund Register Centre. */
  @Pattern(regexp = "^[0-9]{9}$")
  @NotBlank(groups = {Insert.class})
  protected String orgnummer;

  /** An internal code or identifier for the unit. */
  @NoSSN
  @Size(max = 500)
  protected String enhetskode;

  /** The postal address for the unit's contact point. */
  @NoSSN
  @Size(max = 500)
  protected String kontaktpunktAdresse;

  /** The primary contact email address for the unit. */
  @Email
  @NotBlank(groups = {Insert.class})
  protected String kontaktpunktEpost;

  /** The primary contact phone number for the unit. */
  @NoSSN
  @Size(max = 500)
  protected String kontaktpunktTelefon;

  /** The dedicated email address for receiving Freedom of Information (FOI) requests. */
  @Email
  @NotBlank(groups = {Insert.class})
  protected String innsynskravEpost;

  /** The type of the organizational unit. */
  @ValidEnum(enumClass = EnhetstypeEnum.class)
  @NotNull(groups = {Insert.class})
  protected String enhetstype;

  /** The date when the unit was officially dissolved or became inactive. */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
  protected String avsluttetDato;

  /** If true, this unit should be hidden from public view. */
  protected Boolean skjult;

  /** If true, this unit is configured to use the eFormidling platform for digital communication. */
  protected Boolean eFormidling;

  /** If true, this is a technical or system-internal unit, not a real-world organizational unit. */
  protected Boolean teknisk;

  /** A flag indicating if legacy identifiers for this unit should be converted. */
  protected Boolean skalKonvertereId;

  /** A flag indicating if the unit should receive receipts for submissions. */
  protected Boolean skalMottaKvittering;

  /** A UI hint to display this unit as a top-level node in a hierarchy. */
  protected Boolean visToppnode;

  /** The version of the order XML format used by this unit. */
  protected Integer orderXmlVersjon;

  /** A list of sub-units belonging to this unit. */
  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected List<ExpandableField<EnhetDTO>> underenhet;

  /** The unit that is responsible for handling tasks on behalf of this unit. */
  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected ExpandableField<EnhetDTO> handteresAv;

  /** The parent unit in the organizational hierarchy. */
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
