package no.einnsyn.backend.entities.apikey;

import java.util.stream.Stream;
import no.einnsyn.backend.entities.apikey.models.ApiKey;
import no.einnsyn.backend.entities.base.BaseRepository;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;

public interface ApiKeyRepository extends BaseRepository<ApiKey> {

  @Query(
      """
      SELECT k FROM ApiKey k
      WHERE enhet = :enhet
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Slice<ApiKey> paginateAsc(Enhet enhet, String pivot, Pageable pageable);

  @Query(
      """
      SELECT k FROM ApiKey k
      WHERE enhet = :enhet
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Slice<ApiKey> paginateDesc(Enhet enhet, String pivot, Pageable pageable);

  @Query("SELECT id FROM ApiKey WHERE enhet = :enhet")
  Stream<String> streamIdByEnhet(Enhet enhet);

  ApiKey findBySecret(String hashedSecret);
}
