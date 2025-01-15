package no.einnsyn.backend.entities.base.models;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.utils.idgenerator.IdGenerator;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Base class for all eInnsyn objects, containing metadata fields that are common to all objects.
 */
@FilterDef(
    name = "combinedFilter",
    applyToLoadByKey = true,
    parameters =
        @ParamDef(name = "journalenhet", type = String.class, resolver = Base.DefSupply.class),
    defaultCondition = "COALESCE(:journalenhet, 'default') = 'default'")
@FilterDef(
    name = "visibilityFilter",
    applyToLoadByKey = true,
    parameters =
        @ParamDef(name = "journalenhet", type = String.class, resolver = Base.DefSupply.class),
    defaultCondition = "COALESCE(:journalenhet, 'default') = 'default'")
@Filter(
    name = "visibilityFilter",
    condition = "current_date >= COALESCE($FILTER_PLACEHOLDER$._visible_from, current_date) ")
@MappedSuperclass
@Getter
@Setter
public abstract class Base {

  protected static class DefSupply implements Supplier<String> {
    @Override
    public String get() {
      return "'default'";
    }
  }

  // An ID that can be exposed to the public
  @Id
  @NotNull
  @Column(name = "_id")
  protected String id;

  // Replaces old IRIs, the client's reference id
  @Column(name = "_external_id")
  protected String externalId;

  @CreationTimestamp
  @Column(name = "_created")
  protected Instant created;

  @UpdateTimestamp
  @Column(name = "_updated")
  protected Instant updated;

  @Column(name = "_visible_from")
  protected LocalDate visibleFrom;

  @Version protected Long lockVersion;

  @PrePersist
  protected void prePersist() {
    setId(IdGenerator.generateId(getClass()));
    if (visibleFrom == null) {
      setVisibleFrom(LocalDate.now());
    }
  }

  @PreUpdate
  protected void preUpdate() {
    setUpdated(Instant.now());
    if (visibleFrom == null) {
      setVisibleFrom(LocalDate.now());
    }
  }
}

/*
api_key
driftsmelding
enhet
flyway_schema_history
frontend_states
innsynskrav
innsynskrav_del
innsynskrav_del_status
journalpost_dokumentbeskrivelse
journalpost_følgsakenreferanse
kategori
kategori_kategori_gyldig_for
lagret_sak
lagret_sak_treff
lagret_soek_hit
lagret_sok
lagret_sok_treff
møtedokumentregistrering_dokumentbeskrivelse
møtesaksregistrering_dokumentbeskrivelse
shedlock
tilbakemelding
utredning_utredningsdokument
vedtak_vedtaksdokument
 */
