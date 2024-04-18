package no.einnsyn.apiv3.entities.arkiv;

import java.util.stream.Stream;
import no.einnsyn.apiv3.entities.arkiv.models.Arkiv;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface ArkivRepository extends ArkivBaseRepository<Arkiv> {
  @Query(
      "SELECT o FROM Arkiv o WHERE o.parent = :parent AND (:pivot IS NULL OR o.id >= :pivot)"
          + " ORDER BY o.id ASC")
  Page<Arkiv> paginateAsc(Arkiv parent, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Arkiv o WHERE o.parent = :parent AND (:pivot IS NULL OR o.id <= :pivot)"
          + " ORDER BY o.id DESC")
  Page<Arkiv> paginateDesc(Arkiv parent, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Arkiv o WHERE o.journalenhet = :enhet AND (:pivot IS NULL OR o.id >= :pivot)"
          + " ORDER BY o.id ASC")
  Page<Arkiv> paginateAsc(Enhet enhet, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Arkiv o WHERE o.journalenhet = :enhet AND (:pivot IS NULL OR o.id <= :pivot)"
          + " ORDER BY o.id DESC")
  Page<Arkiv> paginateDesc(Enhet enhet, String pivot, Pageable pageable);

  Stream<Arkiv> findAllByParent(Arkiv parent);
}
