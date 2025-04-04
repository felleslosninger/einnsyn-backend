package no.einnsyn.backend.entities.arkiv;

import java.util.stream.Stream;
import no.einnsyn.backend.entities.arkiv.models.Arkiv;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;

public interface ArkivRepository extends ArkivBaseRepository<Arkiv> {
  @Query(
      """
      SELECT o FROM Arkiv o
      WHERE parent = :parent
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Slice<Arkiv> paginateAsc(Arkiv parent, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Arkiv o
      WHERE parent = :parent
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Slice<Arkiv> paginateDesc(Arkiv parent, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Arkiv o
      WHERE journalenhet = :enhet
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Slice<Arkiv> paginateAsc(Enhet enhet, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Arkiv o
      WHERE journalenhet = :enhet
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Slice<Arkiv> paginateDesc(Enhet enhet, String pivot, Pageable pageable);

  @Query("SELECT o.id FROM Arkiv o WHERE parent = :parent")
  Stream<String> findIdsByParent(Arkiv parent);
}
