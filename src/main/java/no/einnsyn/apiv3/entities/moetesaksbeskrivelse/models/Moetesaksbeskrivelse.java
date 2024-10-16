package no.einnsyn.apiv3.entities.moetesaksbeskrivelse.models;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;

@Getter
@Setter
@Entity
public class Moetesaksbeskrivelse extends ArkivBase {
  private String tekstInnhold;

  private String tekstFormat;
}
