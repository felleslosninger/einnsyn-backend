package no.einnsyn.backend.common.hasslug;

import no.einnsyn.backend.entities.base.BaseRepository;
import no.einnsyn.backend.entities.base.models.Base;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for entities that have a slug.
 *
 * @param <T> The entity type that extends Base.
 */
@NoRepositoryBean
public interface HasSlugRepository<T extends Base> extends BaseRepository<T> {
  /**
   * Finds an entity by its slug.
   *
   * @param slug The slug of the entity.
   * @return The entity with the given slug, or null if not found.
   */
  T findBySlug(String slug);

  /**
   * Acquires an advisory lock for the given slug.
   *
   * @param slug The slug to lock on.
   */
  @Query(value = "SELECT pg_advisory_xact_lock(hashtext(:slug))", nativeQuery = true)
  void acquireAdvisoryLock(String slug);

  /**
   * Counts entities by slug.
   *
   * @param slug The slug to count.
   * @return The number of entities with the given slug.
   */
  @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.slug = :slug")
  long countBySlug(String slug);

  /**
   * Count by slug in a new transaction.
   *
   * @param slug The slug to count.
   * @return The number of entities with the given slug.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.slug = :slug")
  long countBySlugInNewTransaction(String slug);
}
