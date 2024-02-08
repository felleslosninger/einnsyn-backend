package no.einnsyn.apiv3.entities.votering.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;
import no.einnsyn.apiv3.entities.identifikator.models.Identifikator;
import no.einnsyn.apiv3.entities.moetedeltaker.models.Moetedeltaker;
import no.einnsyn.apiv3.entities.vedtak.models.Vedtak;

@Getter
@Setter
@Entity
public class Votering extends ArkivBase {

  @ManyToOne(
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
  @JoinColumn(name = "moetedeltaker__id")
  private Moetedeltaker moetedeltaker;

  @ManyToOne(
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
  @JoinColumn(name = "representerer__id")
  private Identifikator representerer;

  private StemmeEnum stemme;

  @ManyToOne(
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
  @JoinColumn(name = "vedtak__id")
  private Vedtak vedtak;
}
