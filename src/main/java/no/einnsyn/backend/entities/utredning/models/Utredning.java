package no.einnsyn.backend.entities.utredning.models;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBase;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.backend.entities.moetesak.models.Moetesak;
import no.einnsyn.backend.entities.moetesaksbeskrivelse.models.Moetesaksbeskrivelse;

@Getter
@Setter
@Entity
public class Utredning extends ArkivBase {

  @OneToOne(mappedBy = "utredning")
  private Moetesak moetesak;

  @OneToOne private Moetesaksbeskrivelse saksbeskrivelse;

  @OneToOne private Moetesaksbeskrivelse innstilling;

  @JoinTable(name = "utredning_utredningsdokument")
  @ManyToMany
  @OrderBy("id ASC")
  private List<Dokumentbeskrivelse> utredningsdokument;

  public void addUtredningsdokument(Dokumentbeskrivelse dokumentbeskrivelse) {
    if (utredningsdokument == null) {
      utredningsdokument = new ArrayList<>();
    }
    if (!utredningsdokument.contains(dokumentbeskrivelse)) {
      utredningsdokument.add(dokumentbeskrivelse);
    }
  }
}
