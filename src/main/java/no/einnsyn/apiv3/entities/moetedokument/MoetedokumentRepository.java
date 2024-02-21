package no.einnsyn.apiv3.entities.moetedokument;

import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.moetedokument.models.Moetedokument;
import no.einnsyn.apiv3.entities.moetemappe.models.Moetemappe;
import no.einnsyn.apiv3.entities.registrering.RegistreringRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface MoetedokumentRepository extends RegistreringRepository<Moetedokument> {
  @Query(
      "SELECT o FROM Moetedokument o WHERE o.moetemappe = :moetemappe AND (:pivot IS NULL OR o.id"
          + " >= :pivot) ORDER BY o.id ASC")
  Page<Moetedokument> paginateAsc(Moetemappe moetemappe, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Moetedokument o WHERE o.moetemappe = :moetemappe AND (:pivot IS NULL OR o.id"
          + " <= :pivot) ORDER BY o.id DESC")
  Page<Moetedokument> paginateDesc(Moetemappe moetemappe, String pivot, Pageable pageable);

  @Query(
      "SELECT COUNT(m) FROM Moetedokument m JOIN m.dokumentbeskrivelse d WHERE d ="
          + " :dokumentbeskrivelse")
  int countByDokumentbeskrivelse(Dokumentbeskrivelse dokumentbeskrivelse);
}
