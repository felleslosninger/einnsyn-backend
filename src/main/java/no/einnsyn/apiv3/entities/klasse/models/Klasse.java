package no.einnsyn.apiv3.entities.klasse.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;
import no.einnsyn.apiv3.entities.arkivdel.models.Arkivdel;
import no.einnsyn.apiv3.entities.klassifikasjonssystem.models.Klassifikasjonssystem;
import org.hibernate.annotations.Generated;

@Getter
@Setter
@Entity
public class Klasse extends ArkivBase {

  @Generated
  @Column(name = "klasse_id", unique = true)
  private Integer klasseId;

  private String tittel;

  @ManyToOne
  @JoinColumn(name = "parentklasse", referencedColumnName = "klasse_id")
  private Klasse parentKlasse;

  @ManyToOne
  @JoinColumn(name = "arkivdel_id", referencedColumnName = "arkivdel_id")
  private Arkivdel parentArkivdel;

  @ManyToOne
  @JoinColumn(name = "klassifikasjonssystem__id")
  private Klassifikasjonssystem parentKlassifikasjonssystem;

  @Column(name = "nøkkelord")
  private String noekkelord;

  // Legacy
  @Column(name = "klasse_iri")
  private String klasseIri;

  @PrePersist
  @Override
  protected void prePersist() {
    super.prePersist();

    // Populate required legacy fields. Use id as a replacement for IRIs
    if (klasseIri == null) {
      if (externalId != null && externalId.startsWith("http://")) {
        klasseIri = externalId;
      } else {
        klasseIri = "http://" + id;
        // The legacy API requires an externalId
        if (externalId == null) {
          externalId = klasseIri;
        }
      }
    }
  }
}
