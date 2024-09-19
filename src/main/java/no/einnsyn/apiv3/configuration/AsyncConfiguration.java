package no.einnsyn.apiv3.configuration;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfiguration {

  @Value("${application.elasticsearch.concurrency:5}")
  private int indexConcurrency;

  @Bean(name = "indexTaskExecutor")
  TaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(indexConcurrency);
    executor.setQueueCapacity(1000);
    executor.setRejectedExecutionHandler(new BlockingQueuePolicy()); // Custom blocking handler
    executor.setThreadNamePrefix("Index-");
    executor.initialize();
    return executor;
  }

  class BlockingQueuePolicy implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
      try {
        executor.getQueue().put(r);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RejectedExecutionException(
            "Task was interrupted while waiting to be enqueued", e);
      }
    }
  }
}
