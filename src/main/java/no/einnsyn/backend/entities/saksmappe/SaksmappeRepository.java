package no.einnsyn.backend.entities.saksmappe;

import java.util.stream.Stream;
import no.einnsyn.backend.common.indexable.IndexableRepository;
import no.einnsyn.backend.entities.arkivdel.models.Arkivdel;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import no.einnsyn.backend.entities.klasse.models.Klasse;
import no.einnsyn.backend.entities.mappe.MappeRepository;
import no.einnsyn.backend.entities.saksmappe.models.Saksmappe;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;

public interface SaksmappeRepository
    extends MappeRepository<Saksmappe>, IndexableRepository<Saksmappe> {

  @Query(
      """
      SELECT o FROM Saksmappe o
      WHERE parentArkivdel = :arkivdel
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Slice<Saksmappe> paginateAsc(Arkivdel arkivdel, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Saksmappe o
      WHERE parentArkivdel = :arkivdel
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Slice<Saksmappe> paginateDesc(Arkivdel arkivdel, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Saksmappe o
      WHERE parentKlasse = :klasse
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Slice<Saksmappe> paginateAsc(Klasse klasse, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Saksmappe o
      WHERE parentKlasse = :klasse
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Slice<Saksmappe> paginateDesc(Klasse klasse, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Saksmappe o
      WHERE administrativEnhetObjekt = :administrativEnhetObjekt
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Slice<Saksmappe> paginateAsc(Enhet administrativEnhetObjekt, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Saksmappe o
      WHERE administrativEnhetObjekt = :administrativEnhetObjekt
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Slice<Saksmappe> paginateDesc(Enhet administrativEnhetObjekt, String pivot, Pageable pageable);

  @Query(
      """
      SELECT id FROM Saksmappe
      WHERE administrativEnhetObjekt = :administrativEnhetObjekt
      """)
  Stream<String> streamIdByAdministrativEnhetObjekt(Enhet administrativEnhetObjekt);

  @Query(
      """
      SELECT s.id FROM Journalpost j
      JOIN j.saksmappe s
      WHERE j.id = :journalpostId
      """)
  String findIdByJournalpostId(String journalpostId);
}
