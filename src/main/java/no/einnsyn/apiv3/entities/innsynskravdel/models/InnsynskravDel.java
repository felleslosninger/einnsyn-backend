package no.einnsyn.apiv3.entities.innsynskravdel.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.base.models.Base;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;

@Getter
@Setter
@Table(name = "innsynskrav_del")
@Entity
public class InnsynskravDel extends Base {

  @Column(name = "id", unique = true)
  private UUID innsynskravDelId;

  @NotNull private boolean skjult = false;

  // Inserts / updates are handled manually, to avoid optimistic locking exceptions
  @Column(insertable = false, updatable = false)
  private int retryCount = 0;

  // Inserts / updates are handled manually, to avoid optimistic locking exceptions
  @Column(insertable = false, updatable = false)
  private Instant retryTimestamp;

  // Inserts / updates are handled manually, to avoid optimistic locking exceptions
  @Column(insertable = false, updatable = false)
  private Instant sent;

  @ManyToOne
  @NotNull
  @JoinColumn(name = "innsynskrav_id", referencedColumnName = "id")
  private Innsynskrav innsynskrav;

  @ManyToOne @NotNull @JoinColumn private Journalpost journalpost;

  @ManyToOne(fetch = FetchType.EAGER)
  @NotNull
  @JoinColumn
  private Enhet enhet;

  @ElementCollection(targetClass = InnsynskravDelStatus.class)
  @CollectionTable(
      indexes = {@Index(columnList = "innsynskrav, status")},
      joinColumns = @JoinColumn(referencedColumnName = "id"))
  @NotNull
  private List<InnsynskravDelStatus> status;

  // Legacy (this is an IRI)
  @NotNull private String rettetMot;

  // Legacy (this is an IRI)
  @NotNull private String virksomhet;

  @PrePersist
  @Override
  protected void prePersist() {
    super.prePersist();

    if (innsynskravDelId == null) {
      setInnsynskravDelId(UUID.randomUUID());
    }

    // Set legacy rettetMot value
    setRettetMot(journalpost.getJournalpostIri());

    // Set legacy virksomhet value
    setVirksomhet(enhet.getIri());
  }
}
