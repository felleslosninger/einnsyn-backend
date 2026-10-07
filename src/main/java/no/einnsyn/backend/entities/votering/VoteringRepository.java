package no.einnsyn.backend.entities.votering;

import no.einnsyn.backend.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.backend.entities.identifikator.models.Identifikator;
import no.einnsyn.backend.entities.moetedeltaker.models.Moetedeltaker;
import no.einnsyn.backend.entities.vedtak.models.Vedtak;
import no.einnsyn.backend.entities.votering.models.Votering;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;

public interface VoteringRepository extends ArkivBaseRepository<Votering> {

  boolean existsByMoetedeltaker(Moetedeltaker moetedeltaker);

  boolean existsByRepresenterer(Identifikator representerer);

  @Query(
      """
      SELECT o FROM Votering o
      WHERE vedtak = :vedtak
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Slice<Votering> paginateAsc(Vedtak vedtak, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Votering o
      WHERE vedtak = :vedtak
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Slice<Votering> paginateDesc(Vedtak vedtak, String pivot, Pageable pageable);
}
