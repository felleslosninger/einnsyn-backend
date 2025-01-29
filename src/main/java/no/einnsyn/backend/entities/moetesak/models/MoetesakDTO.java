// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.entities.moetesak.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.entities.moetemappe.MoetemappeService;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.moetesaksbeskrivelse.MoetesaksbeskrivelseService;
import no.einnsyn.backend.entities.moetesaksbeskrivelse.models.MoetesaksbeskrivelseDTO;
import no.einnsyn.backend.entities.registrering.models.RegistreringDTO;
import no.einnsyn.backend.entities.utredning.UtredningService;
import no.einnsyn.backend.entities.utredning.models.UtredningDTO;
import no.einnsyn.backend.entities.vedtak.VedtakService;
import no.einnsyn.backend.entities.vedtak.models.VedtakDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;
import no.einnsyn.backend.validation.validenum.ValidEnum;

@Getter
@Setter
public class MoetesakDTO extends RegistreringDTO {
  final String entity = "Moetesak";

  @ValidEnum(enumClass = MoetesakstypeEnum.class)
  @NotNull(groups = {Insert.class})
  String moetesakstype;

  @Min(1900)
  @NotNull(groups = {Insert.class})
  Integer moetesaksaar;

  @Min(0)
  @NotNull(groups = {Insert.class})
  Integer moetesakssekvensnummer;

  @NoSSN
  @Size(max = 500)
  String utvalg;

  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @Null(groups = {Insert.class, Update.class})
  ExpandableField<EnhetDTO> utvalgObjekt;

  @NoSSN
  @Size(max = 500)
  String videoLink;

  @ExpandableObject(
      service = UtredningService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<UtredningDTO> utredning;

  @ExpandableObject(
      service = MoetesaksbeskrivelseService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<MoetesaksbeskrivelseDTO> innstilling;

  @ExpandableObject(
      service = VedtakService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<VedtakDTO> vedtak;

  @ExpandableObject(
      service = MoetemappeService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<MoetemappeDTO> moetemappe;

  @NoSSN
  @Size(max = 500)
  String legacyMoetesakstype;

  @NoSSN
  @Size(max = 500)
  String legacyReferanseTilMoetesak;

  public enum MoetesakstypeEnum {
    MOETE("moete"),
    POLITISK("politisk"),
    DELEGERT("delegert"),
    INTERPELLASJON("interpellasjon"),
    GODKJENNING("godkjenning"),
    ORIENTERING("orientering"),
    REFERAT("referat"),
    ANNET("annet");

    private final String value;

    MoetesakstypeEnum(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }

    public String toJson() {
      return value;
    }

    public static MoetesakstypeEnum fromValue(String value) {
      value = value.trim().toLowerCase();
      for (MoetesakstypeEnum val : MoetesakstypeEnum.values()) {
        if (val.value.toLowerCase().equals(value)) {
          return val;
        }
      }
      throw new IllegalArgumentException("Unknown value: " + value);
    }
  }
}
