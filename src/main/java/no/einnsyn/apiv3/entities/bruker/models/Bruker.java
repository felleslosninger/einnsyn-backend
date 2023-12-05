package no.einnsyn.apiv3.entities.bruker.models;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;

@Getter
@Setter
@Entity
public class Bruker extends EinnsynObject {

  @Id
  @NotNull
  @Column(name = "id")
  private UUID legacyId;

  @NotNull
  private boolean active;

  @Column(name = "epost", unique = true)
  @Email
  private String email;

  @Column(name = "passord")
  private String password;

  @Column(unique = true)
  private String secret;

  private ZonedDateTime secretExpiry;

  private int loginForsok;

  private String language = "nb";

  // Legacy
  private Date passordExpiry;

  // Legacy
  @NotNull
  private Date oppdatertDato;

  // Legacy
  @NotNull
  private Date opprettetDato;

  // Legacy
  @Enumerated(EnumType.STRING)
  @NotNull
  private BrukerType type;

  // Legacy
  @NotNull
  @Column(unique = true)
  private String brukernavn;

  // Legacy
  private String virksomhet;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "bruker", cascade = CascadeType.ALL,
      orphanRemoval = true)
  private List<Innsynskrav> innsynskrav;

  // @OneToMany(fetch = FetchType.LAZY, mappedBy = "bruker", cascade = CascadeType.ALL,
  // orphanRemoval = true)
  // private List<LagretSak> lagredeSaker;

  // @OneToMany(fetch = FetchType.LAZY, mappedBy = "bruker", cascade = CascadeType.ALL,
  // orphanRemoval = true)
  // private List<LagretSok> lagredeSok;

  @PrePersist
  public void prePersist() {
    if (legacyId == null) {
      legacyId = UUID.randomUUID();
    }

    if (brukernavn == null) {
      brukernavn = email;
    }

    if (oppdatertDato == null) {
      oppdatertDato = new Date();
    }

    if (opprettetDato == null) {
      opprettetDato = new Date();
    }

    if (type == null) {
      type = BrukerType.SLUTTBRUKER;
    }
  }

}
