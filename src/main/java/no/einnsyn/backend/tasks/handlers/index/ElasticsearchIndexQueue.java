package no.einnsyn.backend.tasks.handlers.index;

import java.util.LinkedHashMap;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * This class keeps a queue of objects that need to be indexed in Elasticsearch. It is used to avoid
 * indexing the same object multiple times in the same request, and they are all batched to be
 * indexed at the end of the request.
 */
@Slf4j
@Component
@RequestScope
public class ElasticsearchIndexQueue {

  private final JournalpostService journalpostService;
  private final SaksmappeService saksmappeService;
  private final MoetemappeService moetemappeService;
  private final MoetesakService moetesakService;
  private final InnsynskravService innsynskravService;
  private final LagretSoekService lagretSoekService;

  private final Map<String, Class<? extends Base>> queueMap = new LinkedHashMap<>();

  public ElasticsearchIndexQueue(
      JournalpostService journalpostService,
      SaksmappeService saksmappeService,
      MoetemappeService moetemappeService,
      MoetesakService moetesakService,
      InnsynskravService innsynskravService,
      LagretSoekService lagretSoekService) {
    this.journalpostService = journalpostService;
    this.saksmappeService = saksmappeService;
    this.moetemappeService = moetemappeService;
    this.moetesakService = moetesakService;
    this.innsynskravService = innsynskravService;
    this.lagretSoekService = lagretSoekService;
  }

  public void add(Base obj) {
    var clazz = obj.getClass();
    var id = obj.getId();
    queueMap.put(id, clazz);
  }

  @Async("requestSideEffectExecutor")
  public void execute() {
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
