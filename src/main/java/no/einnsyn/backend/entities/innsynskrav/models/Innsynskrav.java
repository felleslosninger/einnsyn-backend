package no.einnsyn.backend.entities.innsynskrav.models;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.indexable.Indexable;
import no.einnsyn.backend.entities.base.models.Base;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestilling;
import no.einnsyn.backend.entities.journalpost.models.Journalpost;

@Getter
@Setter
@Table(name = "innsynskrav_del")
@Entity
public class Innsynskrav extends Base implements Indexable {

  @Column(name = "id", unique = true)
  private UUID legacyId;

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
  private InnsynskravBestilling innsynskravBestilling;

  @ManyToOne(optional = true)
  @JoinColumn
  private Journalpost journalpost;

  @ManyToOne(fetch = FetchType.EAGER)
  @NotNull
  @JoinColumn
  private Enhet enhet;

  // lastIndexed should not be updated through JPA
  @Column(insertable = false, updatable = false)
  private Instant lastIndexed;

  @ElementCollection
  @CollectionTable(
      name = "innsynskrav_del_status",
      indexes = {@Index(columnList = "innsynskrav_del_id, status")},
      joinColumns = @JoinColumn(name = "innsynskrav_del_id", referencedColumnName = "id"))
  private List<InnsynskravStatus> legacyStatus = new ArrayList<>();

  // Legacy (this is an IRI)
  @NotNull private String rettetMot;

  // Legacy (this is an IRI)
  @NotNull private String virksomhet;

  @PrePersist
  @Override
  protected void prePersist() {
    super.prePersist();

    if (legacyId == null) {
      setLegacyId(UUID.randomUUID());
    }

    // Set legacy rettetMot value
    setRettetMot(journalpost.getJournalpostIri());

    // Set legacy virksomhet value
    setVirksomhet(enhet.getIri());
  }
}
