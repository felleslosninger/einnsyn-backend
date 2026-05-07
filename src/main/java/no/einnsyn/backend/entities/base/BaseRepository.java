package no.einnsyn.backend.entities.base;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import no.einnsyn.backend.entities.base.models.Base;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;

@NoRepositoryBean
public interface BaseRepository<T extends Base> extends CrudRepository<T, String> {

  List<T> findByIdIn(List<String> ids);

  T findByExternalId(String externalId);

  List<T> findByExternalIdIn(List<String> externalIds);

  void delete(@NotNull T object);

  void deleteById(@NotNull String id);

  T saveAndFlush(T object);

  @Transactional
  @Modifying
  @Query("UPDATE #{#entityName} e SET e.updated = CURRENT_TIMESTAMP WHERE e.id = :id")
  void touchUpdated(String id);

  Slice<T> findAllByOrderByIdDesc(Pageable pageable);

  Slice<T> findAllByOrderByIdAsc(Pageable pageable);

  Slice<T> findByIdLessThanEqualOrderByIdDesc(String id, Pageable pageable);

  Slice<T> findByIdGreaterThanEqualOrderByIdAsc(String id, Pageable pageable);
}
