package no.einnsyn.backend.entities.korrespondansepart.models;

import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseES;

@Getter
@Setter
public class KorrespondansepartES extends ArkivBaseES {
  private String korrespondansepartNavn;

  @SuppressWarnings("java:S116")
  private String korrespondansepartNavn_SENSITIV;

  private String korrespondanseparttype;
  private String postadresse;
  private String administrativEnhet;
  private boolean erBehandlingsansvarlig;

  @SuppressWarnings("java:S2065")
  private transient String saksbehandler;
}
