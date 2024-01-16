package no.einnsyn.apiv3.entities.moetedokument.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.moetemappe.models.Moetemappe;
import no.einnsyn.apiv3.entities.registrering.models.Registrering;

@Getter
@Setter
@Entity
public class Moetedokument extends Registrering {

  // Legacy
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "møtedokreg_seq")
  @SequenceGenerator(
      name = "møtedokreg_seq",
      sequenceName = "møtedokumentregistrering_seq",
      allocationSize = 1)
  @Column(name = "møtedokumentregistrering_id", unique = true)
  private Integer møtedokumentregistreringId;

  // Legacy
  private String møtedokumentregistreringIri;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "møtemappe_id", referencedColumnName = "møtemappe_id")
  private Moetemappe moetemappe;

  private String møtemappeIri;

  private String møtedokumentregistreringstype;

  private String administrativEnhet;

  private String saksbehandler;

  private String saksbehandlerSensitiv;

  @JoinTable(
      name = "møtedokumentregistrering_dokumentbeskrivelse",
      joinColumns = {@JoinColumn(name = "møtedokumentregistrering_id")},
      inverseJoinColumns = {@JoinColumn(name = "dokumentbeskrivelse_id")})
  @ManyToMany
  private List<Dokumentbeskrivelse> dokumentbeskrivelse = new ArrayList<>();
}
