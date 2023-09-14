package no.einnsyn.apiv3.entities.enhet.models;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;

@Getter
@Setter
public class Enhet extends EinnsynObject {

  @Column(name = "id")
  private String legacyId;

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
  private boolean eFormidling;

  private String enhetsKode;

  @Enumerated(EnumType.STRING)
  @NotNull
  private Enhetstype type;

  @NotNull
  private boolean visToppnode;

  @NotNull
  private boolean erTeknisk;

  @NotNull
  private boolean skalKonvertereId;

  @NotNull
  private boolean skalMottaKvittering;

  private Integer orderXmlVersjon;
}
