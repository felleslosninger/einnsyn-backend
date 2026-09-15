package no.einnsyn.backend.entities.matrikkelnummer.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBase;
import no.einnsyn.backend.entities.journalpost.models.Journalpost;
import no.einnsyn.backend.entities.moetedokument.models.Moetedokument;
import no.einnsyn.backend.entities.moetemappe.models.Moetemappe;
import no.einnsyn.backend.entities.moetesak.models.Moetesak;
import no.einnsyn.backend.entities.saksmappe.models.Saksmappe;
import org.hibernate.annotations.Generated;

@Getter
@Setter
@Entity
public class Matrikkelnummer extends ArkivBase {

  @Generated
  @Column(name = "matrikkelnummer_id", unique = true)
  private Integer matrikkelnummerId;

  private String kommunenummer;

  private Integer gaardsnummer;

  private Integer bruksnummer;

  private Integer festenummer;

  private Integer seksjonsnummer;

  @ManyToOne
  @JoinColumn(name = "saksmappe__id", referencedColumnName = "_id")
  private Saksmappe saksmappe;

  @ManyToOne
  @JoinColumn(name = "moetemappe__id", referencedColumnName = "_id")
  private Moetemappe moetemappe;

  @ManyToOne
  @JoinColumn(name = "journalpost__id", referencedColumnName = "_id")
  private Journalpost journalpost;

  @ManyToOne
  @JoinColumn(name = "moetesak__id", referencedColumnName = "_id")
  private Moetesak moetesak;

  @ManyToOne
  @JoinColumn(name = "moetedokument__id", referencedColumnName = "_id")
  private Moetedokument moetedokument;
}
