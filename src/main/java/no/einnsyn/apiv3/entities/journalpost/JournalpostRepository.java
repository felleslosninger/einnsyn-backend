package no.einnsyn.apiv3.entities.journalpost;

import java.util.stream.Stream;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.registrering.RegistreringRepository;
import no.einnsyn.apiv3.entities.skjerming.models.Skjerming;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface JournalpostRepository extends RegistreringRepository<Journalpost> {

  @Query(
      "SELECT j FROM Journalpost j, Saksmappe s WHERE j.saksmappe = s AND s.id = ?1 ORDER BY j.id"
          + " DESC")
  Page<Journalpost> findBySaksmappeIdOrderByIdDesc(String saksmappeId, Pageable pageable);

  @Query(
      "SELECT j FROM Journalpost j, Saksmappe s WHERE j.saksmappe = s AND s.id = ?1 AND j.id > ?2"
          + " ORDER BY j.id DESC")
  Page<Journalpost> findBySaksmappeIdAndIdGreaterThanOrderByIdDesc(
      String saksmappeId, String id, Pageable pageable);

  @Query(
      "SELECT j FROM Journalpost j, Saksmappe s WHERE j.saksmappe = s AND s.id = ?1 AND j.id < ?2"
          + " ORDER BY j.id DESC")
  Page<Journalpost> findBySaksmappeIdAndIdLessThanOrderByIdDesc(
      String saksmappeId, String id, Pageable pageable);

  @Query(
      "SELECT COUNT(j) FROM Journalpost j JOIN j.dokumentbeskrivelse d WHERE d ="
          + " :dokumentbeskrivelse")
  int countByDokumentbeskrivelse(Dokumentbeskrivelse dokumentbeskrivelse);

  int countBySkjerming(Skjerming skjerming);

  Stream<Journalpost> findByAdministrativEnhetObjekt(Enhet enhet);
}
