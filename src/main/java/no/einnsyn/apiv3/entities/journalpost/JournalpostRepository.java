package no.einnsyn.apiv3.entities.journalpost;

import java.util.List;
import no.einnsyn.apiv3.common.indexable.IndexableRepository;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.registrering.RegistreringRepository;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;
import no.einnsyn.apiv3.entities.skjerming.models.Skjerming;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface JournalpostRepository
    extends RegistreringRepository<Journalpost>, IndexableRepository<Journalpost> {

  @Query(
      "SELECT o FROM Journalpost o WHERE o.saksmappe = :saksmappe AND (:pivot IS NULL OR o.id >="
          + " :pivot) ORDER BY o.id ASC")
  Page<Journalpost> paginateAsc(Saksmappe saksmappe, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Journalpost o WHERE o.saksmappe = :saksmappe AND (:pivot IS NULL OR o.id <="
          + " :pivot) ORDER BY o.id DESC")
  Page<Journalpost> paginateDesc(Saksmappe saksmappe, String pivot, Pageable pageable);

  @Query(
      "SELECT COUNT(j) FROM Journalpost j JOIN j.dokumentbeskrivelse d WHERE d ="
          + " :dokumentbeskrivelse")
  int countByDokumentbeskrivelse(Dokumentbeskrivelse dokumentbeskrivelse);

  @Query("SELECT j FROM Journalpost j JOIN j.dokumentbeskrivelse d WHERE d = :dokumentbeskrivelse")
  List<Journalpost> findByDokumentbeskrivelse(Dokumentbeskrivelse dokumentbeskrivelse);

  List<Journalpost> findBySkjerming(Skjerming skjerming);

  boolean existsBySkjerming(Skjerming skjerming);
}
