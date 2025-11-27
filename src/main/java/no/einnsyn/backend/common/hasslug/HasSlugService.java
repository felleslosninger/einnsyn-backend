package no.einnsyn.backend.common.hasslug;

import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.entities.base.BaseRepository;
import no.einnsyn.backend.entities.base.models.Base;
import no.einnsyn.backend.utils.SlugGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Service for entities that have a slug.
 *
 * @param <O> The entity type that extends Base and implements HasSlug.
 * @param <S> The service type that extends this service.
 */
public interface HasSlugService<O extends Base & HasSlug, S extends HasSlugService<O, S>> {

  Logger log = LoggerFactory.getLogger(HasSlugService.class);

  /**
   * Gets the repository for the entity.
   *
   * @return The repository.
   */
  BaseRepository<O> getRepository();

  /**
   * Gets the proxy for this service. This is needed to call transactional methods from within the
   * same service.
   *
   * @return The proxy.
   */
  S getProxy();

  /**
   * Gets the base string for generating the slug.
   *
   * @param object The object to generate the slug for.
   * @return The base string for the slug.
   */
  String getSlugBase(O object);

  /**
   * Sets the slug for an entity in a new transaction.
   *
   * @param id The id of the entity.
   * @param slugBase The base string for the slug.
   * @param attempt The attempt number for generating the slug.
   * @return The updated entity.
   * @throws EInnsynException If an error occurs.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  default O setSlugInNewTransaction(String id, String slugBase, int attempt) {
    var object = getRepository().findById(id).orElse(null);
    var slug = SlugGenerator.generate(slugBase, attempt > 0);
    object.setSlug(slug);
    return getRepository().saveAndFlush(object);
  }

  /**
   * Schedules a slug update after the current transaction commits. This is done to avoid affecting
   * the current transaction with slug uniqueness checks.
   *
   * @param object The object to update.
   * @param slugBase The base string for the slug.
   * @return The object.
   * @throws EInnsynException If an error occurs.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  default O scheduleSlugUpdate(O object, String slugBase) throws EInnsynException {
    if (object.getSlug() == null && slugBase != null) {

      // Set slug after transaction commit to avoid affecting current transaction
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              for (int attempt = 0; attempt < 5; attempt++) {
                try {
                  getProxy().setSlugInNewTransaction(object.getId(), slugBase, attempt);
                  return;
                } catch (Exception e) {
                  log.warn(
                      "Failed to set slug for {} on attempt {}. Retrying...",
                      object.getId(),
                      attempt + 1,
                      e);
                }
              }
              log.error("Failed to set slug for {} after multiple attempts.", object.getId());
            }
          });
    }
    return object;
  }
}
