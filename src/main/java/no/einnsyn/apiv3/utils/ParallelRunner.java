package no.einnsyn.apiv3.utils;

import java.util.concurrent.Semaphore;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ParallelRunner {

  private final Semaphore semaphore;

  public ParallelRunner(int concurrency) {
    this.semaphore = new Semaphore(concurrency);
  }

  public void run(Runnable runnable) {
    try {
      semaphore.acquire();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    Thread.ofVirtual()
        .name("parallelRunner")
        .start(
            () -> {
              try {
                runnable.run();
              } catch (Exception e) {
                log.error("Error in parallelRunner: {}", e.getMessage(), e);
              } finally {
                semaphore.release();
              }
            });
  }
}
