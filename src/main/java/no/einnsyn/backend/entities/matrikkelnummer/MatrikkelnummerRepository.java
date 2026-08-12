package no.einnsyn.backend.entities.matrikkelnummer;

import java.util.Optional;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.backend.entities.journalpost.models.Journalpost;
import no.einnsyn.backend.entities.matrikkelnummer.models.Matrikkelnummer;
import no.einnsyn.backend.entities.moetedokument.models.Moetedokument;
import no.einnsyn.backend.entities.moetemappe.models.Moetemappe;
import no.einnsyn.backend.entities.moetesak.models.Moetesak;
import no.einnsyn.backend.entities.saksmappe.models.Saksmappe;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatrikkelnummerRepository extends ArkivBaseRepository<Matrikkelnummer> {

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

  @Query(
      "SELECT m FROM Matrikkelnummer m WHERE m.saksmappe = :saksmappe AND m.id >= COALESCE(:pivot, m.id) ORDER BY m.id ASC")
  Slice<Matrikkelnummer> paginateAsc(Saksmappe saksmappe, String pivot, Pageable pageable);

  @Query(
      "SELECT m FROM Matrikkelnummer m WHERE m.saksmappe = :saksmappe AND m.id <= COALESCE(:pivot, m.id) ORDER BY m.id DESC")
  Slice<Matrikkelnummer> paginateDesc(Saksmappe saksmappe, String pivot, Pageable pageable);

  @Query(
      "SELECT m FROM Matrikkelnummer m WHERE m.moetemappe = :moetemappe AND m.id >= COALESCE(:pivot, m.id) ORDER BY m.id ASC")
  Slice<Matrikkelnummer> paginateAsc(Moetemappe moetemappe, String pivot, Pageable pageable);

  @Query(
      "SELECT m FROM Matrikkelnummer m WHERE m.moetemappe = :moetemappe AND m.id <= COALESCE(:pivot, m.id) ORDER BY m.id DESC")
  Slice<Matrikkelnummer> paginateDesc(Moetemappe moetemappe, String pivot, Pageable pageable);

  @Query(
      "SELECT m FROM Matrikkelnummer m WHERE m.journalpost = :journalpost AND m.id >= COALESCE(:pivot, m.id) ORDER BY m.id ASC")
  Slice<Matrikkelnummer> paginateAsc(Journalpost journalpost, String pivot, Pageable pageable);

  @Query(
      "SELECT m FROM Matrikkelnummer m WHERE m.journalpost = :journalpost AND m.id <= COALESCE(:pivot, m.id) ORDER BY m.id DESC")
  Slice<Matrikkelnummer> paginateDesc(Journalpost journalpost, String pivot, Pageable pageable);

  @Query(
      "SELECT m FROM Matrikkelnummer m WHERE m.moetesak = :moetesak AND m.id >= COALESCE(:pivot, m.id) ORDER BY m.id ASC")
  Slice<Matrikkelnummer> paginateAsc(Moetesak moetesak, String pivot, Pageable pageable);

  @Query(
      "SELECT m FROM Matrikkelnummer m WHERE m.moetesak = :moetesak AND m.id <= COALESCE(:pivot, m.id) ORDER BY m.id DESC")
  Slice<Matrikkelnummer> paginateDesc(Moetesak moetesak, String pivot, Pageable pageable);

  @Query(
      "SELECT m FROM Matrikkelnummer m WHERE m.moetedokument = :moetedokument AND m.id >= COALESCE(:pivot, m.id) ORDER BY m.id ASC")
  Slice<Matrikkelnummer> paginateAsc(Moetedokument moetedokument, String pivot, Pageable pageable);

  @Query(
      "SELECT m FROM Matrikkelnummer m WHERE m.moetedokument = :moetedokument AND m.id <= COALESCE(:pivot, m.id) ORDER BY m.id DESC")
  Slice<Matrikkelnummer> paginateDesc(Moetedokument moetedokument, String pivot, Pageable pageable);

  Optional<Matrikkelnummer>
      findBySaksmappeAndKommunenummerAndGaardsnummerAndBruksnummerAndFestenummerAndSeksjonsnummer(
          Saksmappe saksmappe,
          String kommunenummer,
          Integer gaardsnummer,
          Integer bruksnummer,
          Integer festenummer,
          Integer seksjonsnummer);

  Optional<Matrikkelnummer>
      findByMoetemappeAndKommunenummerAndGaardsnummerAndBruksnummerAndFestenummerAndSeksjonsnummer(
          Moetemappe moetemappe,
          String kommunenummer,
          Integer gaardsnummer,
          Integer bruksnummer,
          Integer festenummer,
          Integer seksjonsnummer);

  Optional<Matrikkelnummer>
      findByJournalpostAndKommunenummerAndGaardsnummerAndBruksnummerAndFestenummerAndSeksjonsnummer(
          Journalpost journalpost,
          String kommunenummer,
          Integer gaardsnummer,
          Integer bruksnummer,
          Integer festenummer,
          Integer seksjonsnummer);

  Optional<Matrikkelnummer>
      findByMoetesakAndKommunenummerAndGaardsnummerAndBruksnummerAndFestenummerAndSeksjonsnummer(
          Moetesak moetesak,
          String kommunenummer,
          Integer gaardsnummer,
          Integer bruksnummer,
          Integer festenummer,
          Integer seksjonsnummer);

  Optional<Matrikkelnummer>
      findByMoetedokumentAndKommunenummerAndGaardsnummerAndBruksnummerAndFestenummerAndSeksjonsnummer(
          Moetedokument moetedokument,
          String kommunenummer,
          Integer gaardsnummer,
          Integer bruksnummer,
          Integer festenummer,
          Integer seksjonsnummer);
}
