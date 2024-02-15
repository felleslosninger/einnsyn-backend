package no.einnsyn.apiv3.entities.moetemappe;

import java.util.stream.Stream;
import no.einnsyn.apiv3.entities.arkiv.models.Arkiv;
import no.einnsyn.apiv3.entities.arkivdel.models.Arkivdel;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.klasse.models.Klasse;
import no.einnsyn.apiv3.entities.mappe.MappeRepository;
import no.einnsyn.apiv3.entities.moetemappe.models.Moetemappe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface MoetemappeRepository extends MappeRepository<Moetemappe> {

  @Query(
      "SELECT o FROM Moetemappe o WHERE o.parentArkiv = :arkiv AND (:pivot IS NULL OR o.id >="
          + " :pivot) ORDER BY o.id ASC")
  Page<Moetemappe> paginateAsc(Arkiv arkiv, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Moetemappe o WHERE o.parentArkiv = :arkiv AND (:pivot IS NULL OR o.id <="
          + " :pivot) ORDER BY o.id DESC")
  Page<Moetemappe> paginateDesc(Arkiv arkiv, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Moetemappe o WHERE o.parentArkivdel = :arkivdel AND (:pivot IS NULL OR o.id >="
          + " :pivot) ORDER BY o.id ASC")
  Page<Moetemappe> paginateAsc(Arkivdel arkivdel, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Moetemappe o WHERE o.parentArkivdel = :arkivdel AND (:pivot IS NULL OR o.id <="
          + " :pivot) ORDER BY o.id DESC")
  Page<Moetemappe> paginateDesc(Arkivdel arkivdel, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Moetemappe o WHERE o.parentKlasse = :klasse AND (:pivot IS NULL OR o.id >="
          + " :pivot) ORDER BY o.id ASC")
  Page<Moetemappe> paginateAsc(Klasse klasse, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Moetemappe o WHERE o.parentKlasse = :klasse AND (:pivot IS NULL OR o.id >="
          + " :pivot) ORDER BY o.id ASC")
  Page<Moetemappe> paginateDesc(Klasse klasse, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Moetemappe o WHERE o.utvalgObjekt = :utvalgObjekt AND (:pivot IS NULL OR o.id"
          + " >= :pivot) ORDER BY o.id ASC")
  Page<Moetemappe> paginateAsc(Enhet utvalgObjekt, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Moetemappe o WHERE o.utvalgObjekt = :utvalgObjekt AND (:pivot IS NULL OR o.id"
          + " >= :pivot) ORDER BY o.id ASC")
  Page<Moetemappe> paginateDesc(Enhet utvalgObjekt, String pivot, Pageable pageable);

  Stream<Moetemappe> findAllByUtvalgObjekt(Enhet administrativEnhetObjekt);
}
