package no.einnsyn.apiv3.entities.journalpost;

import java.util.stream.Stream;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.registrering.RegistreringRepository;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;
import no.einnsyn.apiv3.entities.skjerming.models.Skjerming;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface JournalpostRepository extends RegistreringRepository<Journalpost> {

  Page<Journalpost> findBySaksmappeOrderByIdDesc(Saksmappe saksmappe, Pageable pageable);

  Page<Journalpost> findBySaksmappeOrderByIdAsc(Saksmappe saksmappe, Pageable pageable);

  Page<Journalpost> findBySaksmappeAndIdLessThanEqualOrderByIdDesc(
      Saksmappe saksmappe, String id, Pageable pageable);

  Page<Journalpost> findBySaksmappeAndIdGreaterThanEqualOrderByIdAsc(
      Saksmappe saksmappe, String id, Pageable pageable);

  @Query(
      "SELECT COUNT(j) FROM Journalpost j JOIN j.dokumentbeskrivelse d WHERE d ="
          + " :dokumentbeskrivelse")
  int countByDokumentbeskrivelse(Dokumentbeskrivelse dokumentbeskrivelse);

  int countBySkjerming(Skjerming skjerming);

  Stream<Journalpost> findByAdministrativEnhetObjekt(Enhet enhet);
}
