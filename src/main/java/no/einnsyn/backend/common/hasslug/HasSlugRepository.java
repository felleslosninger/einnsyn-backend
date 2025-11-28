package no.einnsyn.backend.common.hasslug;

import no.einnsyn.backend.entities.base.BaseRepository;
import no.einnsyn.backend.entities.base.models.Base;
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
}
