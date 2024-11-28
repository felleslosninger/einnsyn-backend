package no.einnsyn.backend.tasks;

import no.einnsyn.backend.tasks.handlers.subscription.SubscriptionScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("test")
public class LagretSakSoekSubscriptionTestController {

  @Autowired SubscriptionScheduler subscriptionScheduler;

  @PostMapping("/lagretSakTest/notifyLagretSak")
  public void notifyLagretSak() {
    subscriptionScheduler.notifyLagretSak();
  }

  @PostMapping("/lagretSakTest/notifyLagretSoek")
  public void notifyLagretSoek() {
    subscriptionScheduler.notifyLagretSoek();
  }
}
