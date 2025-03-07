package no.einnsyn.backend.entities.moetedokument;

import java.util.List;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.backend.entities.moetedokument.models.Moetedokument;
import no.einnsyn.backend.entities.moetemappe.models.Moetemappe;
import no.einnsyn.backend.entities.registrering.RegistreringRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;

public interface MoetedokumentRepository extends RegistreringRepository<Moetedokument> {
  @Query(
      """
      SELECT o FROM Moetedokument o
      WHERE moetemappe = :moetemappe
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Slice<Moetedokument> paginateAsc(Moetemappe moetemappe, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Moetedokument o
      WHERE moetemappe = :moetemappe
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Slice<Moetedokument> paginateDesc(Moetemappe moetemappe, String pivot, Pageable pageable);

  @Query(
      """
      SELECT COUNT(m) FROM Moetedokument m
      JOIN m.dokumentbeskrivelse d
      WHERE d = :dokumentbeskrivelse
      """)
  int countByDokumentbeskrivelse(Dokumentbeskrivelse dokumentbeskrivelse);

  @Query(
      """
      SELECT m FROM Moetedokument m
      JOIN dokumentbeskrivelse d
      WHERE d = :dokumentbeskrivelse
      """)
  List<Moetedokument> findByDokumentbeskrivelse(Dokumentbeskrivelse dokumentbeskrivelse);
}
