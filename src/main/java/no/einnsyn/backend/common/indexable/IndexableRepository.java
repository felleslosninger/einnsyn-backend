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

  abstract Stream<String> streamUnIndexed(Instant schemaVersion);

  abstract List<String> findNonExistingIds(String[] ids);
}
