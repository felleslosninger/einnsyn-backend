package no.einnsyn.apiv3.entities.moetesak;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import no.einnsyn.apiv3.common.indexable.IndexableRepository;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.moetemappe.models.Moetemappe;
import no.einnsyn.apiv3.entities.moetesak.models.Moetesak;
import no.einnsyn.apiv3.entities.registrering.RegistreringRepository;
import no.einnsyn.apiv3.entities.utredning.models.Utredning;
import no.einnsyn.apiv3.entities.vedtak.models.Vedtak;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface MoetesakRepository
    extends RegistreringRepository<Moetesak>, IndexableRepository<Moetesak> {
  @Query(
      "SELECT o FROM Moetesak o WHERE o.moetemappe = :moetemappe AND o.id >= COALESCE(:pivot, o.id)"
          + " ORDER BY o.id ASC")
  Page<Moetesak> paginateAsc(Moetemappe moetemappe, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Moetesak o WHERE o.moetemappe = :moetemappe AND o.id <= COALESCE(:pivot, o.id)"
          + " ORDER BY o.id DESC")
  Page<Moetesak> paginateDesc(Moetemappe moetemappe, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Moetesak o WHERE o.utvalgObjekt = :utvalgObjekt AND"
          + " o.id >= COALESCE(:pivot, o.id) ORDER BY o.id ASC")
  Page<Moetesak> paginateAsc(Enhet utvalgObjekt, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Moetesak o WHERE o.utvalgObjekt = :utvalgObjekt AND"
          + " o.id <= COALESCE(:pivot, o.id) ORDER BY o.id DESC")
  Page<Moetesak> paginateDesc(Enhet utvalgObjekt, String pivot, Pageable pageable);

  Stream<Moetesak> findAllByUtvalgObjekt(Enhet utvalgObjekt);

  @Query(
      "SELECT COUNT(m) FROM Moetesak m JOIN m.dokumentbeskrivelse d WHERE d = :dokumentbeskrivelse")
  int countByDokumentbeskrivelse(Dokumentbeskrivelse dokumentbeskrivelse);

  @Query("SELECT m FROM Moetesak m JOIN m.dokumentbeskrivelse d WHERE d = :dokumentbeskrivelse")
  List<Moetesak> findByDokumentbeskrivelse(Dokumentbeskrivelse dokumentbeskrivelse);

  Moetesak findByUtredning(Utredning utredning);

  Moetesak findByVedtak(Vedtak vedtak);

  @Query(
      value =
          ""
              + "SELECT * FROM møtesaksregistrering e WHERE e.last_indexed IS NULL "
              + "UNION ALL "
              + "SELECT * FROM møtesaksregistrering e WHERE e.last_indexed < e._updated "
              + "UNION ALL "
              + "SELECT * FROM møtesaksregistrering e WHERE e.last_indexed < :schemaVersion",
      nativeQuery = true)
  Stream<Moetesak> findUnIndexed(Instant schemaVersion);

  @Query(
      value =
          ""
              + "WITH ids AS (SELECT unnest(cast(:ids AS text[])) AS _id) "
              + "SELECT ids._id "
              + "FROM ids "
              + "LEFT JOIN møtesaksregistrering AS t ON t._id = ids._id "
              + "WHERE t._id IS NULL",
      nativeQuery = true)
  @Transactional(readOnly = true)
  List<String> findNonExistingIds(String[] ids);
}
