package no.einnsyn.backend.tasks.handlers.index;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
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

  private final Map<String, String> queueMap = new LinkedHashMap<>();

  public ElasticsearchIndexQueue() {}

  public void add(String entityName, String id) {
    var queueEntry = entityName;
    queueMap.put(id, queueEntry);
  }

  public Map<String, String> getQueueCopy() {
    return new LinkedHashMap<>(queueMap);
  }
}
