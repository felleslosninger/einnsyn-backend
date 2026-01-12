package no.einnsyn.backend.common.statistics;

import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.NoSuchElementException;
import no.einnsyn.backend.entities.innsynskrav.InnsynskravRepository;
import no.einnsyn.backend.entities.journalpost.JournalpostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatisticsTestService {

  @Autowired private JournalpostRepository journalpostRepository;
  @Autowired private InnsynskravRepository innsynskravRepository;

  private static Instant plus(Instant instant, long amountToAdd, TemporalUnit unit) {
    try {
      return instant.plus(amountToAdd, unit);
    } catch (UnsupportedTemporalTypeException _) {
      // Instant only supports time-based units up to DAYS. Tests sometimes need calendar-based
      // units like MONTHS/YEARS to simulate older data.
      return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC).plus(amountToAdd, unit).toInstant();
    }
  }

  @Transactional
  public void modifyInnsynskravCreatedDate(String ikId, int change, TemporalUnit timeUnit)
      throws NoSuchElementException {
    var innsynskrav = innsynskravRepository.findById(ikId).orElseThrow();
    innsynskrav.setCreated(plus(innsynskrav.getCreated(), change, timeUnit));
    innsynskrav.setUpdated(Instant.now());
    innsynskravRepository.save(innsynskrav);
  }

  @Transactional
  public void modifyJournalpostCreatedDate(String jpId, int change, TemporalUnit timeUnit)
      throws NoSuchElementException {
    var journalpost = journalpostRepository.findById(jpId).orElseThrow();
    journalpost.setCreated(plus(journalpost.getCreated(), change, timeUnit));
    journalpost.setUpdated(Instant.now());
    journalpostRepository.save(journalpost);
  }
}
