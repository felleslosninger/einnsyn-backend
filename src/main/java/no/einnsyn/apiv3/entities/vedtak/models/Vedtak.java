package no.einnsyn.apiv3.entities.vedtak.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;
import no.einnsyn.apiv3.entities.behandlingsprotokoll.models.Behandlingsprotokoll;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.moetesaksbeskrivelse.models.Moetesaksbeskrivelse;
import no.einnsyn.apiv3.entities.votering.models.Votering;

@Getter
@Setter
@Entity
public class Vedtak extends ArkivBase {

  @OneToOne(
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
  @JoinColumn(name = "vedtakstekst__id")
  private Moetesaksbeskrivelse vedtakstekst;

  @OneToOne(
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
  @JoinColumn(name = "behandlingsprotokoll__id")
  private Behandlingsprotokoll behandlingsprotokoll;

  @OneToMany(
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH},
      mappedBy = "vedtak")
  private List<Votering> votering;

  @JoinTable(name = "vedtak_vedtaksdokument")
  @ManyToMany(
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
  private List<Dokumentbeskrivelse> vedtaksdokument;

  private LocalDate dato;

  public void addVotering(Votering votering) {
    if (this.votering == null) {
      this.votering = new ArrayList<>();
    }
    this.votering.add(votering);
  }

  public void addVedtaksdokument(Dokumentbeskrivelse vedtaksdokument) {
    if (this.vedtaksdokument == null) {
      this.vedtaksdokument = new ArrayList<>();
    }
    this.vedtaksdokument.add(vedtaksdokument);
  }
}
