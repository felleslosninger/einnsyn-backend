package no.einnsyn.backend.entities.mappe;

import java.util.stream.Stream;
import no.einnsyn.backend.entities.arkiv.models.Arkiv;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.backend.entities.arkivdel.models.Arkivdel;
import no.einnsyn.backend.entities.klasse.models.Klasse;
import no.einnsyn.backend.entities.mappe.models.Mappe;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface MappeRepository<T extends Mappe> extends ArkivBaseRepository<T> {
  Stream<T> findAllByParentArkiv(Arkiv parentArkiv);

  Stream<T> findAllByParentArkivdel(Arkivdel parentArkivdel);

  Stream<T> findAllByParentKlasse(Klasse parentKlasse);
}
