package no.einnsyn.apiv3.entities.skjerming.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;
import org.hibernate.annotations.DynamicUpdate;

@Getter
@Setter
@Entity
@DynamicUpdate
public class Skjerming extends EinnsynObject {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "skjerm_seq")
  @SequenceGenerator(name = "skjerm_seq", sequenceName = "skjerming_seq", allocationSize = 1)
  private Integer skjermingId;

  private String skjermingIri;

  private String tilgangsrestriksjon;

  private String skjermingshjemmel;
}
