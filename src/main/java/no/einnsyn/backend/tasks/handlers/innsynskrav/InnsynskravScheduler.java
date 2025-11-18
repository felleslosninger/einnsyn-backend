package no.einnsyn.backend.tasks.handlers.innsynskrav;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.entities.innsynskravbestilling.InnsynskravBestillingRepository;
import no.einnsyn.backend.entities.innsynskravbestilling.InnsynskravBestillingService;
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
  private final InnsynskravBestillingRepository innsynskravBestillingRepository;
  private final InnsynskravSenderService innsynskravSenderService;
  private final InnsynskravBestillingService innsynskravBestillingService;

  @Value("${application.innsynskravRetryInterval}")
  private int retryInterval;

  @Value("${application.innsynskravAnonymousMaxAge}")
  private int anonymousMaxAge;

  public InnsynskravScheduler(
      ApplicationShutdownListenerService applicationShutdownListenerService,
      InnsynskravBestillingRepository innsynskravBestillingRepository,
      InnsynskravSenderService innsynskravSenderService,
      InnsynskravBestillingService innsynskravBestillingService1) {
    this.applicationShutdownListenerService = applicationShutdownListenerService;
    this.innsynskravBestillingRepository = innsynskravBestillingRepository;
    this.innsynskravSenderService = innsynskravSenderService;
    this.innsynskravBestillingService = innsynskravBestillingService1;
  }

  @SchedulerLock(name = "SendUnsentInnsynskrav", lockAtLeastFor = "1m")
  @Scheduled(fixedDelayString = "${application.innsynskravRetryInterval}")
  @Transactional(rollbackFor = Exception.class)
  public void sendUnsentInnsynskrav() {
    // Get an instant from previous interval
    var currentTimeMinus1Interval = Instant.now().minusMillis(retryInterval);
    try (var innsynskravBestillingStream =
        innsynskravBestillingRepository.streamFailedSendings(currentTimeMinus1Interval)) {
      var innsynskravBestillingIterator = innsynskravBestillingStream.iterator();
      while (innsynskravBestillingIterator.hasNext()) {
        if (applicationShutdownListenerService.isShuttingDown()) {
          log.warn("Application is shutting down. Aborting sending of unsent Innsynskrav.");
          return;
        }
        innsynskravSenderService.sendInnsynskravBestilling(innsynskravBestillingIterator.next());
      }
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
        innsynskravBestillingRepository.streamIdsWithoutUserOlderThan(
            Instant.now().minus(anonymousMaxAge, ChronoUnit.DAYS))) {
      log.info("Starting deletion of old InnsynskravBestilling.");
      var oldBestillingIterator = oldBestillingStream.iterator();
      var count = 0;
      while (oldBestillingIterator.hasNext()) {
        var innsynskravBestilling = oldBestillingIterator.next();
        if (applicationShutdownListenerService.isShuttingDown()) {
          log.warn("Application is shutting down. Aborting deletion of old InnsynskravBestilling.");
          return;
        }
        innsynskravBestillingService.deleteInNewTransaction(innsynskravBestilling);
        count++;

        log.debug("deleted InnsynskravBestilling: {}", innsynskravBestilling);
        if (count % 1000 == 0 || !oldBestillingIterator.hasNext()) {
          log.info("Deleted {} InnsynskravBestilling.", count);
        }
      }
      log.info("Finished deleting a total of {} old InnsynskravBestilling.", count);
    } catch (EInnsynException e) {
      log.error("Unable to delete old InnsynskravBestilling. Error: {}", e.getMessage(), e);
    }
  }
}
