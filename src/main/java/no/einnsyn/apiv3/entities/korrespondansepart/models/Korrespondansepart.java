package no.einnsyn.apiv3.entities.korrespondansepart.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;

@Getter
@Setter
@Entity
public class Korrespondansepart extends EinnsynObject {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "korrpart_seq")
  @SequenceGenerator(name = "korrpart_seq", sequenceName = "korrespondansepart_seq",
      allocationSize = 1)
  private Integer korrespondansepartId;

  private String korrespondansepartIri;

  @ManyToOne
  @JoinColumn(name = "journalpost_id")
  private Journalpost journalpost;

  private String korrespondanseparttype;

  private String korrespondansepartNavn;

  private String korrespondansepartNavnSensitiv;

  private String administrativEnhet;

  private String saksbehandler;

  private String epostadresse;

  private String postnummer;

  private Boolean erBehandlingsansvarlig;


  @PrePersist
  public void prePersist() {
    super.prePersist();
  }
}
