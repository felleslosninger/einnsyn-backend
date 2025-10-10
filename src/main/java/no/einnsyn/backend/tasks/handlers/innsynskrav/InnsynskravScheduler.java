package no.einnsyn.backend.tasks.handlers.innsynskrav;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.einnsyn.backend.entities.innsynskravbestilling.InnsynskravBestillingRepository;
import no.einnsyn.backend.entities.innsynskravbestilling.InnsynskravSenderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InnsynskravScheduler {

  InnsynskravBestillingRepository innsynskravBestillingRepository;

  InnsynskravSenderService innsynskravSenderService;

  @Value("${application.innsynskravRetryInterval}")
  private int retryInterval;

  @Value("${application.innsynskravAnonymousMaxAge}")
  int anonymousMaxAge;

  public InnsynskravScheduler(
      InnsynskravBestillingRepository innsynskravBestillingRepository,
      InnsynskravSenderService innsynskravSenderService) {
    this.innsynskravBestillingRepository = innsynskravBestillingRepository;
    this.innsynskravSenderService = innsynskravSenderService;
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

  @SchedulerLock(name = "cleanOldInnsynskrav", lockAtLeastFor = "1m")
  @Scheduled(cron = "0 0 0 * * *")
  @Transactional(rollbackFor = Exception.class)
  public void cleanOldInnsynskravBestillings() {
    // Guest-users: find all bestillings where email is not null, created more than
    // ${anonymousMaxAge} days ago and bruker__id is null
    var OldBestillings =
        innsynskravBestillingRepository.streamAllByCreatedBeforeAndEpostIsNotNullAndBrukerIsNull(
            Instant.now().minus(anonymousMaxAge, ChronoUnit.DAYS));

    OldBestillings.forEach(
        innsynskravBestilling -> {
          innsynskravBestilling.setEpost(null);
          innsynskravBestillingRepository.save(innsynskravBestilling);
        });
  }
}
