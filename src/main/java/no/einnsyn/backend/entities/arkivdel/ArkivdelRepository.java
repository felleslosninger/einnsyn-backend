package no.einnsyn.backend.entities.arkivdel;

import java.util.stream.Stream;
import no.einnsyn.backend.entities.arkiv.models.Arkiv;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.backend.entities.arkivdel.models.Arkivdel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;

public interface ArkivdelRepository extends ArkivBaseRepository<Arkivdel> {
  @Query(
      """
      SELECT o FROM Arkivdel o
      WHERE parent = :arkiv
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Slice<Arkivdel> paginateAsc(Arkiv arkiv, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Arkivdel o
      WHERE parent = :arkiv
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Slice<Arkivdel> paginateDesc(Arkiv arkiv, String pivot, Pageable pageable);

  @Query("SELECT o.id FROM Arkivdel o WHERE parent = :parent")
  Stream<String> findIdsByParent(Arkiv parent);
}
