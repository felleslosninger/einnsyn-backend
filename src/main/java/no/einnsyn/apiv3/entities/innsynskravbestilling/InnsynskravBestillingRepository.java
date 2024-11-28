package no.einnsyn.apiv3.entities.innsynskravbestilling;

import java.time.Instant;
import java.util.stream.Stream;
import no.einnsyn.apiv3.entities.base.BaseRepository;
import no.einnsyn.apiv3.entities.bruker.models.Bruker;
import no.einnsyn.apiv3.entities.innsynskravbestilling.models.InnsynskravBestilling;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface InnsynskravBestillingRepository extends BaseRepository<InnsynskravBestilling> {

  // TODO: Create an index for this query
  @Query(
      """
        SELECT DISTINCT ib
        FROM InnsynskravBestilling ib
        INNER JOIN ib.innsynskrav id
        WHERE ib.verified = true
        AND id.sent IS NULL
        AND id.retryCount < 6
        AND (
          id.retryTimestamp IS NULL OR
          id.retryTimestamp < :compareTimestamp
        )
        AND (ib.innsynskravVersion = 1)
      """)
  Stream<InnsynskravBestilling> findFailedSendings(Instant compareTimestamp);

  @Query(
      """
      SELECT o FROM InnsynskravBestilling o
      WHERE bruker = :bruker
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Page<InnsynskravBestilling> paginateAsc(Bruker bruker, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM InnsynskravBestilling o
      WHERE bruker = :bruker
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Page<InnsynskravBestilling> paginateDesc(Bruker bruker, String pivot, Pageable pageable);
}
