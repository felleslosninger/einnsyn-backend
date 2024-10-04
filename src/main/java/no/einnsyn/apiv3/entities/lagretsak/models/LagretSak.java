package no.einnsyn.apiv3.entities.lagretsak.models;

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
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.moetemappe.models.Moetemappe;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;

@Getter
@Setter
@Entity
public class LagretSak extends Base {

  @JoinColumn(name = "bruker_id", referencedColumnName = "id")
  @ManyToOne
  @NotNull
  private Bruker bruker;

  @JoinColumn @ManyToOne private Saksmappe saksmappe;

  @JoinColumn @ManyToOne private Moetemappe moetemappe;

  @JoinColumn @ManyToOne private Enhet enhet;

  private boolean abonnere = false;

  private int hitCount = 0;

  @Column(unique = true, name = "id")
  private UUID legacyId;

  @Column(name = "opprettet")
  private Date legacyOpprettetDato;

  @Column(name = "oppdatert")
  private Date legacyOppdatertDato;

  @Column(name = "sak_id")
  private String legacySakIri;

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
