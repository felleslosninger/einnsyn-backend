package no.einnsyn.backend.entities.mappe;

import java.util.stream.Stream;
import no.einnsyn.backend.entities.arkiv.models.Arkiv;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.backend.entities.arkivdel.models.Arkivdel;
import no.einnsyn.backend.entities.klasse.models.Klasse;
import no.einnsyn.backend.entities.mappe.models.Mappe;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface MappeRepository<T extends Mappe> extends ArkivBaseRepository<T> {
  @Query("SELECT o.id FROM #{#entityName} o WHERE parentArkiv = :parentArkiv")
  Stream<String> findIdsByParentArkiv(Arkiv parentArkiv);

  @Query("SELECT o.id FROM #{#entityName} o WHERE parentArkivdel = :parentArkivdel")
  Stream<String> findIdsByParentArkivdel(Arkivdel parentArkivdel);

  @Query("SELECT o.id FROM #{#entityName} o WHERE parentKlasse = :parentKlasse")
  Stream<String> findAllByParentKlasse(Klasse parentKlasse);
}
