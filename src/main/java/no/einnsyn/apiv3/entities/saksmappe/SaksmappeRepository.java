package no.einnsyn.apiv3.entities.saksmappe;

import no.einnsyn.apiv3.entities.arkiv.models.Arkiv;
import no.einnsyn.apiv3.entities.arkivdel.models.Arkivdel;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.klasse.models.Klasse;
import no.einnsyn.apiv3.entities.mappe.MappeRepository;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface SaksmappeRepository extends MappeRepository<Saksmappe> {

  @Query(
      "SELECT o FROM Saksmappe o WHERE o.arkiv = :arkiv AND (:pivot IS NULL OR o.id >="
          + " :pivot) ORDER BY o.id ASC")
  Page<Saksmappe> paginateAsc(Arkiv arkiv, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Saksmappe o WHERE o.arkiv = :arkiv AND (:pivot IS NULL OR o.id <="
          + " :pivot) ORDER BY o.id DESC")
  Page<Saksmappe> paginateDesc(Arkiv arkiv, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Saksmappe o WHERE o.arkivdel = :arkivdel AND (:pivot IS NULL OR o.id >="
          + " :pivot) ORDER BY o.id ASC")
  Page<Saksmappe> paginateAsc(Arkivdel arkivdel, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Saksmappe o WHERE o.arkivdel = :arkivdel AND (:pivot IS NULL OR o.id <="
          + " :pivot) ORDER BY o.id DESC")
  Page<Saksmappe> paginateDesc(Arkivdel arkivdel, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Saksmappe o WHERE o.klasse = :klasse AND (:pivot IS NULL OR o.id >="
          + " :pivot) ORDER BY o.id ASC")
  Page<Saksmappe> paginateAsc(Klasse klasse, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Saksmappe o WHERE o.klasse = :klasse AND (:pivot IS NULL OR o.id >="
          + " :pivot) ORDER BY o.id ASC")
  Page<Saksmappe> paginateDesc(Klasse klasse, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Saksmappe o WHERE o.administrativEnhetObjekt = :administrativEnhetObjekt AND"
          + " (:pivot IS NULL OR o.id >= :pivot) ORDER BY o.id ASC")
  Page<Saksmappe> paginateAsc(Enhet administrativEnhetObjekt, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Saksmappe o WHERE o.administrativEnhetObjekt = :administrativEnhetObjekt AND"
          + " (:pivot IS NULL OR o.id >= :pivot) ORDER BY o.id ASC")
  Page<Saksmappe> paginateDesc(Enhet administrativEnhetObjekt, String pivot, Pageable pageable);
}
