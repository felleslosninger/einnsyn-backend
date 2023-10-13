package no.einnsyn.apiv3.entities.bruker.models;

import java.util.Date;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;

@Getter
@Setter
public class Bruker extends EinnsynObject {

  @Id
  @NotNull
  @Column(name = "id")
  private UUID legacyId;

  @NotNull
  private Date oppdatertDato;

  @NotNull
  private Date opprettetDato;

  @NotNull
  private boolean active;

  @Column(unique = true)
  @Email
  private String epost;

  @Enumerated(EnumType.STRING)
  @NotNull
  private BrukerType type;

  @NotNull
  @Column(unique = true)
  private String brukernavn;

  private String passord;

  private Date passordExpiry;

  @Column(unique = true)
  private String secret;

  private Date secretExpiry;

  private int loginForsok;

  private String virksomhet;

  // @OneToMany(fetch = FetchType.LAZY, mappedBy = "bruker", cascade = CascadeType.ALL,
  // orphanRemoval = true)
  // private List<LagretSak> lagredeSaker;

  // @OneToMany(fetch = FetchType.LAZY, mappedBy = "bruker", cascade = CascadeType.ALL,
  // orphanRemoval = true)
  // private List<LagretSok> lagredeSok;

}
