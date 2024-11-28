// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.mappe.models;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.hasid.HasId;
import no.einnsyn.backend.entities.arkiv.models.Arkiv;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.Arkivdel;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.klasse.models.Klasse;
import no.einnsyn.backend.entities.klasse.models.KlasseDTO;
import no.einnsyn.backend.entities.moetemappe.models.Moetemappe;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.saksmappe.models.Saksmappe;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.utils.idgenerator.IdGenerator;

@Getter
@Setter
public class MappeParentDTO implements HasId {

  String id;

  @Valid SaksmappeDTO saksmappe;

  @Valid MoetemappeDTO moetemappe;

  @Valid ArkivDTO arkiv;

  @Valid ArkivdelDTO arkivdel;

  @Valid KlasseDTO klasse;

  public MappeParentDTO(String id) {
    this.id = id;
  }

  public MappeParentDTO(ExpandableField<?> expandableField) {
    this.id = expandableField.getId();
    HasId obj = expandableField.getExpandedObject();
    if (obj instanceof SaksmappeDTO typedObj) {
      this.saksmappe = typedObj;
    }
    if (obj instanceof MoetemappeDTO typedObj) {
      this.moetemappe = typedObj;
    }
    if (obj instanceof ArkivDTO typedObj) {
      this.arkiv = typedObj;
    }
    if (obj instanceof ArkivdelDTO typedObj) {
      this.arkivdel = typedObj;
    }
    if (obj instanceof KlasseDTO typedObj) {
      this.klasse = typedObj;
    }
  }

  public MappeParentDTO(SaksmappeDTO saksmappe) {
    this.saksmappe = saksmappe;
    this.id = saksmappe.getId();
  }

  public MappeParentDTO(MoetemappeDTO moetemappe) {
    this.moetemappe = moetemappe;
    this.id = moetemappe.getId();
  }

  public MappeParentDTO(ArkivDTO arkiv) {
    this.arkiv = arkiv;
    this.id = arkiv.getId();
  }

  public MappeParentDTO(ArkivdelDTO arkivdel) {
    this.arkivdel = arkivdel;
    this.id = arkivdel.getId();
  }

  public MappeParentDTO(KlasseDTO klasse) {
    this.klasse = klasse;
    this.id = klasse.getId();
  }

  public boolean isSaksmappe() {
    return (saksmappe != null || id.startsWith(IdGenerator.getPrefix(Saksmappe.class) + "_"));
  }

  public boolean isMoetemappe() {
    return (moetemappe != null || id.startsWith(IdGenerator.getPrefix(Moetemappe.class) + "_"));
  }

  public boolean isArkiv() {
    return (arkiv != null || id.startsWith(IdGenerator.getPrefix(Arkiv.class) + "_"));
  }

  public boolean isArkivdel() {
    return (arkivdel != null || id.startsWith(IdGenerator.getPrefix(Arkivdel.class) + "_"));
  }

  public boolean isKlasse() {
    return (klasse != null || id.startsWith(IdGenerator.getPrefix(Klasse.class) + "_"));
  }
}
