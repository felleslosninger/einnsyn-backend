package no.einnsyn.apiv3.entities.bruker.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.base.models.Base;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;

@Getter
@Setter
@Entity
public class Bruker extends Base {

  @Column(name = "id", unique = true)
  private UUID brukerId;

  @NotNull private boolean active;

  @Column(name = "epost", unique = true)
  @Email
  private String email;

  @Column(name = "passord")
  private String password;

  @Column(unique = true)
  private String secret;

  private ZonedDateTime secretExpiry;

  private int loginForsok;

  private LanguageEnum language = LanguageEnum.NB;

  // Legacy
  private Date passordExpiry;

  // Legacy
  @NotNull private Date oppdatertDato;

  // Legacy
  @NotNull private Date opprettetDato;

  // Legacy
  @NotNull private String type = "Sluttbruker";

  // Legacy
  @NotNull
  @Column(unique = true)
  private String brukernavn;

  // Legacy
  private String virksomhet;

  @OneToMany(
      fetch = FetchType.LAZY,
      mappedBy = "bruker",
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH},
      orphanRemoval = true)
  private List<Innsynskrav> innsynskrav;

  // @OneToMany(fetch = FetchType.LAZY, mappedBy = "bruker", cascade = {CascadeType.MERGE,
  // CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH},
  // orphanRemoval = true)
  // private List<LagretSak> lagredeSaker;

  // @OneToMany(fetch = FetchType.LAZY, mappedBy = "bruker", cascade = {CascadeType.MERGE,
  // CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH},
  // orphanRemoval = true)
  // private List<LagretSok> lagredeSok;

  @PrePersist
  @Override
  protected void prePersist() {
    super.prePersist();

    if (getBrukerId() == null) {
      setBrukerId(UUID.randomUUID());
    }

    if (getBrukernavn() == null) {
      setBrukernavn(email);
    }

    var now = new Date(System.currentTimeMillis());
    if (getOppdatertDato() == null) {
      setOppdatertDato(now);
    }

    if (getOpprettetDato() == null) {
      setOpprettetDato(now);
    }
  }
}
