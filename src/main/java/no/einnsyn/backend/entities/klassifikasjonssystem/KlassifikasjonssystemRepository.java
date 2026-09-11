package no.einnsyn.backend.entities.klassifikasjonssystem;

import java.util.stream.Stream;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.backend.entities.arkivdel.models.Arkivdel;
import no.einnsyn.backend.entities.klassifikasjonssystem.models.Klassifikasjonssystem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;

public interface KlassifikasjonssystemRepository
    extends ArkivBaseRepository<Klassifikasjonssystem> {
  @Query("SELECT id FROM Klassifikasjonssystem WHERE arkivdel = :arkivdel")
  Stream<String> streamIdByArkivdel(Arkivdel arkivdel);

  @Query(
      """
      SELECT o FROM Klassifikasjonssystem o
      WHERE arkivdel = :arkivdel
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Slice<Klassifikasjonssystem> paginateAsc(Arkivdel arkivdel, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Klassifikasjonssystem o
      WHERE arkivdel = :arkivdel
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Slice<Klassifikasjonssystem> paginateDesc(Arkivdel arkivdel, String pivot, Pageable pageable);
}
