package no.einnsyn.backend.tasks.handlers.index;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.entities.base.models.Base;
import no.einnsyn.backend.entities.innsynskrav.InnsynskravService;
import no.einnsyn.backend.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.backend.entities.journalpost.JournalpostService;
import no.einnsyn.backend.entities.journalpost.models.Journalpost;
import no.einnsyn.backend.entities.lagretsoek.LagretSoekService;
import no.einnsyn.backend.entities.lagretsoek.models.LagretSoek;
import no.einnsyn.backend.entities.moetemappe.MoetemappeService;
import no.einnsyn.backend.entities.moetemappe.models.Moetemappe;
import no.einnsyn.backend.entities.moetesak.MoetesakService;
import no.einnsyn.backend.entities.moetesak.models.Moetesak;
import no.einnsyn.backend.entities.saksmappe.SaksmappeService;
import no.einnsyn.backend.entities.saksmappe.models.Saksmappe;
import no.einnsyn.backend.utils.ParallelRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * This is a HandlerInterceptor that will execute elasticsearchIndexQueue.execute() at the end of
 * each web request.
 *
 * <p>Throughout a request, various actions may add objects to the elasticsearchIndexQueue. To avoid
 * having multiple index requests happen for the same objects, these are batched together to be
 * executed at the end of the request, when we are sure all update events are done.
 */
@Component
@Slf4j
public class ElasticsearchHandlerInterceptor implements HandlerInterceptor {

  private final JournalpostService journalpostService;
  private final SaksmappeService saksmappeService;
  private final MoetemappeService moetemappeService;
  private final MoetesakService moetesakService;
  private final InnsynskravService innsynskravService;
  private final LagretSoekService lagretSoekService;

  private final ElasticsearchIndexQueue elasticsearchIndexQueue;
  private final ParallelRunner parallelRunner;

  public ElasticsearchHandlerInterceptor(
      JournalpostService journalpostService,
      SaksmappeService saksmappeService,
      MoetemappeService moetemappeService,
      MoetesakService moetesakService,
      InnsynskravService innsynskravService,
      LagretSoekService lagretSoekService,
      ElasticsearchIndexQueue elasticsearchIndexQueue) {
    this.journalpostService = journalpostService;
    this.saksmappeService = saksmappeService;
    this.moetemappeService = moetemappeService;
    this.moetesakService = moetesakService;
    this.innsynskravService = innsynskravService;
    this.lagretSoekService = lagretSoekService;
    this.elasticsearchIndexQueue = elasticsearchIndexQueue;

    parallelRunner = new ParallelRunner(10);
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
      throws Exception {
    var queueMap = elasticsearchIndexQueue.getQueueCopy();
    parallelRunner.run(() -> executeQueue(queueMap));
  }

  private void executeQueue(Map<String, Class<? extends Base>> queueMap) {
    for (var entry : queueMap.entrySet()) {
      var id = entry.getKey();
      var clazz = entry.getValue();
      try {
        if (Journalpost.class.isAssignableFrom(clazz)) {
          journalpostService.index(id);
        } else if (Saksmappe.class.isAssignableFrom(clazz)) {
          saksmappeService.index(id);
        } else if (Moetemappe.class.isAssignableFrom(clazz)) {
          moetemappeService.index(id);
        } else if (Moetesak.class.isAssignableFrom(clazz)) {
          moetesakService.index(id);
        } else if (Innsynskrav.class.isAssignableFrom(clazz)) {
          innsynskravService.index(id);
        } else if (LagretSoek.class.isAssignableFrom(clazz)) {
          lagretSoekService.index(id);
        }
      } catch (Exception e) {
        log.error(
            "Failed to index {} with id: {}: {}", clazz.getSimpleName(), id, e.getMessage(), e);
      }
    }
    queueMap.clear();
  }
}
