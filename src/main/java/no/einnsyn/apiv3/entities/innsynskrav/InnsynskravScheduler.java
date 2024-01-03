package no.einnsyn.apiv3.entities.innsynskrav;

import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.stream.Stream;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
      fixedDelayString = "#{${application.innsynskravRetryInterval}}",
      initialDelayString =
          "#{T(java.lang.Math).round(T(java.lang.Math).random() * ${application.innsynskravRetryInterval})}")
  @Transactional
  void sendUnsentInnsynskrav() {

    // Get an instant from previous interval
    Instant currentTimeMinus1Interval = Instant.now().minusMillis(retryInterval);
    Stream<Innsynskrav> innsynskravStream =
        innsynskravRepository.findFailedSendings(currentTimeMinus1Interval);

    innsynskravStream.forEach(innsynskravSenderService::sendInnsynskrav);
  }
}
