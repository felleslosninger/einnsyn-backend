package no.einnsyn.backend.tasks.handlers.index;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.entities.innsynskrav.InnsynskravService;
import no.einnsyn.backend.entities.journalpost.JournalpostService;
import no.einnsyn.backend.entities.lagretsoek.LagretSoekService;
import no.einnsyn.backend.entities.moetemappe.MoetemappeService;
import no.einnsyn.backend.entities.moetesak.MoetesakService;
import no.einnsyn.backend.entities.saksmappe.SaksmappeService;
import no.einnsyn.backend.utils.ParallelRunner;
import no.einnsyn.backend.utils.id.IdUtils;
import org.springframework.beans.factory.annotation.Value;
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
      ElasticsearchIndexQueue elasticsearchIndexQueue,
      @Value("${application.elasticsearch.concurrency:10}") int concurrency) {
    this.journalpostService = journalpostService;
    this.saksmappeService = saksmappeService;
    this.moetemappeService = moetemappeService;
    this.moetesakService = moetesakService;
    this.innsynskravService = innsynskravService;
    this.lagretSoekService = lagretSoekService;
    this.elasticsearchIndexQueue = elasticsearchIndexQueue;
    parallelRunner = new ParallelRunner(concurrency);
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
      throws Exception {
    var queueMap = elasticsearchIndexQueue.getQueueCopy();
    parallelRunner.run(() -> executeQueue(queueMap));
  }

  private void executeQueue(Map<String, Boolean> queueMap) {
    var timestamp = Instant.now();
    for (var entry : queueMap.entrySet()) {
      var id = entry.getKey();
      var entityName = IdUtils.resolveEntity(id);
      try {
        switch (entityName) {
          case "Journalpost" -> journalpostService.index(id, timestamp);
          case "Saksmappe" -> saksmappeService.index(id, timestamp);
          case "Moetemappe" -> moetemappeService.index(id, timestamp);
          case "Moetesak" -> moetesakService.index(id, timestamp);
          case "Innsynskrav" -> innsynskravService.index(id, timestamp);
          case "LagretSoek" -> lagretSoekService.index(id, timestamp);
          default -> log.warn("Unknown entity type: {}", entityName);
        }
      } catch (Exception e) {
        log.atError()
            .setCause(e)
            .setMessage("Failed to index {} with id: {}: {}")
            .addArgument(entityName)
            .addArgument(id)
            .addArgument(e.getMessage())
            .log();
      }
    }
    queueMap.clear();
  }
}
