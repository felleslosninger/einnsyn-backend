package no.einnsyn.backend.testutils;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.Executor;
import no.einnsyn.backend.utils.ParallelRunner;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class SideEffectService {

  /**
   * Awaitility's default poll interval is 100ms, which rounds every wait up to a 100ms boundary.
   * This wait is paid after every REST call in the suite, so that adds up.
   */
  private static final Duration POLL_INTERVAL = Duration.ofMillis(5);

  @Autowired
  @Qualifier("requestSideEffectExecutor")
  private Executor sideEffectExecutor;

  private ThreadPoolTaskExecutor sideEffectTaskExecutor;

  @PostConstruct
  void init() {
    if (sideEffectExecutor instanceof ThreadPoolTaskExecutor taskExecutor) {
      sideEffectTaskExecutor = taskExecutor;
    } else {
      throw new IllegalStateException(
          "requestSideEffectExecutor is not a ThreadPoolTaskExecutor: "
              + sideEffectExecutor.getClass().getName());
    }
  }

  public void awaitSideEffects() {
    Awaitility.await()
        .atMost(Duration.ofSeconds(10))
        .pollDelay(Duration.ZERO)
        .pollInterval(POLL_INTERVAL)
        .until(
            () -> {
              var queuedTaskCount = ParallelRunner.getGlobalQueuedTaskCount();
              var activeCount = sideEffectTaskExecutor.getActiveCount();
              var isQueueEmpty =
                  sideEffectTaskExecutor.getThreadPoolExecutor().getQueue().isEmpty();
              return queuedTaskCount == 0 && activeCount == 0 && isQueueEmpty;
            });
  }
}
