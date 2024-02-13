package no.einnsyn.apiv3.entities.mappe;

import java.util.stream.Stream;
import no.einnsyn.apiv3.entities.arkiv.models.Arkiv;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.apiv3.entities.arkivdel.models.Arkivdel;
import no.einnsyn.apiv3.entities.klasse.models.Klasse;
import no.einnsyn.apiv3.entities.mappe.models.Mappe;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface MappeRepository<T extends Mappe> extends ArkivBaseRepository<T> {
  Stream<T> findAllByParentArkiv(Arkiv parentArkiv);

  Stream<T> findAllByParentArkivdel(Arkivdel parentArkivdel);

  Stream<T> findAllByParentKlasse(Klasse parentKlasse);
}
