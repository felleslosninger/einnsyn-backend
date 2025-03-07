package no.einnsyn.backend.entities.enhet.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.base.models.Base;

@Getter
@Setter
@Entity
public class Enhet extends Base {

  @Column(name = "id", unique = true)
  private UUID enhetId;

  // Legacy
  @NotNull private String iri;

  private String navn;

  private String navnNynorsk;

  private String navnEngelsk;

  private String navnSami;

  private LocalDate avsluttetDato;

  // Legacy
  @NotNull private Date opprettetDato;

  // Legacy
  @NotNull private Date oppdatertDato;

  @ManyToOne
  @JoinColumn(name = "parent_id", referencedColumnName = "id")
  private Enhet parent;

  @OneToMany(
      fetch = FetchType.LAZY,
      mappedBy = "parent",
      cascade = {CascadeType.ALL})
  @OrderBy("id ASC")
  private List<Enhet> underenhet;

  private boolean skjult;

  @Email private String innsynskravEpost;

  private String kontaktpunktAdresse;

  @Email private String kontaktpunktEpost;

  private String kontaktpunktTelefon;

  @Column(unique = true)
  private String orgnummer;

  @ManyToOne
  @JoinColumn(name = "handteres_av_id", referencedColumnName = "id")
  private Enhet handteresAv;

  private boolean eFormidling;

  @Column(name = "enhets_kode")
  private String enhetskode;

  @Enumerated(EnumType.STRING)
  @NotNull
  @Column(name = "type") // Avoid conflict with ES indexed field in EinnsynObject by calling this
  // `enhetstype`
  private EnhetDTO.EnhetstypeEnum enhetstype;

  private boolean visToppnode;

  private boolean erTeknisk;

  private boolean skalKonvertereId;

  private boolean skalMottaKvittering;

  private Integer orderXmlVersjon;

  /**
   * Helper that adds a underenhet to the list of underenhets and sets the parent on the underenhet
   *
   * @param ue
   */
  public void addUnderenhet(Enhet ue) {
    if (underenhet == null) {
      underenhet = new ArrayList<>();
    }
    if (!underenhet.contains(ue)) {
      underenhet.add(ue);
      ue.setParent(this);
    }
  }

  @PrePersist
  @Override
  protected void prePersist() {
    super.prePersist();

    if (enhetId == null) {
      setEnhetId(UUID.randomUUID());
    }

    // Set legacy field IRI
    if (iri == null) {
      setIri(externalId);
    }
    if (iri == null) {
      setIri(id);
    }

    setOpprettetDato(new Date());
    setOppdatertDato(new Date());
  }

  @PreUpdate
  public void updateDates() {
    setOppdatertDato(new Date());
  }
}
