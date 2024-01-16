package no.einnsyn.apiv3.entities.moetesak.models;

import jakarta.persistence.Entity;
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
public class Moetesak extends Registrering {
  // Legacy
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "møtesakreg_seq")
  @SequenceGenerator(
      name = "møtesakreg_seq",
      sequenceName = "møtesaksregistrering_seq",
      allocationSize = 1)
  private Integer møtesaksregistreringId;

  // Legacy
  private String møtesaksregistreringIri;

  // Legacy
  private String arkivskaper;

  private String registreringsidVar;

  private String møtesakstype;

  private String sorteringstype;

  private String administrativEnhet;

  private Integer møtesakssekvensnummer;

  private Integer møtesaksår;

  private String videolink;

  private String saksbehandler;

  private String saksbehandlerSensitiv;

  @ManyToOne
  @JoinColumn(name = "møtemappe_id", referencedColumnName = "møtemappe_id")
  private Moetemappe moetemappe;

  // Legacy
  private String møtemappeIri;

  private String journalpostIri;

  @JoinTable(
      name = "møtesaksregistrering_dokumentbeskrivelse",
      joinColumns = {@JoinColumn(name = "møtesaksregistrering_id")},
      inverseJoinColumns = {@JoinColumn(name = "dokumentbeskrivelse_id")})
  @ManyToMany
  private List<Dokumentbeskrivelse> dokumentbeskrivelse = new ArrayList<>();
}
