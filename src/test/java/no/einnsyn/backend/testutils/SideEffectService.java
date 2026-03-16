package no.einnsyn.backend.testutils;

import java.time.Duration;
import no.einnsyn.backend.configuration.TrackingSimpleAsyncTaskExecutor;
import no.einnsyn.backend.utils.ParallelRunner;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class SideEffectService {

  @Autowired
  @Qualifier("requestSideEffectExecutorDelegate")
  private TrackingSimpleAsyncTaskExecutor sideEffectTaskExecutor;

  public void awaitSideEffects() {
    Awaitility.await()
        .atMost(Duration.ofSeconds(10))
        .pollDelay(Duration.ZERO)
        .until(
            () -> {
              var queuedTaskCount = ParallelRunner.getGlobalQueuedTaskCount();
              var activeTaskCount = sideEffectTaskExecutor.getActiveTaskCount();
              return queuedTaskCount == 0 && activeTaskCount == 0;
            });
  }
}
