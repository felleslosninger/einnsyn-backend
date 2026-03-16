package no.einnsyn.backend.configuration;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

public class TrackingSimpleAsyncTaskExecutor extends SimpleAsyncTaskExecutor {

  private final AtomicInteger activeTaskCount = new AtomicInteger(0);

  public TrackingSimpleAsyncTaskExecutor(String threadNamePrefix) {
    super(threadNamePrefix);
  }

  @Override
  protected void doExecute(Runnable task) {
    activeTaskCount.incrementAndGet();
    try {
      super.doExecute(
          () -> {
            try {
              task.run();
            } finally {
              activeTaskCount.decrementAndGet();
            }
          });
    } catch (RuntimeException | Error e) {
      activeTaskCount.decrementAndGet();
      throw e;
    }
  }

  public int getActiveTaskCount() {
    return activeTaskCount.get();
  }
}
