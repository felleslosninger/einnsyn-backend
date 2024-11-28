package no.einnsyn.backend.entities.journalpost;

import java.util.List;
import no.einnsyn.backend.common.indexable.IndexableRepository;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.backend.entities.journalpost.models.Journalpost;
import no.einnsyn.backend.entities.registrering.RegistreringRepository;
import no.einnsyn.backend.entities.saksmappe.models.Saksmappe;
import no.einnsyn.backend.entities.skjerming.models.Skjerming;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface JournalpostRepository
    extends RegistreringRepository<Journalpost>, IndexableRepository<Journalpost> {

  @Query(
      """
      SELECT o FROM Journalpost o
      WHERE saksmappe = :saksmappe
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Page<Journalpost> paginateAsc(Saksmappe saksmappe, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Journalpost o
      WHERE saksmappe = :saksmappe
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Page<Journalpost> paginateDesc(Saksmappe saksmappe, String pivot, Pageable pageable);

  @Query(
      """
      SELECT COUNT(j)
      FROM Journalpost j
      JOIN j.dokumentbeskrivelse d
      WHERE d = :dokumentbeskrivelse
      """)
  int countByDokumentbeskrivelse(Dokumentbeskrivelse dokumentbeskrivelse);

  @Query(
      """
      SELECT j FROM Journalpost j
      JOIN j.dokumentbeskrivelse d
      WHERE d = :dokumentbeskrivelse
      """)
  List<Journalpost> findByDokumentbeskrivelse(Dokumentbeskrivelse dokumentbeskrivelse);

  List<Journalpost> findBySkjerming(Skjerming skjerming);

  boolean existsBySkjerming(Skjerming skjerming);
}
