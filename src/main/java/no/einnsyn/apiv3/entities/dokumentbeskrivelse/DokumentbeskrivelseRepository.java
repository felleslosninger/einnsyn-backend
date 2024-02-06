package no.einnsyn.apiv3.entities.dokumentbeskrivelse;

import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface DokumentbeskrivelseRepository extends ArkivBaseRepository<Dokumentbeskrivelse> {

  @Query(
      "SELECT d FROM Dokumentbeskrivelse d JOIN d.journalpost j WHERE j = :journalpost AND (:pivot"
          + " IS NULL OR d.id >= :pivot) ORDER BY d.id ASC")
  Page<Dokumentbeskrivelse> paginateAsc(Journalpost journalpost, String pivot, Pageable pageable);

  @Query(
      "SELECT d FROM Dokumentbeskrivelse d JOIN d.journalpost j WHERE j = :journalpost AND (:pivot"
          + " IS NULL OR d.id <= :pivot) ORDER BY d.id DESC")
  Page<Dokumentbeskrivelse> paginateDesc(Journalpost journalpost, String pivot, Pageable pageable);
}
