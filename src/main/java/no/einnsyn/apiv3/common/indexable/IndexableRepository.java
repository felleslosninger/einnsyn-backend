package no.einnsyn.apiv3.common.indexable;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;

@NoRepositoryBean
public interface IndexableRepository<T> extends CrudRepository<T, String> {

  @Transactional
  @Modifying
  @Query("UPDATE #{#entityName} e SET e.lastIndexed = :timestamp WHERE e.id = :id")
  void updateLastIndexed(String id, Instant timestamp);

  @Query(
      "SELECT e FROM #{#entityName} e WHERE e.lastIndexed < e.updated OR e.lastIndexed <"
          + " :schemaVersion ORDER BY e.id ASC")
  Stream<T> findUnIndexed(Instant schemaVersion);

  @Query(
      value = "SELECT id FROM (VALUES(:ids)) AS V(id) EXCEPT SELECT _id FROM #{#entityName}",
      nativeQuery = true)
  List<String> findNonExistingIds(List<String> ids);
}
