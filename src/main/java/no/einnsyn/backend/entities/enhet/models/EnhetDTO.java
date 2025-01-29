// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.entities.enhet.models;

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
  final String entity = "Enhet";

  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  String navn;

  @NoSSN
  @Size(max = 500)
  String navnNynorsk;

  @NoSSN
  @Size(max = 500)
  String navnEngelsk;

  @NoSSN
  @Size(max = 500)
  String navnSami;

  @Pattern(regexp = "^[0-9]{9}$")
  @NotBlank(groups = {Insert.class})
  String orgnummer;

  @NoSSN
  @Size(max = 500)
  String enhetskode;

  @NoSSN
  @Size(max = 500)
  String kontaktpunktAdresse;

  @Email
  @NotBlank(groups = {Insert.class})
  String kontaktpunktEpost;

  @NoSSN
  @Size(max = 500)
  String kontaktpunktTelefon;

  @Email
  @NotBlank(groups = {Insert.class})
  String innsynskravEpost;

  @ValidEnum(enumClass = EnhetstypeEnum.class)
  @NotNull(groups = {Insert.class})
  String enhetstype;

  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
  String avsluttetDato;

  Boolean skjult;

  Boolean eFormidling;

  Boolean teknisk;

  Boolean skalKonvertereId;

  Boolean skalMottaKvittering;

  Boolean visToppnode;

  Integer orderXmlVersjon;

  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  List<ExpandableField<EnhetDTO>> underenhet;

  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<EnhetDTO> handteresAv;

  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<EnhetDTO> parent;

  public enum EnhetstypeEnum {
    ADMINISTRATIVENHET("ADMINISTRATIVENHET"),
    AVDELING("AVDELING"),
    BYDEL("BYDEL"),
    DUMMYENHET("DUMMYENHET"),
    FYLKE("FYLKE"),
    KOMMUNE("KOMMUNE"),
    ORGAN("ORGAN"),
    SEKSJON("SEKSJON"),
    UTVALG("UTVALG"),
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
