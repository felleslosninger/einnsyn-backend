package no.einnsyn.apiv3.entities.korrespondansepart.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import org.hibernate.annotations.Generated;

@Getter
@Setter
@Entity
public class Korrespondansepart extends ArkivBase {

  @Generated
  @Column(name = "korrespondansepart_id", unique = true)
  private Integer korrespondansepartId;

  private String korrespondansepartIri;

  @NotNull
  @ManyToOne
  @JoinColumn(name = "journalpost_id", referencedColumnName = "journalpost_id")
  private Journalpost journalpost;

  private String korrespondanseparttype;

  private String korrespondansepartNavn;

  private String korrespondansepartNavnSensitiv;

  private String administrativEnhet;

  private String saksbehandler;

  private String epostadresse;

  private String postnummer;

  private boolean erBehandlingsansvarlig = false;
}
