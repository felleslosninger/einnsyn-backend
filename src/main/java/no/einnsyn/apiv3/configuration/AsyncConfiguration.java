package no.einnsyn.apiv3.configuration;

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
    executor.setRejectedExecutionHandler(
        new ThreadPoolExecutor.CallerRunsPolicy()); // Block the calling thread when queue is full
    executor.setThreadNamePrefix("Index-");
    executor.initialize();
    return executor;
  }
}
