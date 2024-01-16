package no.einnsyn.apiv3.entities.enhet.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
import no.einnsyn.apiv3.entities.base.models.Base;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDel;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;

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
  private List<Enhet> underenhet = new ArrayList<>();

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
  private EnhetstypeEnum enhetstype;

  private boolean visToppnode;

  private boolean erTeknisk;

  private boolean skalKonvertereId;

  private boolean skalMottaKvittering;

  private Integer orderXmlVersjon;

  // The following lists can get very large, and should only be used when deleting an Enhet

  @OneToMany(
      fetch = FetchType.LAZY,
      mappedBy = "administrativEnhetObjekt",
      cascade = {CascadeType.ALL})
  private List<Journalpost> journalpost;

  @OneToMany(
      fetch = FetchType.LAZY,
      mappedBy = "journalenhet",
      cascade = {CascadeType.ALL})
  private List<Saksmappe> saksmappe;

  @OneToMany(
      fetch = FetchType.LAZY,
      mappedBy = "enhet",
      cascade = {CascadeType.ALL},
      orphanRemoval = true)
  private List<InnsynskravDel> innsynskravDel;

  /**
   * Helper that adds a underenhet to the list of underenhets and sets the parent on the underenhet
   *
   * @param underenhet
   */
  public void addUnderenhet(Enhet underenhet) {
    this.underenhet.add(underenhet);
    underenhet.setParent(this);
  }

  @PrePersist
  public void prePersist() {
    if (this.getEnhetId() == null) {
      this.setEnhetId(UUID.randomUUID());
    }

    // Set legacy field IRI
    if (this.getIri() == null) {
      this.setIri(this.getExternalId());
    }
    if (this.getIri() == null) {
      this.setIri(this.getId());
    }

    this.setOpprettetDato(new Date());
    this.setOppdatertDato(new Date());
  }

  @PreUpdate
  public void updateDates() {
    this.setOppdatertDato(new Date());
  }
}
