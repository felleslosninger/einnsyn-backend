package no.einnsyn.apiv3.entities.lagretsoek.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotNull;
import java.sql.Date;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.base.models.Base;
import no.einnsyn.apiv3.entities.bruker.models.Bruker;

@Getter
@Setter
@Entity
public class LagretSoek extends Base {

  @JoinColumn(name = "bruker_id", referencedColumnName = "id")
  @ManyToOne
  @NotNull
  private Bruker bruker;

  private String label;

  private boolean abonnere = false;

  private String sporring;

  private String sporringEs;

  private int hitCount = 0;

  @Column(unique = true, name = "id")
  private UUID legacyId;

  @Column(name = "opprettetDato")
  private Date legacyOpprettetDato;

  @Column(name = "oppdatert")
  private Date legacyOppdatertDato;

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
