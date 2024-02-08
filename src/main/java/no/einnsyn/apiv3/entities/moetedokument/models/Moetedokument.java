package no.einnsyn.apiv3.entities.moetedokument.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.moetemappe.models.Moetemappe;
import no.einnsyn.apiv3.entities.registrering.models.Registrering;
import org.hibernate.annotations.Generated;

@Getter
@Setter
@Entity
@Table(name = "møtedokumentregistrering")
public class Moetedokument extends Registrering {

  // Legacy
  @Generated
  @Column(name = "møtedokumentregistrering_id", unique = true)
  private Integer moetedokumentregistreringId;

  // Legacy
  @Column(name = "møtedokumentregistrering_iri")
  private String moetedokumentregistreringIri;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "møtemappe_id", referencedColumnName = "møtemappe_id")
  private Moetemappe moetemappe;

  @Column(name = "møtedokumentregistreringstype")
  private String moetedokumentregistreringstype;

  private String administrativEnhet;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "administrativ_enhet__id")
  private Enhet administrativEnhetObjekt;

  private String saksbehandler;

  private String saksbehandlerSensitiv;

  @JoinTable(
      name = "møtedokumentregistrering_dokumentbeskrivelse",
      joinColumns = {
        @JoinColumn(
            name = "møtedokumentregistrering_id",
            referencedColumnName = "møtedokumentregistrering_id")
      },
      inverseJoinColumns = {
        @JoinColumn(
            name = "dokumentbeskrivelse_id",
            referencedColumnName = "dokumentbeskrivelse_id")
      })
  @ManyToMany
  private List<Dokumentbeskrivelse> dokumentbeskrivelse;
}
