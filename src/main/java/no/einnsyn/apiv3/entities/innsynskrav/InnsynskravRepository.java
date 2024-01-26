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
      """)
  Stream<Innsynskrav> findFailedSendings(Instant compareTimestamp);

  Page<Innsynskrav> findByBrukerOrderByIdDesc(Bruker bruker, Pageable pageable);

  Page<Innsynskrav> findByBrukerAndIdGreaterThanOrderByIdDesc(
      Bruker bruker, String id, Pageable pageable);

  Page<Innsynskrav> findByBrukerAndIdLessThanOrderByIdDesc(
      Bruker bruker, String id, Pageable pageable);
}
