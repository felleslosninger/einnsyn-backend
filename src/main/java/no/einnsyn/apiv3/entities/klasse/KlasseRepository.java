package no.einnsyn.apiv3.entities.klasse;

import java.util.stream.Stream;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.apiv3.entities.arkivdel.models.Arkivdel;
import no.einnsyn.apiv3.entities.klasse.models.Klasse;
import no.einnsyn.apiv3.entities.klassifikasjonssystem.models.Klassifikasjonssystem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface KlasseRepository extends ArkivBaseRepository<Klasse> {

  Stream<Klasse> findAllByParentArkivdel(Arkivdel parentArkivdel);

  Stream<Klasse> findAllByParentKlasse(Klasse parentKlasse);

  Stream<Klasse> findAllByParentKlassifikasjonssystem(
      Klassifikasjonssystem parentKlassifikasjonssystem);

  @Query(
      """
      SELECT o FROM Klasse o
      WHERE parentArkivdel = :arkivdel
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Page<Klasse> paginateAsc(Arkivdel arkivdel, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Klasse o
      WHERE parentArkivdel = :arkivdel
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Page<Klasse> paginateDesc(Arkivdel arkivdel, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Klasse o
      WHERE parentKlasse = :parentKlasse
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Page<Klasse> paginateAsc(Klasse parentKlasse, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Klasse o
      WHERE parentKlasse = :parentKlasse
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Page<Klasse> paginateDesc(Klasse parentKlasse, String pivot, Pageable pageable);
}
