package no.einnsyn.backend.entities.moetesak;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import no.einnsyn.backend.common.indexable.IndexableRepository;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import no.einnsyn.backend.entities.moetemappe.models.Moetemappe;
import no.einnsyn.backend.entities.moetesak.models.Moetesak;
import no.einnsyn.backend.entities.registrering.RegistreringRepository;
import no.einnsyn.backend.entities.utredning.models.Utredning;
import no.einnsyn.backend.entities.vedtak.models.Vedtak;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface MoetesakRepository
    extends RegistreringRepository<Moetesak>, IndexableRepository<Moetesak> {
  @Query(
      """
      SELECT o FROM Moetesak o
      WHERE moetemappe = :moetemappe
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Slice<Moetesak> paginateAsc(Moetemappe moetemappe, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Moetesak o
      WHERE moetemappe = :moetemappe
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Slice<Moetesak> paginateDesc(Moetemappe moetemappe, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Moetesak o
      WHERE utvalgObjekt = :utvalgObjekt
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Slice<Moetesak> paginateAsc(Enhet utvalgObjekt, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Moetesak o
      WHERE utvalgObjekt = :utvalgObjekt
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Slice<Moetesak> paginateDesc(Enhet utvalgObjekt, String pivot, Pageable pageable);

  @Query(
      """
      SELECT id FROM Moetesak
      WHERE utvalgObjekt = :utvalgObjekt
      ORDER BY id DESC
      """)
  Stream<String> streamIdByUtvalgObjekt(Enhet utvalgObjekt);

  @Query(
      """
      SELECT COUNT(m) FROM Moetesak m
      JOIN m.dokumentbeskrivelse d
      WHERE d = :dokumentbeskrivelse
      """)
  int countByDokumentbeskrivelse(Dokumentbeskrivelse dokumentbeskrivelse);

  @Query("SELECT m FROM Moetesak m JOIN m.dokumentbeskrivelse d WHERE d = :dokumentbeskrivelse")
  List<Moetesak> findByDokumentbeskrivelse(Dokumentbeskrivelse dokumentbeskrivelse);

  Moetesak findByUtredning(Utredning utredning);

  Moetesak findByVedtak(Vedtak vedtak);

  @Query(
      value =
          """
          SELECT _id FROM møtesaksregistrering WHERE last_indexed IS NULL
          UNION ALL
          SELECT _id FROM møtesaksregistrering WHERE last_indexed < _updated
          UNION ALL
          SELECT _id FROM møtesaksregistrering WHERE last_indexed < :schemaVersion
          UNION ALL
          SELECT _id FROM møtesaksregistrering WHERE (
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
          LEFT JOIN møtesaksregistrering AS t ON t._id = ids._id
          WHERE t._id IS NULL
          """,
      nativeQuery = true)
  @Transactional(readOnly = true)
  List<String> findNonExistingIds(String[] ids);
}
