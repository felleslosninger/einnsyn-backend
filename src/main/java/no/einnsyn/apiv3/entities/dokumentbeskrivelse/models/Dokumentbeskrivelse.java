package no.einnsyn.apiv3.entities.dokumentbeskrivelse.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.Dokumentobjekt;
import org.hibernate.annotations.Generated;

@Getter
@Setter
@Entity
public class Dokumentbeskrivelse extends ArkivBase {

  @Generated
  @Column(name = "dokumentbeskrivelse_id", unique = true)
  private Integer dokumentbeskrivelseId;

  private Integer dokumentnummer;

  private String tilknyttetRegistreringSom;

  private String dokumenttype;

  private String tittel;

  @SuppressWarnings("java:S116")
  private String tittel_SENSITIV;

  // Legacy
  @NotNull private String dokumentbeskrivelseIri;

  @OneToMany(
      fetch = FetchType.EAGER,
      mappedBy = "dokumentbeskrivelse",
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
  private List<Dokumentobjekt> dokumentobjekt;

  public void addDokumentobjekt(Dokumentobjekt dobj) {
    if (dokumentobjekt == null) {
      dokumentobjekt = new ArrayList<>();
    }
    if (!dokumentobjekt.contains(dobj)) {
      dokumentobjekt.add(dobj);
      dobj.setDokumentbeskrivelse(this);
    }
  }

  // Set legacy values
  @PrePersist
  public void prePersistDokumentbeskrivelse() {
    // Set values to legacy field DokumentbeskrivelseIri
    // Try externalId first (if one is given), use generated id if not
    if (getDokumentbeskrivelseIri() == null) {
      if (getExternalId() != null) {
        setDokumentbeskrivelseIri(getExternalId());
      } else {
        setDokumentbeskrivelseIri(getId());
      }
    }
  }
}
