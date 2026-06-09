package no.einnsyn.backend.entities.matrikkelnummer;

import java.util.Optional;
import no.einnsyn.backend.entities.base.BaseRepository;
import no.einnsyn.backend.entities.matrikkelnummer.models.Matrikkelnummer;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatrikkelnummerRepository extends BaseRepository<Matrikkelnummer> {

  @Query("SELECT m.saksmappe.id FROM Matrikkelnummer m WHERE m.id = :id")
  String findSaksmappeIdById(@Param("id") String id);

  @Query("SELECT m.moetemappe.id FROM Matrikkelnummer m WHERE m.id = :id")
  String findMoetemappeIdById(@Param("id") String id);

  @Query("SELECT m.journalpost.id FROM Matrikkelnummer m WHERE m.id = :id")
  String findJournalpostIdById(@Param("id") String id);

  @Query("SELECT m.moetesak.id FROM Matrikkelnummer m WHERE m.id = :id")
  String findMoetesakIdById(@Param("id") String id);

  @Query("SELECT m.moetedokument.id FROM Matrikkelnummer m WHERE m.id = :id")
  String findMoetedokumentIdById(@Param("id") String id);

  @Query(
      """
      SELECT m FROM Matrikkelnummer m
      LEFT JOIN FETCH m.saksmappe
      LEFT JOIN FETCH m.moetemappe
      LEFT JOIN FETCH m.journalpost
      LEFT JOIN FETCH m.moetesak
      LEFT JOIN FETCH m.moetedokument
      WHERE m.id = :id
      """)
  Optional<Matrikkelnummer> findByIdWithParents(@Param("id") String id);
}
