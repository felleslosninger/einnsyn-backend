package no.einnsyn.apiv3.entities.moetesak.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.indexable.Indexable;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.moetemappe.models.Moetemappe;
import no.einnsyn.apiv3.entities.registrering.models.Registrering;
import org.hibernate.annotations.Generated;

@Getter
@Setter
@Entity
public class Moetesak extends Registrering implements Indexable {
  // Legacy
  @Generated
  @Column(name = "møtesaksregistrering_id", unique = true)
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

  private Instant lastIndexed;

  @ManyToOne
  @JoinColumn(name = "møtemappe_id", referencedColumnName = "møtemappe_id")
  private Moetemappe moetemappe;

  // Legacy
  private String møtemappeIri;

  private String journalpostIri;

  @JoinTable(
      name = "møtesaksregistrering_dokumentbeskrivelse",
      joinColumns = {
        @JoinColumn(
            name = "møtesaksregistrering_id",
            referencedColumnName = "møtesaksregistrering_id")
      },
      inverseJoinColumns = {
        @JoinColumn(
            name = "dokumentbeskrivelse_id",
            referencedColumnName = "dokumentbeskrivelse_id")
      })
  @ManyToMany
  private List<Dokumentbeskrivelse> dokumentbeskrivelse;
}
