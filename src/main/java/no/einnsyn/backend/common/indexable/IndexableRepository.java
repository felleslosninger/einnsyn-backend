package no.einnsyn.backend.common.indexable;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@NoRepositoryBean
public interface IndexableRepository<T> extends CrudRepository<T, String> {

  /**
   * Advance `lastIndexed` to the given timestamp, but never move it backwards.
   *
   * <p>The same row can be indexed concurrently, e.g. by request-end indexing and by an async
   * sender that also indexes after updating the row. Each indexer reads the row, computes its own
   * `lastIndexed` and writes it in a separate transaction, so without this guard a slower indexer
   * holding an older timestamp could overwrite a newer one. That would leave `lastIndexed` behind
   * `_updated` and make the row look outdated even though the newest version was indexed.
   *
   * @param id ID of the row to update
   * @param timestamp the `lastIndexed` value to advance to
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Modifying
  @Query(
      """
      UPDATE #{#entityName} e
      SET e.lastIndexed = :timestamp
      WHERE e.id = :id
      AND (e.lastIndexed IS NULL OR e.lastIndexed < :timestamp)
      """)
  void updateLastIndexed(String id, Instant timestamp);

  /**
   * Bulk variant of {@link #updateLastIndexed(String, Instant)}, with the same guarantee that
   * `lastIndexed` never moves backwards.
   *
   * @param ids IDs of the rows to update
   * @param timestamp the `lastIndexed` value to advance to
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Modifying
  @Query(
      """
      UPDATE #{#entityName} e
      SET e.lastIndexed = :timestamp
      WHERE e.id IN :ids
      AND (e.lastIndexed IS NULL OR e.lastIndexed < :timestamp)
      """)
  void updateLastIndexed(List<String> ids, Instant timestamp);

  abstract Stream<String> streamUnIndexed(Instant schemaVersion);

  abstract List<String> findNonExistingIds(String[] ids);
}
