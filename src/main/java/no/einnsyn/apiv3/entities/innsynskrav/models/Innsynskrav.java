package no.einnsyn.apiv3.entities.innsynskrav.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.bruker.models.Bruker;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDel;

@Getter
@Setter
@Table(name = "innsynskrav")
@Entity
public class Innsynskrav extends EinnsynObject {

  @Id
  @NotNull
  @Column(name = "id")
  private UUID legacyId;

  @NotNull private String epost;

  @NotNull private Date opprettetDato;

  private Date sendtTilVirksomhet;

  private String verificationSecret;

  private boolean verified;

  private String language = "nb";

  @ManyToOne
  @JoinColumn(name = "bruker_id")
  private Bruker bruker;

  @OneToMany(mappedBy = "innsynskrav", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  @NotNull
  private List<InnsynskravDel> innsynskravDel = new ArrayList<>();

  // Legacy
  private String brukerIri;

  @PrePersist
  public void prePersistInnsynskrav() {
    if (this.legacyId == null) {
      this.legacyId = UUID.randomUUID();
    }

    if (opprettetDato == null) {
      opprettetDato = new Date();
    }
  }
}
