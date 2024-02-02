// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.klasse.models;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.hasid.HasId;
import no.einnsyn.apiv3.entities.arkivdel.models.Arkivdel;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.apiv3.utils.IdGenerator;

@Getter
@Setter
public class KlasseParentDTO implements HasId {

  String id;

  @Valid ArkivdelDTO arkivdel;

  @Valid KlasseDTO klasse;

  public KlasseParentDTO(String id) {
    this.id = id;
  }

  public KlasseParentDTO(ArkivdelDTO arkivdel) {
    this.arkivdel = arkivdel;
    this.id = arkivdel.getId();
  }

  public KlasseParentDTO(KlasseDTO klasse) {
    this.klasse = klasse;
    this.id = klasse.getId();
  }

  public boolean isArkivdel() {
    return (arkivdel != null || id.startsWith(IdGenerator.getPrefix(Arkivdel.class)));
  }

  public boolean isKlasse() {
    return klasse != null || id.startsWith(IdGenerator.getPrefix(Klasse.class));
  }
}
