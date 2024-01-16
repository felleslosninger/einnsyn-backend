package no.einnsyn.apiv3.entities.moetemappe.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.mappe.models.Mappe;
import no.einnsyn.apiv3.entities.moetedokument.models.Moetedokument;
import no.einnsyn.apiv3.entities.moetesak.models.Moetesak;

@Getter
@Setter
@Entity
public class Moetemappe extends Mappe {

  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "møtemap_seq")
  @SequenceGenerator(name = "møtemap_seq", sequenceName = "møtemappe_seq", allocationSize = 1)
  @Column(name = "møtemappe_id", unique = true)
  private Integer møtemappeId;

  private String møtemappeIri;

  private String møtenummer;

  private String utvalg;

  private Instant møtedato;

  private String møtested;

  private String videolink;

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "moetemappe")
  private List<Moetesak> møtesaksregistreringer = Collections.emptyList();

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "moetemappe")
  private List<Moetedokument> møtedokumentregistreringer = Collections.emptyList();
}
