package no.einnsyn.backend.entities.mappe;

import java.util.stream.Stream;
import no.einnsyn.backend.common.hasslug.HasSlugRepository;
import no.einnsyn.backend.entities.arkiv.models.Arkiv;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.backend.entities.arkivdel.models.Arkivdel;
import no.einnsyn.backend.entities.klasse.models.Klasse;
import no.einnsyn.backend.entities.mappe.models.Mappe;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface MappeRepository<T extends Mappe> extends ArkivBaseRepository<T>, HasSlugRepository<T> {
  @Query("SELECT id FROM #{#entityName} WHERE parentArkiv = :parentArkiv")
  Stream<String> streamIdByParentArkiv(Arkiv parentArkiv);

  @Query("SELECT id FROM #{#entityName} WHERE parentArkivdel = :parentArkivdel")
  Stream<String> streamIdByParentArkivdel(Arkivdel parentArkivdel);

  @Query("SELECT id FROM #{#entityName} WHERE parentKlasse = :parentKlasse")
  Stream<String> streamIdByParentKlasse(Klasse parentKlasse);
}
