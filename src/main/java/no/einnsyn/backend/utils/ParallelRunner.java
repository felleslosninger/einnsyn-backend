package no.einnsyn.backend.utils;

import io.micrometer.context.ContextSnapshotFactory;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ParallelRunner {

  private static final AtomicInteger globalQueuedTaskCount = new AtomicInteger(0);
  private static final AtomicInteger instanceCounter = new AtomicInteger(0);
  private static final AtomicLong threadNameCounter = new AtomicLong(0);

  private final Semaphore semaphore;
  private final ContextSnapshotFactory contextSnapshotFactory =
      ContextSnapshotFactory.builder().build();

  public ParallelRunner(int concurrency) {
    this.semaphore = new Semaphore(concurrency);
    instanceCounter.incrementAndGet();
  }

  public CompletableFuture<Void> run(Runnable runnable) {
    globalQueuedTaskCount.incrementAndGet();
    var future = new CompletableFuture<Void>();

    try {
      semaphore.acquire();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      future.completeExceptionally(e);
      globalQueuedTaskCount.decrementAndGet();
      return future;
    }

    // Get a copy of the current context, which will be used in the new thread to get the same trace
    // ids, authentication etc.
    var contextSnapshot = this.contextSnapshotFactory.captureAll();
    var threadName =
        "parallelRunner-" + instanceCounter.get() + "-" + threadNameCounter.incrementAndGet();

    Thread.ofVirtual()
        .name(threadName)
        .start(
            () -> {
              // Set the context snapshot for the current thread
              try (var scope = contextSnapshot.setThreadLocals()) {
                runnable.run();
                future.complete(null);
              } catch (Exception e) {
                log.error("Error in parallelRunner: {}", e.getMessage(), e);
                future.completeExceptionally(e);
              } finally {
                semaphore.release();
                globalQueuedTaskCount.decrementAndGet();
              }
            });

    return future;
  }

  public static int getGlobalQueuedTaskCount() {
    return globalQueuedTaskCount.get();
  }
}
