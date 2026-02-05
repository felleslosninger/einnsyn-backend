package no.einnsyn.backend.configuration;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class AsyncConfiguration {

  @Bean
  SecurityContextTaskDecorator securityContextTaskDecorator() {
    return new SecurityContextTaskDecorator();
  }

  /**
   * Request side effects may require the request's security context (indexing of hidden objects
   * etc.). Therefore, we use SecurityContextTaskDecorator to propagate the request's security
   * context to the side effect executor.
   *
   * @param securityContextTaskDecorator the task decorator to propagate security context
   * @return the configured executor
   */
  @Bean(name = "requestSideEffectExecutor")
  Executor taskExecutor(SecurityContextTaskDecorator securityContextTaskDecorator) {
    var executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("EInnsyn-RequestSideEffect-");
    executor.setTaskDecorator(securityContextTaskDecorator);
    executor.initialize();
    return executor;
  }

  static class SecurityContextTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
      var securityContext = SecurityContextHolder.getContext();
      return () -> {
        try {
          SecurityContextHolder.setContext(securityContext);
          runnable.run();
        } finally {
          SecurityContextHolder.clearContext();
        }
      };
    }
  }
}
