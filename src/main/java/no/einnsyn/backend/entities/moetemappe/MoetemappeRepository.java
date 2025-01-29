package no.einnsyn.backend.entities.moetemappe;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import no.einnsyn.backend.common.indexable.IndexableRepository;
import no.einnsyn.backend.entities.arkivdel.models.Arkivdel;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import no.einnsyn.backend.entities.klasse.models.Klasse;
import no.einnsyn.backend.entities.mappe.MappeRepository;
import no.einnsyn.backend.entities.moetemappe.models.Moetemappe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface MoetemappeRepository
    extends MappeRepository<Moetemappe>, IndexableRepository<Moetemappe> {

  @Query(
      """
      SELECT o FROM Moetemappe o
      WHERE parentArkivdel = :arkivdel
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Page<Moetemappe> paginateAsc(Arkivdel arkivdel, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Moetemappe o
      WHERE parentArkivdel = :arkivdel
      AND id <= COALESCE(:pivot, o.id)
      ORDER BY id DESC
      """)
  Page<Moetemappe> paginateDesc(Arkivdel arkivdel, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Moetemappe o
      WHERE parentKlasse = :klasse
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Page<Moetemappe> paginateAsc(Klasse klasse, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Moetemappe o
      WHERE parentKlasse = :klasse
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Page<Moetemappe> paginateDesc(Klasse klasse, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Moetemappe o
      WHERE utvalgObjekt = :utvalgObjekt
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Page<Moetemappe> paginateAsc(Enhet utvalgObjekt, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Moetemappe o
      WHERE utvalgObjekt = :utvalgObjekt
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Page<Moetemappe> paginateDesc(Enhet utvalgObjekt, String pivot, Pageable pageable);

  Stream<Moetemappe> findAllByUtvalgObjekt(Enhet administrativEnhetObjekt);

  @Query(
      value =
          """
          SELECT * FROM møtemappe e WHERE e.last_indexed IS NULL
          UNION ALL
          SELECT * FROM møtemappe e WHERE e.last_indexed < e._updated
          UNION ALL
          SELECT * FROM møtemappe e WHERE e.last_indexed < :schemaVersion
          """,
      nativeQuery = true)
  Stream<Moetemappe> findUnIndexed(Instant schemaVersion);

  @Query(
      value =
          """
          WITH ids AS (SELECT unnest(cast(:ids AS text[])) AS _id)
          SELECT ids._id
          FROM ids
          LEFT JOIN møtemappe AS t ON t._id = ids._id
          WHERE t._id IS NULL
          """,
      nativeQuery = true)
  @Transactional(readOnly = true)
  List<String> findNonExistingIds(String[] ids);
}
