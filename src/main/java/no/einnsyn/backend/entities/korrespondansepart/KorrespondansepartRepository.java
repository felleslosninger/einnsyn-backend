package no.einnsyn.backend.entities.korrespondansepart;

import no.einnsyn.backend.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.backend.entities.journalpost.models.Journalpost;
import no.einnsyn.backend.entities.korrespondansepart.models.Korrespondansepart;
import no.einnsyn.backend.entities.moetedokument.models.Moetedokument;
import no.einnsyn.backend.entities.moetesak.models.Moetesak;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;

public interface KorrespondansepartRepository extends ArkivBaseRepository<Korrespondansepart> {
  @Query(
      """
      SELECT o FROM Korrespondansepart o
      WHERE parentJournalpost = :journalpost
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Slice<Korrespondansepart> paginateAsc(Journalpost journalpost, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Korrespondansepart o
      WHERE parentJournalpost = :journalpost
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Slice<Korrespondansepart> paginateDesc(Journalpost journalpost, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Korrespondansepart o
      WHERE parentMoetedokument = :moetedokument
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Slice<Korrespondansepart> paginateAsc(
      Moetedokument moetedokument, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Korrespondansepart o
      WHERE parentMoetedokument = :moetedokument
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Slice<Korrespondansepart> paginateDesc(
      Moetedokument moetedokument, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Korrespondansepart o
      WHERE parentMoetesak = :moetesak
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Slice<Korrespondansepart> paginateAsc(Moetesak moetesak, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Korrespondansepart o
      WHERE parentMoetesak = :moetesak
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Slice<Korrespondansepart> paginateDesc(Moetesak moetesak, String pivot, Pageable pageable);
}
