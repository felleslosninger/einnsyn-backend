package no.einnsyn.apiv3.entities.enhet;

import no.einnsyn.apiv3.entities.base.BaseRepository;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EnhetRepository extends BaseRepository<Enhet> {

  Page<Enhet> findByParentOrderByIdDesc(Enhet parent, Pageable pageable);

  Page<Enhet> findByParentOrderByIdAsc(Enhet parent, Pageable pageable);

  Page<Enhet> findByParentAndIdLessThanEqualOrderByIdDesc(
      Enhet parent, String id, Pageable pageable);

  Page<Enhet> findByParentAndIdGreaterThanEqualOrderByIdAsc(
      Enhet parent, String id, Pageable pageable);
}
