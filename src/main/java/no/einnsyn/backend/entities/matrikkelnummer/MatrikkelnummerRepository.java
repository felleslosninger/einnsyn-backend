package no.einnsyn.backend.entities.matrikkelnummer;

import java.util.stream.Stream;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import no.einnsyn.backend.entities.matrikkelnummer.models.Matrikkelnummer;
import org.springframework.data.jpa.repository.Query;

public interface MatrikkelnummerRepository extends ArkivBaseRepository<Matrikkelnummer> {

  Matrikkelnummer
      findByJournalenhetAndKommunenummerAndGaardsnummerAndBruksnummerAndFestenummerAndSeksjonsnummer(
          Enhet journalenhet,
          String kommunenummer,
          Integer gaardsnummer,
          Integer bruksnummer,
          Integer festenummer,
          Integer seksjonsnummer);

  @Query(
      """
      SELECT j.id FROM Journalpost j
      JOIN j.matrikkelnummer m
      WHERE m.id = :matrikkelnummerId
      ORDER BY j.id DESC
      """)
  Stream<String> streamJournalpostIdByMatrikkelnummerId(String matrikkelnummerId);

  @Query(
      """
      SELECT s.id FROM Saksmappe s
      JOIN s.matrikkelnummer m
      WHERE m.id = :matrikkelnummerId
      ORDER BY s.id DESC
      """)
  Stream<String> streamSaksmappeIdByMatrikkelnummerId(String matrikkelnummerId);
}
