package no.einnsyn.apiv3.common.indexable;

import java.time.Instant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;

@NoRepositoryBean
public interface IndexableRepository<T> extends CrudRepository<T, String> {

  @Transactional
  @Modifying
  @Query("UPDATE #{#entityName} e SET e.lastIndexed = :lastIndexed WHERE e.id = :id")
  void updateLastIndexed(String id, Instant lastIndexed);
}
