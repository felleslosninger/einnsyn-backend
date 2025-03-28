package no.einnsyn.backend.tasks.handlers.innsynskrav;

import java.time.Instant;
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

  public InnsynskravScheduler(
      InnsynskravBestillingRepository innsynskravBestillingRepository,
      InnsynskravSenderService innsynskravSenderService) {
    this.innsynskravBestillingRepository = innsynskravBestillingRepository;
    this.innsynskravSenderService = innsynskravSenderService;
  }

  @SchedulerLock(name = "UpdateOutdatedEs", lockAtLeastFor = "10m", lockAtMostFor = "10m")
  @Scheduled(fixedDelayString = "${application.innsynskravRetryInterval}")
  @Transactional(rollbackFor = Exception.class)
  public void sendUnsentInnsynskrav() {
    // Get an instant from previous interval
    var currentTimeMinus1Interval = Instant.now().minusMillis(retryInterval);
    try (var innsynskravBestillingStream =
        innsynskravBestillingRepository.findFailedSendings(currentTimeMinus1Interval)) {
      innsynskravBestillingStream.forEach(innsynskravSenderService::sendInnsynskravBestilling);
    }
  }
}
