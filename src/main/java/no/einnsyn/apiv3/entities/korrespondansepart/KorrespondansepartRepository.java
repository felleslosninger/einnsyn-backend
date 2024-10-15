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
      "SELECT o FROM Korrespondansepart o WHERE parentJournalpost = :journalpost AND id >="
          + " COALESCE(:pivot, id) ORDER BY o.id ASC")
  Page<Korrespondansepart> paginateAsc(Journalpost journalpost, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Korrespondansepart o WHERE parentJournalpost = :journalpost AND id <="
          + " COALESCE(:pivot, id) ORDER BY id DESC")
  Page<Korrespondansepart> paginateDesc(Journalpost journalpost, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Korrespondansepart o WHERE parentMoetedokument = :moetedokument AND id >="
          + " COALESCE(:pivot, id) ORDER BY o.id ASC")
  Page<Korrespondansepart> paginateAsc(
      Moetedokument moetedokument, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Korrespondansepart o WHERE parentMoetedokument = :moetedokument AND id <="
          + " COALESCE(:pivot, id) ORDER BY o.id DESC")
  Page<Korrespondansepart> paginateDesc(
      Moetedokument moetedokument, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Korrespondansepart o WHERE parentMoetesak = :moetesak AND id >="
          + " COALESCE(:pivot, id) ORDER BY id ASC")
  Page<Korrespondansepart> paginateAsc(Moetesak moetesak, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Korrespondansepart o WHERE parentMoetesak = :moetesak AND id <="
          + " COALESCE(:pivot, id) ORDER BY o.id DESC")
  Page<Korrespondansepart> paginateDesc(Moetesak moetesak, String pivot, Pageable pageable);
}
