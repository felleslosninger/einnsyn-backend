package no.einnsyn.backend.tasks;

import no.einnsyn.backend.tasks.handlers.reindex.ElasticsearchReindexScheduler;
import no.einnsyn.backend.tasks.handlers.reindex.ElasticsearchRemoveStaleScheduler;
import no.einnsyn.backend.tasks.handlers.subscription.SubscriptionScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("test")
public class TaskTestController {

  @Autowired SubscriptionScheduler subscriptionScheduler;
  @Autowired ElasticsearchReindexScheduler elasticsearchReindexScheduler;
  @Autowired ElasticsearchRemoveStaleScheduler elasticsearchRemoveStaleScheduler;

  @PostMapping("/lagretSakTest/notifyLagretSak")
  public void notifyLagretSak() {
    subscriptionScheduler.notifyLagretSak();
  }

  @PostMapping("/lagretSakTest/notifyLagretSoek")
  public void notifyLagretSoek() {
    subscriptionScheduler.notifyLagretSoek();
  }

  @PostMapping("/updateOutdatedDocuments")
  public void reindex() {
    elasticsearchReindexScheduler.reindexOutdatedDocuments();
  }

  @PostMapping("/removeStaleDocuments")
  public void removeStaleDocuments() {
    elasticsearchRemoveStaleScheduler.removeStaleDocuments();
  }
}
