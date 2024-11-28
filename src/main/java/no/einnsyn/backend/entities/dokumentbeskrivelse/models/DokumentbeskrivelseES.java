package no.einnsyn.backend.entities.dokumentbeskrivelse.models;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseES;
import no.einnsyn.backend.entities.dokumentobjekt.models.DokumentobjektES;

@Getter
@Setter
public class DokumentbeskrivelseES extends ArkivBaseES {
  private String tittel;

  @SuppressWarnings("java:S116")
  private String tittel_SENSITIV;

  private String tilknyttetRegistreringSom;
  private String dokumenttype;
  private List<DokumentobjektES> dokumentobjekt;
}
