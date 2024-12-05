package no.einnsyn.backend.configuration;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfiguration {

  @Bean(name = "requestSideEffectExecutor")
  Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("EInnsyn-RequestSideEffect-");
    executor.initialize();
    return executor;
  }
}
