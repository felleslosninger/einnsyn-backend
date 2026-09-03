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

  /**
   * Atomically create or increment the hourly download bucket for a Dokumentobjekt, and return the
   * id of the affected row.
   *
   * <p>This runs on the download hot path, where concurrent downloads of the same Dokumentobjekt
   * within the same hour are expected. A read-modify-write through JPA would let those requests
   * read the same count and write the same incremented value, losing downloads and failing requests
   * on optimistic lock conflicts. The unique index on (dokumentobjekt__id, bucket_start) instead
   * turns the conflict into a single atomic increment, so no retry is needed.
   *
   * <p>A native insert bypasses JPA's lifecycle callbacks, so this query fills in by hand what
   * {@link no.einnsyn.backend.entities.base.models.Base}'s {@code @PrePersist} used to set: {@code
   * _id}, {@code _created}, {@code _updated} and {@code _accessible_after}. The latter is stamped
   * with the creation time exactly as {@code @PrePersist} does, since a download bucket carries no
   * embargo of its own; it only exists once the Dokumentobjekt has actually been downloaded. {@code
   * lock_version} is bumped so concurrently loaded JPA entities still detect the change.
   *
   * <p>{@code _updated} deliberately uses {@code clock_timestamp()} rather than {@code now()}:
   * {@code now()} is fixed at transaction start, so a transaction that began earlier but reaches
   * this row later would stamp {@code _updated} with an older value than the update it follows.
   * That would let {@code _updated} move backwards while the count still changes, and the stale
   * check in {@link #streamUnIndexed} ({@code last_indexed < _updated}) would then never select the
   * row again — leaving the indexed count permanently behind the database. {@code
   * _accessible_after} keeps {@code now()} so the row is never stamped as accessible in the future
   * relative to its own transaction.
   *
   * @param id id to use if a new bucket is created
   * @param dokumentobjektId the dokumentobjekt being downloaded
   * @param bucketStart start of the hourly bucket
   * @return the id of the created or incremented bucket
   */
  @Query(
      value =
          """
          INSERT INTO dokumentobjekt_download_stat
            (_id, dokumentobjekt__id, bucket_start, download_count,
             _created, _updated, _accessible_after, lock_version)
          VALUES (:id, :dokumentobjektId, :bucketStart, 1,
                  now(), clock_timestamp(), now(), 0)
          ON CONFLICT (dokumentobjekt__id, bucket_start) DO UPDATE
          SET download_count = dokumentobjekt_download_stat.download_count + 1,
              _updated = clock_timestamp(),
              lock_version = dokumentobjekt_download_stat.lock_version + 1
          RETURNING _id
          """,
      nativeQuery = true)
  @Transactional
  String incrementCount(String id, String dokumentobjektId, Instant bucketStart);

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
