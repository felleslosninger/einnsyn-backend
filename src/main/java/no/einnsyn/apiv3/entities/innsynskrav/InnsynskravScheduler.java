package no.einnsyn.apiv3.entities.innsynskrav;

import java.time.Instant;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
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

  // Delay a random amount of time between 0 and 30 minutes, to avoid multiple pods checking at the
  // same time
  @Scheduled(
      fixedDelayString = "${application.innsynskravRetryInterval}",
      initialDelayString =
          "#{T(java.lang.Math).round(T(java.lang.Math).random() *"
              + " ${application.innsynskravRetryInterval})}")
  @Transactional(rollbackFor = EInnsynException.class)
  public void sendUnsentInnsynskrav() {

    // Get an instant from previous interval
    var currentTimeMinus1Interval = Instant.now().minusMillis(retryInterval);
    var innsynskravStream = innsynskravRepository.findFailedSendings(currentTimeMinus1Interval);
    innsynskravStream.forEach(innsynskravSenderService::sendInnsynskrav);
  }
}
