package no.einnsyn.apiv3.entities.innsynskrav;

import jakarta.validation.constraints.NotNull;
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
public class InnsynskravTestController {

  @Autowired private InnsynskravScheduler innsynskravScheduler;
  @Autowired private InnsynskravRepository innsynskravRepository;

  @PostMapping("/innsynskravTest/trigger")
  public void triggerInnsynskravScheduler() {
    innsynskravScheduler.sendUnsentInnsynskrav();
  }

  @GetMapping("/innsynskravTest/getVerificationSecret/{id}")
  @Transactional
  public ResponseEntity<String> getVerificationSecret(@PathVariable @NotNull String id) {
    var innsynskrav = innsynskravRepository.findById(id).orElse(null);
    return ResponseEntity.ok(innsynskrav.getVerificationSecret());
  }

  @GetMapping("/innsynskravTest/isSent/{id}/{delNo}")
  @Transactional
  public ResponseEntity<String> getIsSent(
      @PathVariable @NotNull String id, @PathVariable Integer delNo) {
    var innsynskrav = innsynskravRepository.findById(id).orElse(null);
    var innsynskravDel = innsynskrav.getInnsynskravDel().get(delNo);
    var sent = innsynskravDel.getSent();
    if (sent == null) {
      return ResponseEntity.ok("");
    }
    return ResponseEntity.ok(sent.toString());
  }
}
