package no.einnsyn.backend.tasks.handlers.index;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.entities.base.models.Base;
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

  private final Map<String, Class<? extends Base>> queueMap = new LinkedHashMap<>();

  public ElasticsearchIndexQueue() {}

  public void add(Base obj) {
    var clazz = obj.getClass();
    var id = obj.getId();
    queueMap.put(id, clazz);
  }

  public Map<String, Class<? extends Base>> getQueueCopy() {
    return new LinkedHashMap<>(queueMap);
  }
}
