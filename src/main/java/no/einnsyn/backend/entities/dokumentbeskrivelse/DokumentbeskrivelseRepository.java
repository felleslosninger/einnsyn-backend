package no.einnsyn.backend.entities.dokumentbeskrivelse;

import no.einnsyn.backend.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.backend.entities.journalpost.models.Journalpost;
import no.einnsyn.backend.entities.moetedokument.models.Moetedokument;
import no.einnsyn.backend.entities.moetesak.models.Moetesak;
import no.einnsyn.backend.entities.utredning.models.Utredning;
import no.einnsyn.backend.entities.vedtak.models.Vedtak;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;

public interface DokumentbeskrivelseRepository extends ArkivBaseRepository<Dokumentbeskrivelse> {

  @Query(
      """
      SELECT d FROM Journalpost j
      JOIN j.dokumentbeskrivelse d
      WHERE j = :journalpost
      AND d.id >= COALESCE(:pivot, d.id)
      ORDER BY d.id ASC
      """)
  Slice<Dokumentbeskrivelse> paginateAsc(Journalpost journalpost, String pivot, Pageable pageable);

  @Query(
      """
      SELECT d FROM Journalpost j
      JOIN j.dokumentbeskrivelse d
      WHERE j = :journalpost
      AND d.id <= COALESCE(:pivot, d.id)
      ORDER BY d.id DESC
      """)
  Slice<Dokumentbeskrivelse> paginateDesc(Journalpost journalpost, String pivot, Pageable pageable);

  @Query(
      """
      SELECT d FROM Moetesak m
      JOIN m.dokumentbeskrivelse d
      WHERE m = :moetesak
      AND d.id >= COALESCE(:pivot, d.id)
      ORDER BY d.id ASC
      """)
  Slice<Dokumentbeskrivelse> paginateAsc(Moetesak moetesak, String pivot, Pageable pageable);

  @Query(
      """
      SELECT d FROM Moetesak m
      JOIN m.dokumentbeskrivelse d
      WHERE m = :moetesak
      AND d.id <= COALESCE(:pivot, d.id)
      ORDER BY d.id DESC
      """)
  Slice<Dokumentbeskrivelse> paginateDesc(Moetesak moetesak, String pivot, Pageable pageable);

  @Query(
      """
      SELECT d FROM Moetedokument m
      JOIN m.dokumentbeskrivelse d
      WHERE m = :moetedokument
      AND d.id >= COALESCE(:pivot, d.id)
      ORDER BY d.id ASC
      """)
  Slice<Dokumentbeskrivelse> paginateAsc(
      Moetedokument moetedokument, String pivot, Pageable pageable);

  @Query(
      """
      SELECT d FROM Moetedokument m
      JOIN m.dokumentbeskrivelse d
      WHERE m = :moetedokument
      AND d.id <= COALESCE(:pivot, d.id)
      ORDER BY d.id DESC
      """)
  Slice<Dokumentbeskrivelse> paginateDesc(
      Moetedokument moetedokument, String pivot, Pageable pageable);

  @Query(
      """
      SELECT d FROM Utredning u
      JOIN u.utredningsdokument d
      WHERE u = :utredning
      AND d.id >= COALESCE(:pivot, d.id)
      ORDER BY d.id ASC
      """)
  Slice<Dokumentbeskrivelse> paginateAsc(Utredning utredning, String pivot, Pageable pageable);

  @Query(
      """
      SELECT d FROM Utredning u
      JOIN u.utredningsdokument d
      WHERE u = :utredning
      AND d.id <= COALESCE(:pivot, d.id)
      ORDER BY d.id DESC
      """)
  Slice<Dokumentbeskrivelse> paginateDesc(Utredning utredning, String pivot, Pageable pageable);

  @Query(
      """
      SELECT d FROM Vedtak v
      JOIN v.vedtaksdokument d
      WHERE v = :vedtak
      AND d.id >= COALESCE(:pivot, d.id)
      ORDER BY d.id ASC
      """)
  Slice<Dokumentbeskrivelse> paginateAsc(Vedtak vedtak, String pivot, Pageable pageable);

  @Query(
      """
      SELECT d FROM Vedtak v
      JOIN v.vedtaksdokument d
      WHERE v = :vedtak
      AND d.id <= COALESCE(:pivot, d.id)
      ORDER BY d.id DESC
      """)
  Slice<Dokumentbeskrivelse> paginateDesc(Vedtak vedtak, String pivot, Pageable pageable);
}
