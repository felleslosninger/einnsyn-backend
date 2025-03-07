package no.einnsyn.backend.entities.vedtak.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBase;
import no.einnsyn.backend.entities.behandlingsprotokoll.models.Behandlingsprotokoll;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.backend.entities.moetesak.models.Moetesak;
import no.einnsyn.backend.entities.moetesaksbeskrivelse.models.Moetesaksbeskrivelse;
import no.einnsyn.backend.entities.votering.models.Votering;

@Getter
@Setter
@Entity
public class Vedtak extends ArkivBase {

  @OneToOne(mappedBy = "vedtak")
  private Moetesak moetesak;

  @OneToOne(cascade = {CascadeType.PERSIST})
  private Moetesaksbeskrivelse vedtakstekst;

  @OneToOne(cascade = {CascadeType.PERSIST})
  private Behandlingsprotokoll behandlingsprotokoll;

  @OneToMany(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH},
      mappedBy = "vedtak")
  @OrderBy("id ASC")
  private List<Votering> votering;

  @JoinTable(name = "vedtak_vedtaksdokument")
  @ManyToMany(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
  @OrderBy("id ASC")
  private List<Dokumentbeskrivelse> vedtaksdokument;

  private LocalDate dato;

  public void addVotering(Votering votering) {
    if (this.votering == null) {
      this.votering = new ArrayList<>();
    }
    if (!this.votering.contains(votering)) {
      this.votering.add(votering);
    }
  }

  public void addVedtaksdokument(Dokumentbeskrivelse vedtaksdokument) {
    if (this.vedtaksdokument == null) {
      this.vedtaksdokument = new ArrayList<>();
    }
    if (!this.vedtaksdokument.contains(vedtaksdokument)) {
      this.vedtaksdokument.add(vedtaksdokument);
    }
  }
}
