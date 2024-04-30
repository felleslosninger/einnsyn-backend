package no.einnsyn.apiv3.entities.moetesak;

import java.util.List;
import java.util.stream.Stream;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.moetemappe.models.Moetemappe;
import no.einnsyn.apiv3.entities.moetesak.models.Moetesak;
import no.einnsyn.apiv3.entities.registrering.RegistreringRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface MoetesakRepository extends RegistreringRepository<Moetesak> {
  @Query(
      "SELECT o FROM Moetesak o WHERE o.moetemappe = :moetemappe AND (:pivot IS NULL OR o.id >="
          + " :pivot) ORDER BY o.id ASC")
  Page<Moetesak> paginateAsc(Moetemappe moetemappe, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Moetesak o WHERE o.moetemappe = :moetemappe AND (:pivot IS NULL OR o.id <="
          + " :pivot) ORDER BY o.id DESC")
  Page<Moetesak> paginateDesc(Moetemappe moetemappe, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Moetesak o WHERE o.utvalgObjekt = :utvalgObjekt AND"
          + " (:pivot IS NULL OR o.id >= :pivot) ORDER BY o.id ASC")
  Page<Moetesak> paginateAsc(Enhet utvalgObjekt, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Moetesak o WHERE o.utvalgObjekt = :utvalgObjekt AND"
          + " (:pivot IS NULL OR o.id <= :pivot) ORDER BY o.id DESC")
  Page<Moetesak> paginateDesc(Enhet utvalgObjekt, String pivot, Pageable pageable);

  Stream<Moetesak> findAllByUtvalgObjekt(Enhet utvalgObjekt);

  @Query(
      "SELECT COUNT(m) FROM Moetesak m JOIN m.dokumentbeskrivelse d WHERE d = :dokumentbeskrivelse")
  int countByDokumentbeskrivelse(Dokumentbeskrivelse dokumentbeskrivelse);

  @Query("SELECT m FROM Moetesak m JOIN m.dokumentbeskrivelse d WHERE d = :dokumentbeskrivelse")
  List<Moetesak> findByDokumentbeskrivelse(Dokumentbeskrivelse dokumentbeskrivelse);
}
