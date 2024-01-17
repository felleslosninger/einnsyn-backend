package no.einnsyn.apiv3.entities.moetemappe.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.indexable.Indexable;
import no.einnsyn.apiv3.entities.mappe.models.Mappe;
import no.einnsyn.apiv3.entities.moetedokument.models.Moetedokument;
import no.einnsyn.apiv3.entities.moetesak.models.Moetesak;
import org.hibernate.annotations.Generated;

@Getter
@Setter
@Entity
public class Moetemappe extends Mappe implements Indexable {

  @Generated
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

  private Instant lastIndexed;
}
