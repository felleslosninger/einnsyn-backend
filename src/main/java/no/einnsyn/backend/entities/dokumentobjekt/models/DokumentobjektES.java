package no.einnsyn.backend.entities.dokumentobjekt.models;

import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseES;

@Getter
@Setter
public class DokumentobjektES extends ArkivBaseES {
  private String format;
  private String referanseDokumentfil;
}
