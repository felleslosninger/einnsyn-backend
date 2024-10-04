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
          + "e.hitCount = e.hitCount + 1 WHERE "
          + "(e.saksmappe.id = :mappeId OR e.moetemappe.id = :mappeId) AND "
          + "e.abonnere = true")
  void addHit(String mappeId);

  @Modifying
  @Query(
      ""
          + "UPDATE #{#entityName} e SET "
          + "e.hitCount = 0 WHERE "
          + "(e.saksmappe.id = :mappeId OR e.moetemappe.id = :mappeId) AND "
          + "e.abonnere = true")
  void resetHits(String mappeId);
}
