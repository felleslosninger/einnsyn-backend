package no.einnsyn.apiv3.entities.bruker.models;

import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;
import no.einnsyn.apiv3.features.validation.validationGroups.Update;


@Getter
@Setter
public class BrukerJSON extends EinnsynObjectJSON {

  private boolean active;

  @Email
  @NotNull(groups = {Insert.class, Update.class})
  private String epost;

  @Enumerated(EnumType.STRING)
  @NotNull
  private BrukerType type;

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
