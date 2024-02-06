package no.einnsyn.apiv3.entities.enhet;

import no.einnsyn.apiv3.entities.base.BaseRepository;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface EnhetRepository extends BaseRepository<Enhet> {

  @Query(
      "SELECT o FROM Enhet o WHERE o.parent = :parent AND (:pivot IS NULL OR o.id >= :pivot)"
          + " ORDER BY o.id ASC")
  Page<Enhet> paginateAsc(Enhet parent, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Enhet o WHERE o.parent = :parent AND (:pivot IS NULL OR o.id <= :pivot)"
          + " ORDER BY o.id DESC")
  Page<Enhet> paginateDesc(Enhet parent, String pivot, Pageable pageable);
}
