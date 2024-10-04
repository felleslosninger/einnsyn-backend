package no.einnsyn.apiv3.entities.lagretsoek;

import no.einnsyn.apiv3.entities.base.BaseRepository;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoek;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface LagretSoekRepository extends BaseRepository<LagretSoek> {
  @Modifying
  @Query(
      ""
          + "UPDATE #{#entityName} e SET "
          + "e.hitCount = e.hitCount + 1 WHERE "
          + "e.legacyId = :legacyId AND "
          + "e.abonnere = true")
  void addHitByLegacyId(String legacyId);
}
