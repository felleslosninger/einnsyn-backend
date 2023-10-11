package no.einnsyn.apiv3.entities.innsynskravdel.models;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;

@Getter
@Setter
@Table(name = "innsynskrav_del")
@Entity
public class InnsynskravDel extends EinnsynObject {

  @Id
  @NotNull
  @Column(name = "id")
  private UUID legacyId;

  @NotNull
  private Boolean skjult = false;

  private int retryCount = 0;

  private Instant retryTimestamp;

  private Instant sent;

  @ManyToOne
  @NotNull
  @JoinColumn(name = "innsynskrav_id")
  private Innsynskrav innsynskrav;

  @ManyToOne
  @NotNull
  @JoinColumn(name = "journalpost_id")
  private Journalpost journalpost;

  @ManyToOne
  @NotNull
  private Enhet enhet;

  // @ElementCollection
  // @CollectionTable(indexes = {@Index(columnList = "innsynskrav, status")})
  // @NotNull
  // private List<InnsynskravDelStatus> status;


  // Legacy (this is an IRI)
  @NotNull
  private String rettetMot;

  // Legacy (this is an IRI)
  @NotNull
  private String virksomhet;


  @PrePersist
  public void prePersist() {
    if (legacyId == null) {
      legacyId = UUID.randomUUID();
    }

    // Set legacy rettetMot value
    rettetMot = journalpost.getJournalpostIri();

    // Set legacy virksomhet value
    virksomhet = enhet.getIri();
  }
}
