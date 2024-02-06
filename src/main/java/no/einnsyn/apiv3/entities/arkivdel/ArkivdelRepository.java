package no.einnsyn.apiv3.entities.arkivdel;

import no.einnsyn.apiv3.entities.arkiv.models.Arkiv;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.apiv3.entities.arkivdel.models.Arkivdel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface ArkivdelRepository extends ArkivBaseRepository<Arkivdel> {
  @Query(
      "SELECT o FROM Arkivdel o WHERE o.arkiv = :arkiv AND (:pivot IS NULL OR o.id >= :pivot)"
          + " ORDER BY o.id ASC")
  Page<Arkivdel> paginateAsc(Arkiv arkiv, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Arkivdel o WHERE o.arkiv = :arkiv AND (:pivot IS NULL OR o.id <= :pivot)"
          + " ORDER BY o.id DESC")
  Page<Arkivdel> paginateDesc(Arkiv arkiv, String pivot, Pageable pageable);
}
