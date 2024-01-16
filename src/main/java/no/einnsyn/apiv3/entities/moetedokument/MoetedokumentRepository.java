package no.einnsyn.apiv3.entities.moetedokument;

import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.apiv3.entities.moetedokument.models.Moetedokument;
import no.einnsyn.apiv3.entities.moetemappe.models.Moetemappe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MoetedokumentRepository extends ArkivBaseRepository<Moetedokument> {
  public Page<Moetedokument> findByMoetemappeOrderByIdDesc(
      Moetemappe moetemappe, Pageable pageable);

  public Page<Moetedokument> findByMoetemappeAndIdGreaterThanOrderByIdDesc(
      Moetemappe moetemappe, String id, Pageable pageable);

  public Page<Moetedokument> findByMoetemappeAndIdLessThanOrderByIdDesc(
      Moetemappe moetemappe, String id, Pageable pageable);
}
