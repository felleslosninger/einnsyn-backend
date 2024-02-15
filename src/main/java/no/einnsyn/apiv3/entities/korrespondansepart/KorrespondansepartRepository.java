package no.einnsyn.apiv3.entities.korrespondansepart;

import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.korrespondansepart.models.Korrespondansepart;
import no.einnsyn.apiv3.entities.moetedokument.models.Moetedokument;
import no.einnsyn.apiv3.entities.moetesak.models.Moetesak;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface KorrespondansepartRepository extends ArkivBaseRepository<Korrespondansepart> {
  @Query(
      "SELECT o FROM Korrespondansepart o WHERE o.parentJournalpost = :journalpost AND (:pivot IS"
          + " NULL OR o.id >= :pivot) ORDER BY o.id ASC")
  Page<Korrespondansepart> paginateAsc(Journalpost journalpost, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Korrespondansepart o WHERE o.parentJournalpost = :journalpost AND (:pivot IS"
          + " NULL OR o.id <= :pivot) ORDER BY o.id DESC")
  Page<Korrespondansepart> paginateDesc(Journalpost journalpost, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Korrespondansepart o WHERE o.parentMoetedokument = :moetedokument AND (:pivot"
          + " IS NULL OR o.id >= :pivot) ORDER BY o.id ASC")
  Page<Korrespondansepart> paginateAsc(
      Moetedokument moetedokument, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Korrespondansepart o WHERE o.parentMoetedokument = :moetedokument AND (:pivot"
          + " IS NULL OR o.id <= :pivot) ORDER BY o.id DESC")
  Page<Korrespondansepart> paginateDesc(
      Moetedokument moetedokument, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Korrespondansepart o WHERE o.parentMoetesak = :moetesak AND (:pivot"
          + " IS NULL OR o.id >= :pivot) ORDER BY o.id ASC")
  Page<Korrespondansepart> paginateAsc(Moetesak moetesak, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Korrespondansepart o WHERE o.parentMoetesak = :moetesak AND (:pivot"
          + " IS NULL OR o.id <= :pivot) ORDER BY o.id DESC")
  Page<Korrespondansepart> paginateDesc(Moetesak moetesak, String pivot, Pageable pageable);
}
