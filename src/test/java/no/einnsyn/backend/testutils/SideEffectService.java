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
        .pollDelay(Duration.ZERO)
        .until(
            () ->
                ParallelRunner.getGlobalQueuedTaskCount() == 0
                    && sideEffectTaskExecutor.getActiveCount() == 0
                    && sideEffectTaskExecutor.getThreadPoolExecutor().getQueue().isEmpty()
                    && Thread.getAllStackTraces().keySet().stream()
                            .filter(thread -> thread.getName().contains("parallelRunner"))
                            .count()
                        == 0);
  }
}
