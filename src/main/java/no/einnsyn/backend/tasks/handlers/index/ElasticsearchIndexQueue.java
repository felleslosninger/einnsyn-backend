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

  private final Map<String, Boolean> indexWithChildren = new LinkedHashMap<>();
  private final Map<String, Boolean> indexWithParents = new LinkedHashMap<>();

  public ElasticsearchIndexQueue() {}

  public void add(String id, int direction) {
    if (direction == 0) {
      indexWithChildren.put(id, true);
      indexWithParents.put(id, true);
    } else if (direction == 1) {
      indexWithChildren.put(id, true);
    } else if (direction == -1) {
      indexWithParents.put(id, true);
    }
  }

  public boolean isScheduled(String id, int direction) {
    if (direction == 0) {
      return indexWithChildren.containsKey(id) && indexWithParents.containsKey(id);
    } else if (direction == 1) {
      return indexWithChildren.containsKey(id);
    } else if (direction == -1) {
      return indexWithParents.containsKey(id);
    }
    return false;
  }

  public Map<String, Boolean> getQueueCopy() {
    var mergedQueue = new LinkedHashMap<>(indexWithChildren);
    mergedQueue.putAll(indexWithParents);
    return mergedQueue;
  }
}
