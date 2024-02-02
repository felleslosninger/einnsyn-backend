package no.einnsyn.apiv3.entities.korrespondansepart;

import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.korrespondansepart.models.Korrespondansepart;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface KorrespondansepartRepository extends ArkivBaseRepository<Korrespondansepart> {
  Page<Korrespondansepart> findByJournalpostOrderByIdDesc(
      Journalpost journalpost, Pageable pageable);

  Page<Korrespondansepart> findByJournalpostOrderByIdAsc(
      Journalpost journalpost, Pageable pageable);

  Page<Korrespondansepart> findByJournalpostAndIdLessThanEqualOrderByIdDesc(
      Journalpost journalpost, String id, Pageable pageable);

  Page<Korrespondansepart> findByJournalpostAndIdGreaterThanEqualOrderByIdAsc(
      Journalpost journalpost, String id, Pageable pageable);
}
