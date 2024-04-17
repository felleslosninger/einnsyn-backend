package no.einnsyn.apiv3.entities.innsynskrav.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
import no.einnsyn.apiv3.entities.base.models.Base;
import no.einnsyn.apiv3.entities.bruker.models.Bruker;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDel;

@Getter
@Setter
@Table(name = "innsynskrav")
@Entity
public class Innsynskrav extends Base {

  @Column(name = "id", unique = true)
  private UUID innsynskravId;

  @NotNull private String epost;

  @NotNull private Date opprettetDato;

  private Date sendtTilVirksomhet;

  private String verificationSecret;

  private boolean verified;

  private boolean locked;

  private String language = "nb";

  @ManyToOne @JoinColumn private Bruker bruker;

  @OneToMany(
      mappedBy = "innsynskrav",
      fetch = FetchType.LAZY,
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
  private List<InnsynskravDel> innsynskravDel;

  // Legacy
  private String brukerIri;

  public void addInnsynskravDel(InnsynskravDel id) {
    if (innsynskravDel == null) {
      innsynskravDel = new ArrayList<>();
    }
    if (!innsynskravDel.contains(id)) {
      innsynskravDel.add(id);
      id.setInnsynskrav(this);
    }
  }

  @PrePersist
  public void prePersistInnsynskrav() {
    if (this.innsynskravId == null) {
      this.innsynskravId = UUID.randomUUID();
    }

    if (opprettetDato == null) {
      opprettetDato = new Date();
    }
  }
}
