package no.einnsyn.backend.tasks.handlers.innsynskrav;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.einnsyn.backend.entities.innsynskrav.InnsynskravRepository;
import no.einnsyn.backend.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.backend.entities.innsynskravbestilling.InnsynskravBestillingRepository;
import no.einnsyn.backend.entities.innsynskravbestilling.InnsynskravSenderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InnsynskravScheduler {

  private final InnsynskravRepository innsynskravRepository;
  InnsynskravBestillingRepository innsynskravBestillingRepository;

  InnsynskravSenderService innsynskravSenderService;

  @Value("${application.innsynskravRetryInterval}")
  private int retryInterval;

  @Value("${application.innsynskravAnonymousMaxAge}")
  int anonymousMaxAge;

  public InnsynskravScheduler(
      InnsynskravBestillingRepository innsynskravBestillingRepository,
      InnsynskravSenderService innsynskravSenderService,
      InnsynskravRepository innsynskravRepository) {
    this.innsynskravBestillingRepository = innsynskravBestillingRepository;
    this.innsynskravSenderService = innsynskravSenderService;
    this.innsynskravRepository = innsynskravRepository;
  }

  @SchedulerLock(name = "SendUnsentInnsynskrav", lockAtLeastFor = "1m")
  @Scheduled(fixedDelayString = "${application.innsynskravRetryInterval}")
  @Transactional(rollbackFor = Exception.class)
  public void sendUnsentInnsynskrav() {
    // Get an instant from previous interval
    var currentTimeMinus1Interval = Instant.now().minusMillis(retryInterval);
    try (var innsynskravBestillingStream =
        innsynskravBestillingRepository.streamFailedSendings(currentTimeMinus1Interval)) {
      innsynskravBestillingStream.forEach(innsynskravSenderService::sendInnsynskravBestilling);
    }
  }

  /**
   * Deletes old InnsynskravBestilling entities that were created more than ${anonymousMaxAge} days
   * ago by guest users, defined by having a non-null email but no associated user entity. The
   * deletion process also cleans up related Innsynskrav entities by breaking their association with
   * the deleted Bestilling.
   */
  @SchedulerLock(name = "cleanOldInnsynskrav", lockAtLeastFor = "1m")
  @Scheduled(cron = "0 0 0 * * *")
  @Transactional(rollbackFor = Exception.class)
  public void deleteOldInnsynskravBestilling() {
    // Guest-users: find all Bestilling where email is not null, created more than
    // ${anonymousMaxAge} days ago and bruker__id is null
    try (var oldBestillingStream =
        innsynskravBestillingRepository.streamAllByCreatedBeforeAndEpostIsNotNullAndBrukerIsNull(
            Instant.now().minus(anonymousMaxAge, ChronoUnit.DAYS))) {

      oldBestillingStream.forEach(
          innsynskravBestilling -> {
            for (Innsynskrav innsynskrav : innsynskravBestilling.getInnsynskrav()) {
              innsynskrav.setInnsynskravBestilling(null);
              innsynskravRepository.save(innsynskrav);
            }
            innsynskravBestillingRepository.delete(innsynskravBestilling);
          });
    }
  }
}
