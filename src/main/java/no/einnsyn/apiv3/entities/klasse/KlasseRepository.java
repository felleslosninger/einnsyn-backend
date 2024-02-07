package no.einnsyn.apiv3.entities.klasse;

import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.apiv3.entities.arkivdel.models.Arkivdel;
import no.einnsyn.apiv3.entities.klasse.models.Klasse;
import no.einnsyn.apiv3.entities.klassifikasjonssystem.models.Klassifikasjonssystem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface KlasseRepository extends ArkivBaseRepository<Klasse> {

  Page<Klasse> findByArkivdel(Arkivdel arkivdel, Pageable pageable);

  Page<Klasse> findByParentKlasse(Klasse parentKlasse, Pageable pageable);

  Page<Klasse> findByKlassifikasjonssystem(
      Klassifikasjonssystem klassifikasjonssystem, Pageable pageable);

  @Query(
      "SELECT o FROM Klasse o WHERE o.arkivdel = :arkivdel AND (:pivot IS NULL OR o.id >= :pivot)"
          + " ORDER BY o.id ASC")
  Page<Klasse> paginateAsc(Arkivdel arkivdel, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Klasse o WHERE o.arkivdel = :arkivdel AND (:pivot IS NULL OR o.id <= :pivot)"
          + " ORDER BY o.id DESC")
  Page<Klasse> paginateDesc(Arkivdel arkivdel, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Klasse o WHERE o.parentKlasse = :parentKlasse AND (:pivot IS NULL OR o.id >="
          + " :pivot) ORDER BY o.id ASC")
  Page<Klasse> paginateAsc(Klasse parentKlasse, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Klasse o WHERE o.parentKlasse = :parentKlasse AND (:pivot IS NULL OR o.id <="
          + " :pivot) ORDER BY o.id DESC")
  Page<Klasse> paginateDesc(Klasse parentKlasse, String pivot, Pageable pageable);
}
