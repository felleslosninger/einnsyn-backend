package no.einnsyn.backend.entities.innsynskravbestilling.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.base.models.Base;
import no.einnsyn.backend.entities.bruker.models.Bruker;
import no.einnsyn.backend.entities.innsynskrav.models.Innsynskrav;

@Getter
@Setter
@Table(name = "innsynskrav")
@Entity
public class InnsynskravBestilling extends Base {

  @Column(name = "id", unique = true)
  private UUID innsynskravBestillingId;

  private String epost;

  @NotNull private Date opprettetDato;

  private Date sendtTilVirksomhet;

  private String verificationSecret;

  private boolean verified;

  private boolean locked = false;

  private String language = "nb";

  private Integer innsynskravVersion = 0;

  @ManyToOne @JoinColumn private Bruker bruker;

  @OneToMany(mappedBy = "innsynskravBestilling", fetch = FetchType.LAZY)
  @OrderBy("id DESC")
  private List<Innsynskrav> innsynskrav;

  // Legacy
  @Column(name = "bruker_iri")
  private String legacyBrukerIri;

  public void addInnsynskrav(Innsynskrav id) {
    if (innsynskrav == null) {
      innsynskrav = new ArrayList<>();
    }
    if (!innsynskrav.contains(id)) {
      innsynskrav.add(id);
      id.setInnsynskravBestilling(this);
    }
  }

  @PrePersist
  @Override
  protected void prePersist() {
    super.prePersist();

    if (innsynskravBestillingId == null) {
      setInnsynskravBestillingId(UUID.randomUUID());
    }

    if (opprettetDato == null) {
      setOpprettetDato(new Date());
    }

    if (legacyBrukerIri == null && bruker != null) {
      setLegacyBrukerIri("http://data.einnsyn.no/bruker/" + bruker.getBrukerId());
    }

    setInnsynskravVersion(1);
  }
}
