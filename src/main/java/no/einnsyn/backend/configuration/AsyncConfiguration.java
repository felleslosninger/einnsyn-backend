package no.einnsyn.backend.configuration;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Configuration
public class AsyncConfiguration {

  @Bean(name = "requestSideEffectExecutorDelegate", destroyMethod = "close")
  TrackingSimpleAsyncTaskExecutor requestSideEffectExecutorDelegate() {
    var executor = new TrackingSimpleAsyncTaskExecutor("EInnsyn-RequestSideEffect-");
    executor.setVirtualThreads(true);
    executor.setConcurrencyLimit(32);
    executor.setTaskTerminationTimeout(Duration.ofSeconds(30).toMillis());
    executor.setCancelRemainingTasksOnClose(false);
    return executor;
  }

  /**
   * Request side effects may require the request's security context (indexing of hidden objects
   * etc.). Therefore, we wrap the executor so each submitted task runs with the submitting thread's
   * security context.
   *
   * @param delegate the underlying executor
   * @return the configured executor
   */
  @Bean(name = "requestSideEffectExecutor")
  AsyncTaskExecutor requestSideEffectExecutor(
      @Qualifier("requestSideEffectExecutorDelegate") TrackingSimpleAsyncTaskExecutor delegate) {
    return new DelegatingSecurityContextAsyncTaskExecutor(delegate);
  }
}
