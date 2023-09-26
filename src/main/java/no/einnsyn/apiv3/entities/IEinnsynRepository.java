package no.einnsyn.apiv3.entities;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

public interface IEinnsynRepository<T, K> extends CrudRepository<T, K> {

  public Boolean existsById(String id);

  public T findById(String id);

  public T findByExternalId(String externalId);

  public void deleteById(String id);

  public T saveAndFlush(T saksmappe);

  public Page<T> findAllByOrderByIdDesc(Pageable pageable);

  // This will work when using UUIDv7, since they are sortable by time
  public Page<T> findByIdGreaterThanOrderByIdDesc(String id, Pageable pageable);

  public Page<T> findByIdLessThanOrderByIdDesc(String id, Pageable pageable);

}
