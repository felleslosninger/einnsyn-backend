// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.klasse.models;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.hasid.HasId;
import no.einnsyn.backend.entities.arkivdel.models.Arkivdel;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.klassifikasjonssystem.models.Klassifikasjonssystem;
import no.einnsyn.backend.entities.klassifikasjonssystem.models.KlassifikasjonssystemDTO;
import no.einnsyn.backend.utils.idgenerator.IdGenerator;

@Getter
@Setter
public class KlasseParentDTO implements HasId {

  String id;

  @Valid ArkivdelDTO arkivdel;

  @Valid KlasseDTO klasse;

  @Valid KlassifikasjonssystemDTO klassifikasjonssystem;

  public KlasseParentDTO(String id) {
    this.id = id;
  }

  public KlasseParentDTO(ExpandableField<?> expandableField) {
    this.id = expandableField.getId();
    HasId obj = expandableField.getExpandedObject();
    if (obj instanceof ArkivdelDTO typedObj) {
      this.arkivdel = typedObj;
    }
    if (obj instanceof KlasseDTO typedObj) {
      this.klasse = typedObj;
    }
    if (obj instanceof KlassifikasjonssystemDTO typedObj) {
      this.klassifikasjonssystem = typedObj;
    }
  }

  public KlasseParentDTO(ArkivdelDTO arkivdel) {
    this.arkivdel = arkivdel;
    this.id = arkivdel.getId();
  }

  public KlasseParentDTO(KlasseDTO klasse) {
    this.klasse = klasse;
    this.id = klasse.getId();
  }

  public KlasseParentDTO(KlassifikasjonssystemDTO klassifikasjonssystem) {
    this.klassifikasjonssystem = klassifikasjonssystem;
    this.id = klassifikasjonssystem.getId();
  }

  public boolean isArkivdel() {
    return (arkivdel != null || id.startsWith(IdGenerator.getPrefix(Arkivdel.class) + "_"));
  }

  public boolean isKlasse() {
    return (klasse != null || id.startsWith(IdGenerator.getPrefix(Klasse.class) + "_"));
  }

  public boolean isKlassifikasjonssystem() {
    return (klassifikasjonssystem != null
        || id.startsWith(IdGenerator.getPrefix(Klassifikasjonssystem.class) + "_"));
  }
}
