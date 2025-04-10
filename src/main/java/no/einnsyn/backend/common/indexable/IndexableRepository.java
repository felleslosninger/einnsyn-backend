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

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Modifying
  @Query("UPDATE #{#entityName} e SET e.lastIndexed = :timestamp WHERE e.id = :id")
  void updateLastIndexed(String id, Instant timestamp);

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Modifying
  @Query("UPDATE #{#entityName} e SET e.lastIndexed = :timestamp WHERE e.id IN :ids")
  void updateLastIndexed(List<String> ids, Instant timestamp);

  @Query(
      value =
          """
          SELECT _id FROM #{#entityName} WHERE last_indexed IS NULL
          UNION ALL
          SELECT _id FROM #{#entityName} WHERE last_indexed < _updated
          UNION ALL
          SELECT _id FROM #{#entityName} WHERE last_indexed < :schemaVersion
          UNION ALL
          SELECT _id FROM #{#entityName} WHERE (
              _accessible_after <= NOW() AND
              _accessible_after > last_indexed
          )
          """,
      nativeQuery = true)
  @Transactional(readOnly = true)
  Stream<String> streamUnIndexed(Instant schemaVersion);

  @Query(
      value =
          """
          WITH ids AS (SELECT unnest(cast(:ids AS text[])) AS _id)
          SELECT ids._id
          FROM ids
          LEFT JOIN #{#entityName} AS t ON t._id = ids._id
          WHERE t._id IS NULL
          """,
      nativeQuery = true)
  @Transactional(readOnly = true)
  List<String> findNonExistingIds(String[] ids);
}
