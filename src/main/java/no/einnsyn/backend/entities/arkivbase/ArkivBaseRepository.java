package no.einnsyn.backend.entities.arkivbase;

import java.util.List;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBase;
import no.einnsyn.backend.entities.base.BaseRepository;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface ArkivBaseRepository<T extends ArkivBase> extends BaseRepository<T> {

  T findBySystemId(String systemId);

  List<T> findByExternalIdInAndJournalenhet(List<String> externalIds, Enhet journalenhet);

  T findByExternalIdAndJournalenhet(String externalId, Enhet journalenhet);

  @Query(
      """
      SELECT o FROM #{#entityName} o
      WHERE o.journalenhet = :journalenhet
      AND o.id >= COALESCE(:pivot, o.id)
      ORDER BY o.id ASC
      """)
  Slice<T> paginateByJournalenhetAsc(Enhet journalenhet, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM #{#entityName} o
      WHERE o.journalenhet = :journalenhet
      AND o.id <= COALESCE(:pivot, o.id)
      ORDER BY o.id DESC
      """)
  Slice<T> paginateByJournalenhetDesc(Enhet journalenhet, String pivot, Pageable pageable);
}
