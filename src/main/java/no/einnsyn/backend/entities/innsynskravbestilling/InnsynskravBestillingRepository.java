package no.einnsyn.backend.entities.innsynskravbestilling;

import java.time.Instant;
import java.util.stream.Stream;
import no.einnsyn.backend.entities.base.BaseRepository;
import no.einnsyn.backend.entities.bruker.models.Bruker;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestilling;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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
  Stream<InnsynskravBestilling> streamFailedSendings(Instant compareTimestamp);

  @Query(
      """
      SELECT o FROM InnsynskravBestilling o
      WHERE bruker = :bruker
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Slice<InnsynskravBestilling> paginateAsc(Bruker bruker, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM InnsynskravBestilling o
      WHERE bruker = :bruker
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Slice<InnsynskravBestilling> paginateDesc(Bruker bruker, String pivot, Pageable pageable);

  Stream<InnsynskravBestilling> streamAllByCreatedBeforeAndEpostIsNotNullAndBrukerIsNull(
      Instant created);
}
