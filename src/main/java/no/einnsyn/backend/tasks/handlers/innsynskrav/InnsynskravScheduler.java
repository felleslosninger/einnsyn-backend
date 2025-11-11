package no.einnsyn.backend.tasks.handlers.innsynskrav;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.einnsyn.backend.entities.innsynskrav.InnsynskravRepository;
import no.einnsyn.backend.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.backend.entities.innsynskravbestilling.InnsynskravBestillingRepository;
import no.einnsyn.backend.entities.innsynskravbestilling.InnsynskravSenderService;
import no.einnsyn.backend.utils.ApplicationShutdownListenerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class InnsynskravScheduler {

  private final ApplicationShutdownListenerService applicationShutdownListenerService;
  private final InnsynskravRepository innsynskravRepository;
  private final InnsynskravBestillingRepository innsynskravBestillingRepository;
  private final InnsynskravSenderService innsynskravSenderService;

  @Value("${application.innsynskravRetryInterval}")
  private int retryInterval;

  @Value("${application.innsynskravAnonymousMaxAge}")
  private int anonymousMaxAge;

  public InnsynskravScheduler(
      InnsynskravBestillingRepository innsynskravBestillingRepository,
      InnsynskravSenderService innsynskravSenderService,
      InnsynskravRepository innsynskravRepository,
      ApplicationShutdownListenerService applicationShutdownListenerService) {
    this.innsynskravBestillingRepository = innsynskravBestillingRepository;
    this.innsynskravSenderService = innsynskravSenderService;
    this.innsynskravRepository = innsynskravRepository;
    this.applicationShutdownListenerService = applicationShutdownListenerService;
  }

  @SchedulerLock(name = "SendUnsentInnsynskrav", lockAtLeastFor = "1m")
  @Scheduled(fixedDelayString = "${application.innsynskravRetryInterval}")
  @Transactional(rollbackFor = Exception.class)
  public void sendUnsentInnsynskrav() {
    // Get an instant from previous interval
    var currentTimeMinus1Interval = Instant.now().minusMillis(retryInterval);
    try (var innsynskravBestillingStream =
        innsynskravBestillingRepository.streamFailedSendings(currentTimeMinus1Interval)) {
      if (applicationShutdownListenerService.isShuttingDown()) {
        log.warn("Application is shutting down. Aborting sending of unsent Innsynskrav.");
        return;
      }
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
  @Scheduled(cron = "${application.innsynskravCleanSchedule}")
  @Transactional(rollbackFor = Exception.class)
  public void deleteOldInnsynskravBestilling() {
    // Guest-users: find all Bestilling where email is not null, created more than
    // ${anonymousMaxAge} days ago and bruker__id is null
    try (var oldBestillingStream =
        innsynskravBestillingRepository.streamAllByCreatedBeforeAndEpostIsNotNullAndBrukerIsNull(
            Instant.now().minus(anonymousMaxAge, ChronoUnit.DAYS))) {

      oldBestillingStream.forEach(
          innsynskravBestilling -> {
            if (applicationShutdownListenerService.isShuttingDown()) {
              log.warn(
                  "Application is shutting down. Aborting deletion of old InnsynskravBestilling.");
              return;
            }
            for (Innsynskrav innsynskrav : innsynskravBestilling.getInnsynskrav()) {
              innsynskrav.setInnsynskravBestilling(null);
              innsynskravRepository.save(innsynskrav);
            }
            innsynskravBestillingRepository.delete(innsynskravBestilling);
          });
    }
  }
}
