package no.einnsyn.backend.entities.innsynskravbestilling;

import static org.mockito.Mockito.mock;

import net.javacrumbs.shedlock.core.LockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
public class LocalSchedulingConfig {

  @Bean
  public LockProvider lockProvider() {
    return mock(LockProvider.class);
  }
}
