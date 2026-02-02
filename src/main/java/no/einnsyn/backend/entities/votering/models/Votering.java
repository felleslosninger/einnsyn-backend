package no.einnsyn.backend.entities.votering.models;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBase;
import no.einnsyn.backend.entities.identifikator.models.Identifikator;
import no.einnsyn.backend.entities.moetedeltaker.models.Moetedeltaker;
import no.einnsyn.backend.entities.vedtak.models.Vedtak;

@Getter
@Setter
@Entity
public class Votering extends ArkivBase {

  @ManyToOne
  @JoinColumn(name = "moetedeltaker__id")
  private Moetedeltaker moetedeltaker;

  @ManyToOne
  @JoinColumn(name = "representerer__id")
  private Identifikator representerer;

  private VoteringDTO.StemmeEnum stemme;

  @ManyToOne
  @JoinColumn(name = "vedtak__id")
  private Vedtak vedtak;
}
