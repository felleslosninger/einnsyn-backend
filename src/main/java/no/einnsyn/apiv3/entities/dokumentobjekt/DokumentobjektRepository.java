package no.einnsyn.apiv3.entities.dokumentobjekt;

import org.springframework.data.repository.CrudRepository;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.Dokumentobjekt;

public interface DokumentobjektRepository extends CrudRepository<Dokumentobjekt, Integer> {

  public Dokumentobjekt findById(String id);

}
