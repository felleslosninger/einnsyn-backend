package no.einnsyn.apiv3.entities.dokumentbeskrivelse;

import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface DokumentbeskrivelseRepository extends ArkivBaseRepository<Dokumentbeskrivelse> {

  @Query(
      "SELECT d FROM Dokumentbeskrivelse d JOIN d.journalpost j WHERE j = :journalpost ORDER BY"
          + " d.id DESC")
  Page<Dokumentbeskrivelse> findByJournalpostOrderByIdDesc(
      Journalpost journalpost, Pageable pageable);

  @Query(
      "SELECT d FROM Dokumentbeskrivelse d JOIN d.journalpost j WHERE j = :journalpost ORDER BY"
          + " d.id ASC")
  Page<Dokumentbeskrivelse> findByJournalpostOrderByIdAsc(
      Journalpost journalpost, Pageable pageable);

  @Query(
      "SELECT d FROM Dokumentbeskrivelse d JOIN d.journalpost j WHERE j = :journalpost AND d.id"
          + " <= :id ORDER BY d.id DESC")
  Page<Dokumentbeskrivelse> findByJournalpostAndIdLessThanEqualOrderByIdDesc(
      Journalpost journalpost, String id, Pageable pageable);

  @Query(
      "SELECT d FROM Dokumentbeskrivelse d JOIN d.journalpost j WHERE j = :journalpost AND d.id"
          + " >= :id ORDER BY d.id ASC")
  Page<Dokumentbeskrivelse> findByJournalpostAndIdGreaterThanEqualOrderByIdAsc(
      Journalpost journalpost, String id, Pageable pageable);
}
