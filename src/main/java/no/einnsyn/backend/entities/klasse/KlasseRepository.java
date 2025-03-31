package no.einnsyn.backend.entities.klasse;

import java.util.stream.Stream;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.backend.entities.arkivdel.models.Arkivdel;
import no.einnsyn.backend.entities.klasse.models.Klasse;
import no.einnsyn.backend.entities.klassifikasjonssystem.models.Klassifikasjonssystem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;

public interface KlasseRepository extends ArkivBaseRepository<Klasse> {

  @Query("SELECT o.id FROM Klasse o WHERE parentArkivdel = :parentArkivdel")
  Stream<String> findIdsByParentArkivdel(Arkivdel parentArkivdel);

  @Query("SELECT o.id FROM Klasse o WHERE parentKlasse = :parentKlasse")
  Stream<String> findIdsByParentKlasse(Klasse parentKlasse);

  @Query(
      "SELECT o.id FROM Klasse o WHERE parentKlassifikasjonssystem = :parentKlassifikasjonssystem")
  Stream<String> findIdsByParentKlassifikasjonssystem(
      Klassifikasjonssystem parentKlassifikasjonssystem);

  @Query(
      """
      SELECT o FROM Klasse o
      WHERE parentArkivdel = :arkivdel
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Slice<Klasse> paginateAsc(Arkivdel arkivdel, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Klasse o
      WHERE parentArkivdel = :arkivdel
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Slice<Klasse> paginateDesc(Arkivdel arkivdel, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Klasse o
      WHERE parentKlasse = :parentKlasse
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Slice<Klasse> paginateAsc(Klasse parentKlasse, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Klasse o
      WHERE parentKlasse = :parentKlasse
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Slice<Klasse> paginateDesc(Klasse parentKlasse, String pivot, Pageable pageable);
}
