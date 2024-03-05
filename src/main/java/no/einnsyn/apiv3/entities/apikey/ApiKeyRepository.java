package no.einnsyn.apiv3.entities.apikey;

import java.util.stream.Stream;
import no.einnsyn.apiv3.entities.apikey.models.ApiKey;
import no.einnsyn.apiv3.entities.base.BaseRepository;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface ApiKeyRepository extends BaseRepository<ApiKey> {
  @Query(
      "SELECT k FROM ApiKey k WHERE k.enhet = :enhet AND (:pivot IS NULL OR k.id >= :pivot)"
          + " ORDER BY k.id ASC")
  Page<ApiKey> paginateAsc(Enhet enhet, String pivot, Pageable pageable);

  @Query(
      "SELECT k FROM ApiKey k WHERE k.enhet = :enhet AND (:pivot IS NULL OR k.id <= :pivot)"
          + " ORDER BY k.id DESC")
  Page<ApiKey> paginateDesc(Enhet enhet, String pivot, Pageable pageable);

  Stream<ApiKey> findAllByEnhet(Enhet enhet);
}
