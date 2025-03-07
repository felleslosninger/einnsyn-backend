package no.einnsyn.backend.common.paginators;

import java.util.function.BiFunction;
import lombok.Getter;
import no.einnsyn.backend.entities.base.models.Base;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

/**
 * This class encapsulates two {@link BiFunction} instances that define the pagination logic for
 * ascending and descending order based on a given property and {@link PageRequest}. This makes it
 * easy for subclasses to define custom logic for pagination, i.e. by filtering by a parent entity.
 *
 * @param <T> the type of entities to be paginated, extending from {@link Base}
 */
@Getter
public class Paginators<T extends Base> {

  private final BiFunction<String, PageRequest, Slice<T>> ascFunction;
  private final BiFunction<String, PageRequest, Slice<T>> descFunction;

  public Paginators(
      BiFunction<String, PageRequest, Slice<T>> asc,
      BiFunction<String, PageRequest, Slice<T>> desc) {
    this.ascFunction = asc;
    this.descFunction = desc;
  }

  /**
   * Gets a Page of entities in ascending order, starting from `pivot`.
   *
   * @param pivot ID of the object to start the pagination from
   * @param pageRequest
   * @return a Page of entities
   */
  public Slice<T> getAsc(String pivot, PageRequest pageRequest) {
    return ascFunction.apply(pivot, pageRequest);
  }

  /**
   * Gets a Page of entities in descending order, starting from `pivot`.
   *
   * @param pivot ID of the object to start the pagination from
   * @param pageRequest
   * @return a Page of entities
   */
  public Slice<T> getDesc(String pivot, PageRequest pageRequest) {
    return descFunction.apply(pivot, pageRequest);
  }
}
