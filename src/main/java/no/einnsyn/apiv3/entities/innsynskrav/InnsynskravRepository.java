package no.einnsyn.apiv3.entities.innsynskrav;

import java.time.Instant;
import java.util.stream.Stream;
import no.einnsyn.apiv3.entities.base.BaseRepository;
import no.einnsyn.apiv3.entities.bruker.models.Bruker;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface InnsynskravRepository extends BaseRepository<Innsynskrav> {

  @Query(
      """
        SELECT DISTINCT i
        FROM Innsynskrav i
        INNER JOIN i.innsynskravDel id
        WHERE i.verified = true
        AND id.sent IS NULL
        AND id.retryCount < 6
        AND (
          id.retryTimestamp IS NULL OR
          id.retryTimestamp < :compareTimestamp
        )
        AND (i.innsynskravVersion = 1)
      """)
  Stream<Innsynskrav> findFailedSendings(Instant compareTimestamp);

  @Query(
      "SELECT o FROM Innsynskrav o WHERE o.bruker = :bruker AND (:pivot IS NULL OR o.id >= :pivot)"
          + " ORDER BY o.id ASC")
  Page<Innsynskrav> paginateAsc(Bruker bruker, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Innsynskrav o WHERE o.bruker = :bruker AND (:pivot IS NULL OR o.id <= :pivot)"
          + " ORDER BY o.id DESC")
  Page<Innsynskrav> paginateDesc(Bruker bruker, String pivot, Pageable pageable);
}
