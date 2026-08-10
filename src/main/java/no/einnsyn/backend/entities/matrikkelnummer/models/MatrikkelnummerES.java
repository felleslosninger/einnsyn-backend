package no.einnsyn.backend.entities.matrikkelnummer.models;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseES;

@Getter
@Setter
public class MatrikkelnummerES extends ArkivBaseES {

  private String kommunenummer;
  private Integer gaardsnummer;
  private Integer bruksnummer;
  private Integer festenummer;
  private Integer seksjonsnummer;

  // Multiple search-friendly string variants per matrikkelnummer
  private List<String> matrikkelId;
}
