package no.einnsyn.apiv3.tasks.elasticsearch;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.base.models.Base;
import no.einnsyn.apiv3.entities.journalpost.JournalpostService;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.moetemappe.MoetemappeService;
import no.einnsyn.apiv3.entities.moetemappe.models.Moetemappe;
import no.einnsyn.apiv3.entities.moetesak.MoetesakService;
import no.einnsyn.apiv3.entities.moetesak.models.Moetesak;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeService;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;
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

  private final Map<String, Class<? extends Base>> queueMap = new LinkedHashMap<>();

  public ElasticsearchIndexQueue(
      JournalpostService journalpostService,
      SaksmappeService saksmappeService,
      MoetemappeService moetemappeService,
      MoetesakService moetesakService) {
    this.journalpostService = journalpostService;
    this.saksmappeService = saksmappeService;
    this.moetemappeService = moetemappeService;
    this.moetesakService = moetesakService;
  }

  public void add(Base obj) {
    var clazz = obj.getClass();
    var id = obj.getId();
    queueMap.put(id, clazz);
  }

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
        }
      } catch (Exception e) {
        log.error(
            "Failed to index {} with id: {}: {}", clazz.getSimpleName(), id, e.getMessage(), e);
      }
    }
  }
}
