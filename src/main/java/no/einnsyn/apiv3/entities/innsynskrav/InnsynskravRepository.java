package no.einnsyn.apiv3.entities.innsynskrav;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import no.einnsyn.apiv3.entities.EinnsynRepository;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;
import org.springframework.data.jpa.repository.Query;

public interface InnsynskravRepository extends EinnsynRepository<Innsynskrav, UUID> {

  @Query(
      """
        SELECT i
        FROM Innsynskrav i
        INNER JOIN i.innsynskravDel id
        WHERE i.verified = true
        AND id.sent IS NULL
        AND id.retryCount < 6
        AND (
          id.retryTimestamp IS NULL OR
          id.retryTimestamp < :compareTimestamp
        )
        GROUP BY i
      """)
  public Stream<Innsynskrav> findFailedSendings(Instant compareTimestamp);
}
