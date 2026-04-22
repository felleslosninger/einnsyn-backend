package no.einnsyn.backend.configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.ExtensibleLockProvider;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.support.KeepAliveLockProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
@Profile("!test")
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class SchedulingConfiguration {
  @Bean(name = "taskScheduler")
  ThreadPoolTaskScheduler taskScheduler(
      @Value("${spring.task.scheduling.pool.size:8}") int poolSize) {
    var scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(poolSize);
    scheduler.setThreadNamePrefix("scheduled-task-");
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    scheduler.setAwaitTerminationSeconds(30);
    scheduler.setRemoveOnCancelPolicy(true);
    return scheduler;
  }

  @Bean(destroyMethod = "shutdown")
  ScheduledExecutorService shedLockKeepAliveExecutor() {
    return Executors.newSingleThreadScheduledExecutor(
        Thread.ofPlatform().name("shedlock-keepalive-", 0).factory());
  }

  @Bean
  public LockProvider lockProvider(
      DataSource dataSource, ScheduledExecutorService shedLockKeepAliveExecutor) {
    ExtensibleLockProvider provider =
        new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime()
                .build());
    return new KeepAliveLockProvider(provider, shedLockKeepAliveExecutor);
  }
}
