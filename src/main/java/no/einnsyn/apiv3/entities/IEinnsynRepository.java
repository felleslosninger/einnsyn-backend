package no.einnsyn.apiv3.entities;

import org.springframework.data.repository.CrudRepository;

public interface IEinnsynRepository<T, K> extends CrudRepository<T, K> {

  public Boolean existsById(String id);

  public T findById(String id);

  public T findByExternalId(String externalId);

  public void deleteById(String id);

  public T saveAndFlush(T saksmappe);

}
