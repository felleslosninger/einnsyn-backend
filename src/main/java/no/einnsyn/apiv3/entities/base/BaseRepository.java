package no.einnsyn.apiv3.entities.base;

import no.einnsyn.apiv3.entities.base.models.Base;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public abstract interface BaseRepository<T extends Base> extends CrudRepository<T, String> {

  public T findByExternalId(String externalId);

  public boolean existsByExternalId(String externalId);

  public void delete(T object);

  public void deleteById(String id);

  public T saveAndFlush(T object);

  public Page<T> findAllByOrderByIdDesc(Pageable pageable);

  public Page<T> findByIdGreaterThanOrderByIdDesc(String id, Pageable pageable);

  public Page<T> findByIdLessThanOrderByIdDesc(String id, Pageable pageable);
}
