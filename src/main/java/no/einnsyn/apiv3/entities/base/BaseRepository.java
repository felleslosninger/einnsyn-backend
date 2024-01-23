package no.einnsyn.apiv3.entities.base;

import no.einnsyn.apiv3.entities.base.models.Base;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseRepository<T extends Base> extends CrudRepository<T, String> {

  T findByExternalId(String externalId);

  boolean existsByExternalId(String externalId);

  void delete(T object);

  void deleteById(String id);

  T saveAndFlush(T object);

  Page<T> findAllByOrderByIdDesc(Pageable pageable);

  Page<T> findAllByOrderByIdAsc(Pageable pageable);

  Page<T> findByIdLessThanEqualOrderByIdDesc(String id, Pageable pageable);

  Page<T> findByIdGreaterThanEqualOrderByIdAsc(String id, Pageable pageable);
}
