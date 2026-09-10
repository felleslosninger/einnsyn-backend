package no.einnsyn.backend.tasks;

import no.einnsyn.backend.tasks.handlers.reindex.ElasticsearchReindexScheduler;
import no.einnsyn.backend.tasks.handlers.reindex.ElasticsearchRemoveStaleScheduler;
import no.einnsyn.backend.tasks.handlers.subscription.SubscriptionScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Triggers scheduled tasks from tests. The schedulers normally run outside a web request, where the
 * accessibility filters are disabled. Clear the request context before invoking them so the filter
 * aspect behaves as it does in production. Spring restores the context after the request.
 */
@RestController
@Profile("test")
public class TaskTestController {

  @Autowired SubscriptionScheduler subscriptionScheduler;
  @Autowired ElasticsearchReindexScheduler elasticsearchReindexScheduler;
  @Autowired ElasticsearchRemoveStaleScheduler elasticsearchRemoveStaleScheduler;

  @PostMapping("/lagretSakTest/notifyLagretSak")
  public void notifyLagretSak() {
    RequestContextHolder.resetRequestAttributes();
    subscriptionScheduler.notifyLagretSak();
  }

  @PostMapping("/lagretSoekTest/notifyLagretSoek")
  public void notifyLagretSoek() {
    RequestContextHolder.resetRequestAttributes();
    subscriptionScheduler.notifyLagretSoek();
  }

  @PostMapping("/updateOutdatedDocuments")
  public void reindex() {
    RequestContextHolder.resetRequestAttributes();
    elasticsearchReindexScheduler.reindexOutdatedDocuments();
  }

  @PostMapping("/removeStaleDocuments")
  public void removeStaleDocuments() {
    RequestContextHolder.resetRequestAttributes();
    elasticsearchRemoveStaleScheduler.removeStaleDocuments();
  }
}
