package no.einnsyn.backend.entities.mappe.models;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseES;
import no.einnsyn.backend.entities.matrikkelnummer.models.MatrikkelnummerES;
import no.einnsyn.backend.entities.registrering.models.RegistreringES;

@Getter
@Setter
public class MappeES extends ArkivBaseES {
  private String offentligTittel;
  private String publisertDato;
  private String oppdatertDato;

  @SuppressWarnings("java:S116")
  private String offentligTittel_SENSITIV;

  private List<RegistreringES> child;

  // Files attached to a Moetedokument have no Registrering above them, so their download
  // statistics attach to the Moetemappe. See STAT_PARENT_RELATION for why the name is reused.
  private String statRelation = STAT_PARENT_RELATION;

  private String sorteringstype = "";

  private List<MatrikkelnummerES> matrikkelnummer;
}
