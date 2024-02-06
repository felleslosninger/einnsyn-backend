package no.einnsyn.apiv3.entities.klassifikasjonssystem.models;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;
import no.einnsyn.apiv3.entities.arkivdel.models.Arkivdel;

@Getter
@Setter
@Entity
public class Klassifikasjonssystem extends ArkivBase {

  private String tittel;

  @ManyToOne @JoinColumn private Arkivdel arkivdel;
}
