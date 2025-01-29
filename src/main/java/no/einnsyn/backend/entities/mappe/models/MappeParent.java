// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.entities.mappe.models;

import jakarta.validation.Valid;
import lombok.Getter;
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
public class MappeParent implements HasId {
  @Valid String id;

  @Valid SaksmappeDTO saksmappe;

  @Valid MoetemappeDTO moetemappe;

  @Valid ArkivDTO arkiv;

  @Valid ArkivdelDTO arkivdel;

  @Valid KlasseDTO klasse;

  public MappeParent(String id) {
    this.id = id;
  }

  public MappeParent(SaksmappeDTO saksmappe) {
    this.saksmappe = saksmappe;
  }

  public MappeParent(MoetemappeDTO moetemappe) {
    this.moetemappe = moetemappe;
  }

  public MappeParent(ArkivDTO arkiv) {
    this.arkiv = arkiv;
  }

  public MappeParent(ArkivdelDTO arkivdel) {
    this.arkivdel = arkivdel;
  }

  public MappeParent(KlasseDTO klasse) {
    this.klasse = klasse;
  }

  public MappeParent(ExpandableField<?> expandableField) {
    this.id = expandableField.getId();
    var obj = expandableField.getExpandedObject();
    if (obj == null) return;
    switch (obj) {
      case SaksmappeDTO typedObj -> this.saksmappe = typedObj;
      case MoetemappeDTO typedObj -> this.moetemappe = typedObj;
      case ArkivDTO typedObj -> this.arkiv = typedObj;
      case ArkivdelDTO typedObj -> this.arkivdel = typedObj;
      case KlasseDTO typedObj -> this.klasse = typedObj;
      default ->
          throw new IllegalArgumentException(
              "Unsupported object type: " + obj.getClass().getName());
    }
  }

  public boolean isSaksmappe() {
    return saksmappe != null || id.startsWith(IdGenerator.getPrefix(Saksmappe.class) + "_");
  }

  public boolean isMoetemappe() {
    return moetemappe != null || id.startsWith(IdGenerator.getPrefix(Moetemappe.class) + "_");
  }

  public boolean isArkiv() {
    return arkiv != null || id.startsWith(IdGenerator.getPrefix(Arkiv.class) + "_");
  }

  public boolean isArkivdel() {
    return arkivdel != null || id.startsWith(IdGenerator.getPrefix(Arkivdel.class) + "_");
  }

  public boolean isKlasse() {
    return klasse != null || id.startsWith(IdGenerator.getPrefix(Klasse.class) + "_");
  }
}
