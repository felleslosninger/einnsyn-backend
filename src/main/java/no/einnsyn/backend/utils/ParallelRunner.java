package no.einnsyn.backend.utils;

import io.micrometer.context.ContextSnapshotFactory;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ParallelRunner {

  private final Semaphore semaphore;
  private final ContextSnapshotFactory contextSnapshotFactory =
      ContextSnapshotFactory.builder().build();

  public ParallelRunner(int concurrency) {
    this.semaphore = new Semaphore(concurrency);
  }

  public CompletableFuture<Void> run(Runnable runnable) {
    var future = new CompletableFuture<Void>();

    try {
      semaphore.acquire();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      future.completeExceptionally(e);
      return future;
    }

    // Get a copy of the current context, which will be used in the new thread to get the same trace
    // ids, authentication etc.
    var contextSnapshot = this.contextSnapshotFactory.captureAll();

    Thread.ofVirtual()
        .name("parallelRunner")
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
              }
            });

    return future;
  }
}
