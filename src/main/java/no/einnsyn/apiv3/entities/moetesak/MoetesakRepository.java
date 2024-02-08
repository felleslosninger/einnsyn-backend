package no.einnsyn.apiv3.entities.moetesak;

import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.moetemappe.models.Moetemappe;
import no.einnsyn.apiv3.entities.moetesak.models.Moetesak;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface MoetesakRepository extends ArkivBaseRepository<Moetesak> {
  @Query(
      "SELECT o FROM Moetesak o WHERE o.moetemappe = :moetemappe AND (:pivot IS NULL OR o.id >="
          + " :pivot) ORDER BY o.id ASC")
  Page<Moetesak> paginateAsc(Moetemappe moetemappe, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Moetesak o WHERE o.moetemappe = :moetemappe AND (:pivot IS NULL OR o.id <="
          + " :pivot) ORDER BY o.id DESC")
  Page<Moetesak> paginateDesc(Moetemappe moetemappe, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Moetesak o WHERE o.administrativEnhetObjekt = :administrativEnhetObjekt AND"
          + " (:pivot IS NULL OR o.id >= :pivot) ORDER BY o.id ASC")
  Page<Moetesak> paginateAsc(Enhet administrativEnhetObjekt, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Moetesak o WHERE o.administrativEnhetObjekt = :administrativEnhetObjekt AND"
          + " (:pivot IS NULL OR o.id <= :pivot) ORDER BY o.id DESC")
  Page<Moetesak> paginateDesc(Enhet administrativEnhetObjekt, String pivot, Pageable pageable);
}
