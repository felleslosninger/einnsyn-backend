package no.einnsyn.apiv3.entities.saksmappe;

import java.util.stream.Stream;
import no.einnsyn.apiv3.common.indexable.IndexableRepository;
import no.einnsyn.apiv3.entities.arkiv.models.Arkiv;
import no.einnsyn.apiv3.entities.arkivdel.models.Arkivdel;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.klasse.models.Klasse;
import no.einnsyn.apiv3.entities.mappe.MappeRepository;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface SaksmappeRepository
    extends MappeRepository<Saksmappe>, IndexableRepository<Saksmappe> {

  @Query(
      "SELECT o FROM Saksmappe o WHERE o.parentArkiv = :arkiv AND (:pivot IS NULL OR o.id >="
          + " :pivot) ORDER BY o.id ASC")
  Page<Saksmappe> paginateAsc(Arkiv arkiv, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Saksmappe o WHERE o.parentArkiv = :arkiv AND (:pivot IS NULL OR o.id <="
          + " :pivot) ORDER BY o.id DESC")
  Page<Saksmappe> paginateDesc(Arkiv arkiv, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Saksmappe o WHERE o.parentArkivdel = :arkivdel AND (:pivot IS NULL OR o.id >="
          + " :pivot) ORDER BY o.id ASC")
  Page<Saksmappe> paginateAsc(Arkivdel arkivdel, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Saksmappe o WHERE o.parentArkivdel = :arkivdel AND (:pivot IS NULL OR o.id <="
          + " :pivot) ORDER BY o.id DESC")
  Page<Saksmappe> paginateDesc(Arkivdel arkivdel, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Saksmappe o WHERE o.parentKlasse = :klasse AND (:pivot IS NULL OR o.id >="
          + " :pivot) ORDER BY o.id ASC")
  Page<Saksmappe> paginateAsc(Klasse klasse, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Saksmappe o WHERE o.parentKlasse = :klasse AND (:pivot IS NULL OR o.id >="
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

  Stream<Saksmappe> findAllByAdministrativEnhetObjekt(Enhet administrativEnhetObjekt);
}
