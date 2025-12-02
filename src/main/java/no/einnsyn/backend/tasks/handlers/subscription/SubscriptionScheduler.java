package no.einnsyn.backend.tasks.handlers.subscription;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.einnsyn.backend.entities.lagretsak.LagretSakRepository;
import no.einnsyn.backend.entities.lagretsak.LagretSakService;
import no.einnsyn.backend.entities.lagretsoek.LagretSoekRepository;
import no.einnsyn.backend.entities.lagretsoek.LagretSoekService;
import no.einnsyn.backend.utils.ApplicationShutdownListenerService;
import no.einnsyn.backend.utils.ShedlockExtenderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SubscriptionScheduler {

  private static final int LOCK_EXTEND_INTERVAL = 60 * 1000; // 1 minute

  private final LagretSakService lagretSakService;
  private final LagretSakRepository lagretSakRepository;
  private final LagretSoekService lagretSoekService;
  private final LagretSoekRepository lagretSoekRepository;
  private final ShedlockExtenderService shedlockExtenderService;
  private final ApplicationShutdownListenerService applicationShutdownListenerService;

  public SubscriptionScheduler(
      LagretSakService lagretSakService,
      LagretSakRepository lagretSakRepository,
      LagretSoekService lagretSoekService,
      LagretSoekRepository lagretSoekRepository,
      ShedlockExtenderService shedlockExtenderService,
      ApplicationShutdownListenerService applicationShutdownListenerService) {
    this.lagretSakService = lagretSakService;
    this.lagretSakRepository = lagretSakRepository;
    this.lagretSoekService = lagretSoekService;
    this.lagretSoekRepository = lagretSoekRepository;
    this.shedlockExtenderService = shedlockExtenderService;
    this.applicationShutdownListenerService = applicationShutdownListenerService;
  }

  // Notify lagretSak every ten minutes
  @Scheduled(cron = "${application.lagretSak.notificationSchedule:0 */10 * * * *}")
  @SchedulerLock(name = "NotifyLagretSak", lockAtLeastFor = "1m")
  @Transactional(readOnly = true)
  public void notifyLagretSak() {
    var lastExtended = System.currentTimeMillis();
    try (var matchingSak = lagretSakRepository.streamIdWithHits()) {
      var matchingSakIdIterator = matchingSak.iterator();
      log.debug("Notify matching lagretSak");
      while (matchingSakIdIterator.hasNext()) {
        if (applicationShutdownListenerService.isShuttingDown()) {
          log.warn("Application is shutting down. Aborting lagretSak notifications.");
          break;
        }

        var sakId = matchingSakIdIterator.next();
        log.info("Notifying lagretSak {}", sakId);
        lagretSakService.notifyLagretSak(sakId);
        lastExtended = shedlockExtenderService.maybeExtendLock(lastExtended, LOCK_EXTEND_INTERVAL);
      }
    }
  }

  // Notify lagretSoek daily
  @Scheduled(
      cron = "${application.lagretSoek.notificationSchedule:0 0 6 * * *}",
      zone = "Europe/Oslo")
  @SchedulerLock(name = "NotifyLagretSoek", lockAtLeastFor = "1m")
  @Transactional(readOnly = true)
  public void notifyLagretSoek() {
    var lastExtended = System.currentTimeMillis();
    try (var matchingSoekBrukerId = lagretSoekRepository.streamBrukerIdWithLagretSoekHits()) {
      var matchingSoekBrukerIdIterator = matchingSoekBrukerId.iterator();
      log.info("Notify matching lagretSoek");
      while (matchingSoekBrukerIdIterator.hasNext()) {
        if (applicationShutdownListenerService.isShuttingDown()) {
          log.warn("Application is shutting down. Aborting lagretSoek notifications.");
          break;
        }

        var brukerId = matchingSoekBrukerIdIterator.next();
        log.debug("Notifying lagretSoek for bruker {}", brukerId);
        lagretSoekService.notifyLagretSoek(brukerId);
        lastExtended = shedlockExtenderService.maybeExtendLock(lastExtended, LOCK_EXTEND_INTERVAL);
      }
    }
  }
}
