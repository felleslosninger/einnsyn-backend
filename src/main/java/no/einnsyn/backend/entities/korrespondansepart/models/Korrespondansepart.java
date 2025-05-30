package no.einnsyn.backend.entities.korrespondansepart.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBase;
import no.einnsyn.backend.entities.journalpost.models.Journalpost;
import no.einnsyn.backend.entities.moetedokument.models.Moetedokument;
import no.einnsyn.backend.entities.moetesak.models.Moetesak;
import no.einnsyn.backend.utils.IRIMatcher;
import org.hibernate.annotations.Generated;

@Getter
@Setter
@Entity
public class Korrespondansepart extends ArkivBase {

  @Generated
  @Column(name = "korrespondansepart_id", unique = true)
  private Integer korrespondansepartId;

  private String korrespondansepartIri;

  @ManyToOne
  @JoinColumn(name = "journalpost_id", referencedColumnName = "journalpost_id")
  private Journalpost parentJournalpost;

  @ManyToOne
  @JoinColumn(name = "moetedokument__id")
  private Moetedokument parentMoetedokument;

  @ManyToOne
  @JoinColumn(name = "moetesak__id")
  private Moetesak parentMoetesak;

  private String korrespondanseparttype;

  public String getKorrespondanseparttype() {
    return KorrespondanseparttypeResolver.fromIRI(korrespondanseparttype);
  }

  public void setKorrespondanseparttype(String korrespondanseparttype) {
    this.korrespondanseparttype = KorrespondanseparttypeResolver.toIRI(korrespondanseparttype);
  }

  private String korrespondansepartNavn;

  private String korrespondansepartNavnSensitiv;

  private String administrativEnhet;

  private String saksbehandler;

  private String epostadresse;

  private String postnummer;

  private boolean erBehandlingsansvarlig = false;

  @PrePersist
  @Override
  protected void prePersist() {
    super.prePersist();

    if (korrespondansepartIri == null) {
      if (externalId != null && IRIMatcher.matches(externalId)) {
        korrespondansepartIri = externalId;
      } else {
        korrespondansepartIri = "http://" + id;
        // The legacy API requires an externalId
        if (externalId == null) {
          externalId = korrespondansepartIri;
        }
      }
    }
  }
}
