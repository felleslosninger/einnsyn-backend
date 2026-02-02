package no.einnsyn.backend.entities.lagretsoek.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.sql.Date;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.indexable.Indexable;
import no.einnsyn.backend.entities.base.models.Base;
import no.einnsyn.backend.entities.bruker.models.Bruker;

@Getter
@Setter
@Entity
@Table(name = "lagret_sok")
public class LagretSoek extends Base implements Indexable {

  @JoinColumn(name = "bruker_id", referencedColumnName = "id")
  @ManyToOne
  @NotNull
  private Bruker bruker;

  private String label;

  @Column(name = "abonnere")
  private boolean subscribe = false;

  private int hitCount = 0;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "lagretSoek", orphanRemoval = true)
  @OrderBy("id ASC")
  private List<LagretSoekHit> hitList;

  @Column(name = "search_parameters")
  private String searchParameters;

  @Column(unique = true, name = "id")
  private UUID legacyId;

  @Column(name = "sporring")
  private String legacyQuery;

  @Column(name = "sporring_es")
  private String legacyQueryEs;

  @Column(name = "opprettetDato")
  private Date legacyOpprettetDato;

  @Column(name = "oppdatert")
  private Date legacyOppdatertDato;

  @Column(insertable = false, updatable = false)
  private Instant lastIndexed;

  public void addHit(LagretSoekHit hit) {
    if (hitList == null) {
      hitList = List.of();
    }
    if (hitList.contains(hit)) {
      return;
    }
    hitList.add(hit);
    hit.setLagretSoek(this);
  }

  @PrePersist
  @Override
  protected void prePersist() {
    super.prePersist();

    if (getLegacyId() == null) {
      setLegacyId(UUID.randomUUID());
    }

    var now = new Date(System.currentTimeMillis());
    if (getLegacyOpprettetDato() == null) {
      setLegacyOpprettetDato(now);
    }

    if (getLegacyOppdatertDato() == null) {
      setLegacyOppdatertDato(now);
    }
  }
}
