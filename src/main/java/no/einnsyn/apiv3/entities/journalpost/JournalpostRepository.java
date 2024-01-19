package no.einnsyn.apiv3.entities.journalpost;

import java.util.stream.Stream;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.skjerming.models.Skjerming;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface JournalpostRepository extends ArkivBaseRepository<Journalpost> {

  @Query(
      "SELECT j FROM Journalpost j, Saksmappe s WHERE j.saksmappe = s AND s.id = ?1 ORDER BY j.id"
          + " DESC")
  public Page<Journalpost> findBySaksmappeIdOrderByIdDesc(String saksmappeId, Pageable pageable);

  @Query(
      "SELECT j FROM Journalpost j, Saksmappe s WHERE j.saksmappe = s AND s.id = ?1 AND j.id > ?2"
          + " ORDER BY j.id DESC")
  public Page<Journalpost> findBySaksmappeIdAndIdGreaterThanOrderByIdDesc(
      String saksmappeId, String id, Pageable pageable);

  @Query(
      "SELECT j FROM Journalpost j, Saksmappe s WHERE j.saksmappe = s AND s.id = ?1 AND j.id < ?2"
          + " ORDER BY j.id DESC")
  public Page<Journalpost> findBySaksmappeIdAndIdLessThanOrderByIdDesc(
      String saksmappeId, String id, Pageable pageable);

  @Query(
      "SELECT COUNT(j) FROM Journalpost j JOIN j.dokumentbeskrivelse d WHERE d ="
          + " :dokumentbeskrivelse")
  public int countByDokumentbeskrivelse(Dokumentbeskrivelse dokumentbeskrivelse);

  @Query("SELECT COUNT(j) FROM Journalpost j JOIN j.skjerming s WHERE s =" + " :skjerming")
  public int countBySkjerming(Skjerming skjerming);

  public Stream<Journalpost> findByAdministrativEnhetObjekt(Enhet enhet);
}
