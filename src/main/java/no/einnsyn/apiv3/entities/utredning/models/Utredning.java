package no.einnsyn.apiv3.entities.utredning.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.moetesaksbeskrivelse.models.Moetesaksbeskrivelse;

@Getter
@Setter
@Entity
public class Utredning extends ArkivBase {

  @OneToOne(
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
  private Moetesaksbeskrivelse saksbeskrivelse;

  @OneToOne(
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
  private Moetesaksbeskrivelse innstilling;

  @JoinTable(name = "utredning_utredningsdokument")
  @ManyToMany(
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
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
