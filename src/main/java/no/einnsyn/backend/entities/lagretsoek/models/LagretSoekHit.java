package no.einnsyn.backend.entities.lagretsoek.models;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.base.models.Base;
import no.einnsyn.backend.entities.journalpost.models.Journalpost;
import no.einnsyn.backend.entities.moetemappe.models.Moetemappe;
import no.einnsyn.backend.entities.moetesak.models.Moetesak;
import no.einnsyn.backend.entities.saksmappe.models.Saksmappe;

/** This table is used as a cache for search hits that are not yet notified to the user. */
@Getter
@Setter
@Entity
public class LagretSoekHit extends Base {
  @JoinColumn(name = "lagret_soek__id", referencedColumnName = "_id")
  @ManyToOne(fetch = FetchType.EAGER)
  @NotNull
  private LagretSoek lagretSoek;

  @JoinColumn
  @ManyToOne(fetch = FetchType.EAGER)
  private Saksmappe saksmappe;

  @JoinColumn
  @ManyToOne(fetch = FetchType.EAGER)
  private Journalpost journalpost;

  @JoinColumn
  @ManyToOne(fetch = FetchType.EAGER)
  private Moetemappe moetemappe;

  @JoinColumn
  @ManyToOne(fetch = FetchType.EAGER)
  private Moetesak moetesak;
}
