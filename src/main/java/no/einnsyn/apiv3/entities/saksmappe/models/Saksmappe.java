package no.einnsyn.apiv3.entities.saksmappe.models;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.hibernate.annotations.DynamicUpdate;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.mappe.models.Mappe;

@Getter
@Setter
@Entity
@DynamicUpdate
public class Saksmappe extends Mappe {

  private Integer saksaar;

  private Integer sakssekvensnummer;

  private LocalDate saksdato;

  private String administrativEnhet;

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "saksmappe")
  private List<Journalpost> journalposter = Collections.emptyList();
}
