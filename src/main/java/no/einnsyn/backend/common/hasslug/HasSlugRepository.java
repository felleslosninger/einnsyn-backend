package no.einnsyn.backend.common.hasslug;

import no.einnsyn.backend.entities.base.BaseRepository;
import no.einnsyn.backend.entities.base.models.Base;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

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
   * Acquires an advisory lock for the given slug and checks if it exists in a single query. This
   * ensures a fresh transaction snapshot after acquiring the lock.
   *
   * @param tableName The table name to check (e.g., 'journalpost').
   * @param slug The slug to lock and check.
   * @return 1 if slug exists, 0 if not.
   */
  @Query(
      value =
          """
          SELECT CAST(
            CASE
              WHEN (SELECT pg_advisory_xact_lock(hashtext(:slug))) IS NOT NULL
              AND EXISTS(SELECT 1 FROM journalpost WHERE slug = :slug)
            THEN true
            ELSE false
            END
          AS boolean)
          """,
      nativeQuery = true)
  boolean acquireLockAndCheckSlugExists(String slug);
}
