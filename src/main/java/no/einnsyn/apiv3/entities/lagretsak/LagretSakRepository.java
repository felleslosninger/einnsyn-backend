package no.einnsyn.apiv3.entities.lagretsak;

import no.einnsyn.apiv3.entities.base.BaseRepository;
import no.einnsyn.apiv3.entities.lagretsak.models.LagretSak;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface LagretSakRepository extends BaseRepository<LagretSak> {
  @Modifying
  @Query(
      ""
          + "UPDATE #{#entityName} e SET "
          + "e.hasMatch = e.hasMatch + 1 WHERE "
          + "(e.saksmappeId = :mappeId OR e.moetemappeId = :mappeId) AND "
          + "e.abonnere = true")
  void addMatch(String mappeId);

  @Modifying
  @Query(
      ""
          + "UPDATE #{#entityName} e SET "
          + "e.hasMatch = 0 WHERE "
          + "(e.saksmappeId = :mappeId OR e.moetemappeId = :mappeId) AND "
          + "e.abonnere = true")
  void resetMatch(String mappeId);
}
