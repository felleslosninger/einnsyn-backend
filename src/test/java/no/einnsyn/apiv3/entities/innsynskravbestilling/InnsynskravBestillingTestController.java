package no.einnsyn.apiv3.entities.innsynskravbestilling;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import no.einnsyn.apiv3.tasks.handlers.innsynskrav.InnsynskravScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

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
    var innsynskravBestilling = innsynskravBestillingRepository.findById(id).orElse(null);
    return ResponseEntity.ok(innsynskravBestilling.getVerificationSecret());
  }

  @GetMapping("/innsynskravTest/isSent/{id}/{delNo}")
  @Transactional
  public ResponseEntity<String> getIsSent(
      @PathVariable @NotNull String id, @PathVariable Integer delNo) {
    var innsynskravBestilling = innsynskravBestillingRepository.findById(id).orElse(null);
    var innsynskrav = innsynskravBestilling.getInnsynskrav().get(delNo);
    var sent = innsynskrav.getSent();
    if (sent == null) {
      return ResponseEntity.ok("");
    }
    return ResponseEntity.ok(sent.toString());
  }

  @GetMapping("/innsynskravTest/delLegacyStatus/{id}/{delNo}")
  @Transactional
  public ResponseEntity<List<String>> getDelStatus(
      @PathVariable @NotNull String id, @PathVariable Integer delNo) {
    var innsynskravBestilling = innsynskravBestillingRepository.findById(id).orElse(null);
    var innsynskrav = innsynskravBestilling.getInnsynskrav().get(delNo);

    return ResponseEntity.ok(
        innsynskrav.getLegacyStatus().stream().map(s -> s.getStatus().name()).toList());
  }
}
