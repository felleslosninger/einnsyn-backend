package no.einnsyn.backend.common.hasslug;

import jakarta.persistence.EntityManager;
import no.einnsyn.backend.entities.base.BaseRepository;
import no.einnsyn.backend.entities.base.models.Base;
import no.einnsyn.backend.utils.SlugGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
   * Gets the slug repository for the entity.
   *
   * @return The slug repository.
   */
  default HasSlugRepository<O> getSlugRepository() {
    return (HasSlugRepository<O>) getRepository();
  }

  /**
   * Gets the entity manager.
   *
   * @return The entity manager.
   */
  EntityManager getEntityManager();

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
   * Sets the slug for an entity. Uses PostgreSQL advisory locks to prevent race conditions. First
   * tries to use the base slug without a random suffix. If a conflict is detected, retries with a
   * random suffix until a unique slug is found.
   *
   * @param object The object to update.
   * @param slugBase The base string for the slug.
   * @return The object.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  default O setSlug(O object, String slugBase) {
    if (object.getSlug() == null && slugBase != null) {
      for (int attempt = 0; attempt < 10; attempt++) {
        // Try base slug first (without random suffix), then with suffixes on retries
        var slug = SlugGenerator.generate(slugBase, attempt > 0);

        // Acquire advisory lock and check if slug exists in a single query
        // This ensures we get a fresh transaction snapshot after acquiring the lock
        var slugExists = getSlugRepository().acquireLockAndCheckSlugExists(slug);

        if (!slugExists) {
          object.setSlug(slug);
          if (attempt > 0) {
            log.info(
                "Slug conflict resolved with random suffix for '{}' on attempt {}",
                slug,
                attempt + 1);
          }
          return object;
        } else {
          log.debug(
              "Slug '{}' already exists, retrying with random suffix (attempt {})",
              slug,
              attempt + 1);
        }
      }

      // Failed to find unique slug after 10 attempts - this should be extremely rare
      log.error("Failed to generate unique slug for object {} after 10 attempts", object.getId());
    }
    return object;
  }
}
