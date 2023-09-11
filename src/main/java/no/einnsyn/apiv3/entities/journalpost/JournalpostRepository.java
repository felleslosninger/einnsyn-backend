package no.einnsyn.apiv3.entities.journalpost;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;

public interface JournalpostRepository extends CrudRepository<Journalpost, Long> {

  public Optional<Journalpost> findById(String id);

}
