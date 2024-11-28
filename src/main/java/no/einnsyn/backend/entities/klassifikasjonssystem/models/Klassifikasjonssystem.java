package no.einnsyn.backend.entities.klassifikasjonssystem.models;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBase;
import no.einnsyn.backend.entities.arkivdel.models.Arkivdel;

@Getter
@Setter
@Entity
public class Klassifikasjonssystem extends ArkivBase {

  private String tittel;

  @ManyToOne
  @JoinColumn(name = "arkivdel__id")
  private Arkivdel arkivdel;
}
