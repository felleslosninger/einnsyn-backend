package no.einnsyn.apiv3.entities.journalpost;

import org.springframework.data.repository.CrudRepository;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;

public interface JournalpostRepository extends CrudRepository<Journalpost, Long> {

  public boolean existsById(String id);

  public Journalpost findById(String id);

}
