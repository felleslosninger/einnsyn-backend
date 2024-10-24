package no.einnsyn.apiv3.entities.arkiv;

import java.util.stream.Stream;
import no.einnsyn.apiv3.entities.arkiv.models.Arkiv;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface ArkivRepository extends ArkivBaseRepository<Arkiv> {
  @Query(
      """
      SELECT o FROM Arkiv o
      WHERE parent = :parent
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Page<Arkiv> paginateAsc(Arkiv parent, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Arkiv o
      WHERE parent = :parent
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Page<Arkiv> paginateDesc(Arkiv parent, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Arkiv o
      WHERE journalenhet = :enhet
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Page<Arkiv> paginateAsc(Enhet enhet, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Arkiv o
      WHERE journalenhet = :enhet
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Page<Arkiv> paginateDesc(Enhet enhet, String pivot, Pageable pageable);

  Stream<Arkiv> findAllByParent(Arkiv parent);
}
