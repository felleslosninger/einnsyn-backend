package no.einnsyn.apiv3.entities.moetesak;

import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.apiv3.entities.moetemappe.models.Moetemappe;
import no.einnsyn.apiv3.entities.moetesak.models.Moetesak;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MoetesakRepository extends ArkivBaseRepository<Moetesak> {
  Page<Moetesak> findByMoetemappeOrderByIdDesc(Moetemappe moetemappe, Pageable pageable);

  Page<Moetesak> findByMoetemappeAndIdGreaterThanOrderByIdDesc(
      Moetemappe moetemappe, String id, Pageable pageable);

  Page<Moetesak> findByMoetemappeAndIdLessThanOrderByIdDesc(
      Moetemappe moetemappe, String id, Pageable pageable);
}
