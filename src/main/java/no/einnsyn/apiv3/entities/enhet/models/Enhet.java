package no.einnsyn.apiv3.entities.enhet.models;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;

@Getter
@Setter
@Entity
@DynamicUpdate
public class Enhet extends EinnsynObject {

  @Id
  @Column(name = "id")
  private UUID legacyId;

  private String navn;

  private String navnNynorsk;

  private String navnEngelsk;

  private String navnSami;

  // Legacy
  @NotNull
  private Date opprettetDato;

  // Legacy
  @NotNull
  private Date oppdatertDato;

  private LocalDate avsluttetDato;

  @ManyToOne
  private Enhet parent;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "parent")
  private List<Enhet> underEnheter;

  @NotNull
  private boolean skjult;

  @Email
  private String innsynskravEpost;

  private String kontaktpunktAdresse;

  @Email
  private String kontaktpunktEpost;

  private String kontaktpunktTelefon;

  @Column(unique = true)
  private String orgnummer;

  @ManyToOne
  private Enhet handteresAv;

  @NotNull
  private Boolean eFormidling;

  @Column(name = "enhets_kode")
  private String enhetskode;

  @Enumerated(EnumType.STRING)
  @NotNull
  @Column(name = "type") // Avoid conflict with ES indexed field in EinnsynObject by calling this
                         // `enhetstype`
  private Enhetstype enhetstype;

  @NotNull
  private Boolean visToppnode;

  @NotNull
  private Boolean erTeknisk;

  @NotNull
  private Boolean skalKonvertereId;

  @NotNull
  private Boolean skalMottaKvittering;

  private Integer orderXmlVersjon;
}
