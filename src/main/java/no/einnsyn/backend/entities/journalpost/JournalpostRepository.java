package no.einnsyn.backend.entities.journalpost;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import no.einnsyn.backend.common.indexable.IndexableRepository;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.backend.entities.journalpost.models.Journalpost;
import no.einnsyn.backend.entities.registrering.RegistreringRepository;
import no.einnsyn.backend.entities.saksmappe.models.Saksmappe;
import no.einnsyn.backend.entities.skjerming.models.Skjerming;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface JournalpostRepository
    extends RegistreringRepository<Journalpost>, IndexableRepository<Journalpost> {

  @Query(
      """
      SELECT o FROM Journalpost o
      WHERE saksmappe = :saksmappe
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Slice<Journalpost> paginateAsc(Saksmappe saksmappe, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Journalpost o
      WHERE saksmappe = :saksmappe
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Slice<Journalpost> paginateDesc(Saksmappe saksmappe, String pivot, Pageable pageable);

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
      SELECT j.id FROM Journalpost j
      JOIN j.dokumentbeskrivelse db
      WHERE db.id = :dokumentbeskrivelseId
      ORDER BY j.id DESC
      """)
  Stream<String> streamIdByDokumentbeskrivelseId(String dokumentbeskrivelseId);

  @Query(
      """
      SELECT id FROM Journalpost
      WHERE saksmappe.id = :saksmappeId
      ORDER BY id DESC
      """)
  Stream<String> streamIdBySaksmappeId(String saksmappeId);

  @Query(
      """
      SELECT j.id FROM Journalpost j
      JOIN j.korrespondansepart k
      WHERE k.id = :korrespondansepartId
      """)
  String findIdByKorrespondansepartId(String korrespondansepartId);

  @Query(
      """
      SELECT j.id FROM Journalpost j
      JOIN j.skjerming s
      WHERE s.id = :skjermingId
      ORDER BY j.id DESC
      """)
  Stream<String> streamIdBySkjermingId(String skjermingId);

  boolean existsBySkjerming(Skjerming skjerming);

  @Query(
      value =
          """
          SELECT _id FROM journalpost WHERE last_indexed IS NULL
          UNION ALL
          SELECT _id FROM journalpost WHERE last_indexed < _updated
          UNION ALL
          SELECT _id FROM journalpost WHERE last_indexed < :schemaVersion
          UNION ALL
          SELECT _id FROM journalpost WHERE (
              _accessible_after <= NOW() AND
              _accessible_after > last_indexed
          )
          """,
      nativeQuery = true)
  @Transactional(readOnly = true)
  @Override
  Stream<String> streamUnIndexed(Instant schemaVersion);

  @Query(
      value =
          """
          WITH ids AS (SELECT unnest(cast(:ids AS text[])) AS _id)
          SELECT ids._id
          FROM ids
          LEFT JOIN journalpost AS t ON t._id = ids._id
          WHERE t._id IS NULL
          """,
      nativeQuery = true)
  @Transactional(readOnly = true)
  @Override
  List<String> findNonExistingIds(String[] ids);
}
