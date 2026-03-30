package no.einnsyn.backend.entities.downloadcount;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import no.einnsyn.backend.common.indexable.IndexableRepository;
import no.einnsyn.backend.entities.base.BaseRepository;
import no.einnsyn.backend.entities.downloadcount.models.DownloadCount;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface DownloadCountRepository
    extends BaseRepository<DownloadCount>, IndexableRepository<DownloadCount> {


  @Query("SELECT id FROM DownloadCount WHERE dokumentobjektId = :dokumentobjektId")
  Stream<String> streamIdByDokumentobjektId(String dokumentobjektId);

  @Query(
      value =
          """
          SELECT _id FROM dokumentobjekt_download_stat WHERE last_indexed IS NULL
          UNION ALL
          SELECT _id FROM dokumentobjekt_download_stat WHERE last_indexed < _updated
          UNION ALL
          SELECT _id FROM dokumentobjekt_download_stat WHERE last_indexed < :schemaVersion
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
          LEFT JOIN dokumentobjekt_download_stat AS t ON t._id = ids._id
          WHERE t._id IS NULL
          """,
      nativeQuery = true)
  @Transactional(readOnly = true)
  @Override
  List<String> findNonExistingIds(String[] ids);
}
