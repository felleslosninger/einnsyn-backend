package no.einnsyn.apiv3.tasks;

import java.time.Instant;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.einnsyn.apiv3.entities.innsynskrav.InnsynskravRepository;
import no.einnsyn.apiv3.entities.innsynskrav.InnsynskravSenderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InnsynskravScheduler {

  InnsynskravRepository innsynskravRepository;

  InnsynskravSenderService innsynskravSenderService;

  @Value("${application.innsynskravRetryInterval}")
  private int retryInterval;

  public InnsynskravScheduler(
      InnsynskravRepository innsynskravRepository,
      InnsynskravSenderService innsynskravSenderService) {
    this.innsynskravRepository = innsynskravRepository;
    this.innsynskravSenderService = innsynskravSenderService;
  }

  @SchedulerLock(name = "UpdateOutdatedEs", lockAtLeastFor = "10m", lockAtMostFor = "10m")
  @Scheduled(fixedDelayString = "${application.innsynskravRetryInterval}")
  @Transactional
  public void sendUnsentInnsynskrav() {

    // Get an instant from previous interval
    var currentTimeMinus1Interval = Instant.now().minusMillis(retryInterval);
    var innsynskravStream = innsynskravRepository.findFailedSendings(currentTimeMinus1Interval);
    innsynskravStream.forEach(innsynskravSenderService::sendInnsynskrav);
  }
}
