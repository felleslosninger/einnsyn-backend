package no.einnsyn.apiv3.entities.journalpost;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import no.einnsyn.apiv3.entities.EinnsynRepository;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;

public interface JournalpostRepository extends EinnsynRepository<Journalpost, Long> {

  @Query("SELECT j FROM Journalpost j, Saksmappe s WHERE j.saksmappe = s AND s.id = ?1 ORDER BY j.id DESC")
  public Page<Journalpost> findBySaksmappeIdOrderByIdDesc(String saksmappeId, Pageable pageable);

  @Query("SELECT j FROM Journalpost j, Saksmappe s WHERE j.saksmappe = s AND s.id = ?1 AND j.id > ?2 ORDER BY j.id DESC")
  public Page<Journalpost> findBySaksmappeIdAndIdGreaterThanOrderByIdDesc(String saksmappeId,
      String id, Pageable pageable);

  @Query("SELECT j FROM Journalpost j, Saksmappe s WHERE j.saksmappe = s AND s.id = ?1 AND j.id < ?2 ORDER BY j.id DESC")
  public Page<Journalpost> findBySaksmappeIdAndIdLessThanOrderByIdDesc(String saksmappeId,
      String id, Pageable pageable);

}
