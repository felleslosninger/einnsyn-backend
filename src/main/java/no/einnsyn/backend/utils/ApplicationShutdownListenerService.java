package no.einnsyn.backend.utils;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApplicationShutdownListenerService implements ApplicationListener<ContextClosedEvent> {

  private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

  @Override
  public void onApplicationEvent(ContextClosedEvent event) {
    log.info("ContextClosedEvent received. Application is shutting down.");
    this.shuttingDown.set(true);
  }

  public boolean isShuttingDown() {
    return this.shuttingDown.get();
  }
}
