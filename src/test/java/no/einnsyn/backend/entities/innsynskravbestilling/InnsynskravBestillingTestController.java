package no.einnsyn.backend.entities.innsynskravbestilling;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import no.einnsyn.backend.tasks.handlers.innsynskrav.InnsynskravScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * These helpers are implemented in controllers, to overcome synchronization issues when opening
 * some transactions directly in a test, and some in controller endpoints.
 */
@RestController
@Profile("test")
public class InnsynskravBestillingTestController {

  @Autowired private InnsynskravScheduler innsynskravScheduler;
  @Autowired private InnsynskravBestillingRepository innsynskravBestillingRepository;

  @PostMapping("/innsynskravTest/trigger")
  public void triggerInnsynskravScheduler() {
    innsynskravScheduler.sendUnsentInnsynskrav();
  }

  @GetMapping("/innsynskravTest/getVerificationSecret/{id}")
  @Transactional
  public ResponseEntity<String> getVerificationSecret(@PathVariable @NotNull String id) {
    var innsynskravBestilling = getBestilling(id);
    return ResponseEntity.ok(innsynskravBestilling.getVerificationSecret());
  }

  @GetMapping("/innsynskravTest/isSent/{id}/{delNo}")
  @Transactional
  public ResponseEntity<String> getIsSent(
      @PathVariable @NotNull String id, @PathVariable Integer delNo) {
    var innsynskravBestilling = getBestilling(id);
    var innsynskrav = innsynskravBestilling.getInnsynskrav().get(delNo);
    var sent = innsynskrav.getSent();
    if (sent == null) {
      return ResponseEntity.ok("");
    }
    return ResponseEntity.ok(sent.toString());
  }

  @GetMapping("/innsynskravTest/retryCount/{id}/{delNo}")
  @Transactional
  public ResponseEntity<Integer> getRetryCount(
      @PathVariable @NotNull String id, @PathVariable Integer delNo) {
    var innsynskravBestilling = getBestilling(id);
    var innsynskrav = innsynskravBestilling.getInnsynskrav().get(delNo);
    return ResponseEntity.ok(innsynskrav.getRetryCount());
  }

  @GetMapping("/innsynskravTest/retryCounts/{id}")
  @Transactional
  public ResponseEntity<List<Integer>> getRetryCounts(@PathVariable @NotNull String id) {
    var innsynskravBestilling = getBestilling(id);
    return ResponseEntity.ok(
        innsynskravBestilling.getInnsynskrav().stream()
            .map(innsynskrav -> innsynskrav.getRetryCount())
            .toList());
  }

  @GetMapping("/innsynskravTest/sentStates/{id}")
  @Transactional
  public ResponseEntity<List<Boolean>> getSentStates(@PathVariable @NotNull String id) {
    var innsynskravBestilling = getBestilling(id);
    return ResponseEntity.ok(
        innsynskravBestilling.getInnsynskrav().stream()
            .map(innsynskrav -> innsynskrav.getSent() != null)
            .toList());
  }

  @GetMapping("/innsynskravTest/schedulerEligible/{id}")
  @Transactional(readOnly = true)
  public ResponseEntity<Boolean> isSchedulerEligible(@PathVariable @NotNull String id) {
    try (var failedSendings =
        innsynskravBestillingRepository.streamFailedSendings(Instant.now().plusSeconds(1))) {
      return ResponseEntity.ok(
          failedSendings.anyMatch(
              innsynskravBestilling -> innsynskravBestilling.getId().equals(id)));
    }
  }

  @GetMapping("/innsynskravTest/delLegacyStatus/{id}/{delNo}")
  @Transactional
  public ResponseEntity<List<String>> getDelStatus(
      @PathVariable @NotNull String id, @PathVariable Integer delNo) {
    var innsynskravBestilling = getBestilling(id);
    var innsynskrav = innsynskravBestilling.getInnsynskrav().get(delNo);

    return ResponseEntity.ok(
        innsynskrav.getLegacyStatus().stream().map(s -> s.getStatus().name()).toList());
  }

  private no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestilling
      getBestilling(String id) {
    return innsynskravBestillingRepository
        .findById(id)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "InnsynskravBestilling not found for id " + id));
  }
}
