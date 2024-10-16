package no.einnsyn.apiv3.tasks.handlers.subscription;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import net.javacrumbs.shedlock.core.LockExtender;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.einnsyn.apiv3.entities.lagretsak.LagretSakRepository;
import no.einnsyn.apiv3.entities.lagretsak.LagretSakService;
import no.einnsyn.apiv3.entities.lagretsoek.LagretSoekRepository;
import no.einnsyn.apiv3.entities.lagretsoek.LagretSoekService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionScheduler {

  private static final int LOCK_EXTEND_INTERVAL = 5 * 60 * 1000; // 5 minutes

  LagretSakService lagretSakService;
  LagretSakRepository lagretSakRepository;
  LagretSoekService lagretSoekService;
  LagretSoekRepository lagretSoekRepository;

  public SubscriptionScheduler(
      LagretSakService lagretSakService,
      LagretSakRepository lagretSakRepository,
      LagretSoekService lagretSoekService,
      LagretSoekRepository lagretSoekRepository) {
    this.lagretSakService = lagretSakService;
    this.lagretSakRepository = lagretSakRepository;
    this.lagretSoekService = lagretSoekService;
    this.lagretSoekRepository = lagretSoekRepository;
  }

  public long maybeExtendLock(long lastExtended) {
    var now = System.currentTimeMillis();
    if (now - lastExtended > LOCK_EXTEND_INTERVAL) {
      LockExtender.extendActiveLock(
          Duration.of(LOCK_EXTEND_INTERVAL * 2l, ChronoUnit.MILLIS),
          Duration.of(LOCK_EXTEND_INTERVAL * 2l, ChronoUnit.MILLIS));
      return now;
    }
    return lastExtended;
  }

  // Notify lagretSak every ten minutes
  @Profile("!test")
  @Scheduled(cron = "0 */10 * * * *")
  @SchedulerLock(name = "NotifyLagretSak", lockAtLeastFor = "5m", lockAtMostFor = "5m")
  @Transactional(readOnly = true)
  public void notifyLagretSak() {
    var lastExtended = System.currentTimeMillis();
    var matchingSak = lagretSakRepository.findLagretSakWithHits();
    var matchingSakIterator = matchingSak.iterator();
    while (matchingSakIterator.hasNext()) {
      var sakId = matchingSakIterator.next();
      lagretSakService.notifyLagretSak(sakId);
      lastExtended = maybeExtendLock(lastExtended);
    }
  }

  // Notify lagretSoek daily
  @Profile("!test")
  @Scheduled(cron = "0 0 6 * * *", zone = "Europe/Oslo")
  @SchedulerLock(name = "NotifyLagretSoek", lockAtLeastFor = "10m", lockAtMostFor = "10m")
  @Transactional(readOnly = true)
  public void notifyLagretSoek() {
    var lastExtended = System.currentTimeMillis();
    var matchingSoek = lagretSoekRepository.findBrukerWithLagretSoekHits();
    var matchingSoekIterator = matchingSoek.iterator();
    while (matchingSoekIterator.hasNext()) {
      var brukerId = matchingSoekIterator.next();
      lagretSoekService.notifyLagretSoek(brukerId);
      lastExtended = maybeExtendLock(lastExtended);
    }
  }
}
