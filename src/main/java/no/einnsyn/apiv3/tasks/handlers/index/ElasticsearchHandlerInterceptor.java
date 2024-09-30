package no.einnsyn.apiv3.tasks.handlers.index;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
public class ElasticsearchHandlerInterceptor implements HandlerInterceptor {

  private ElasticsearchIndexQueue elasticsearchIndexQueue;

  public ElasticsearchHandlerInterceptor(ElasticsearchIndexQueue elasticsearchIndexQueue) {
    this.elasticsearchIndexQueue = elasticsearchIndexQueue;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
      throws Exception {
    elasticsearchIndexQueue.execute();
  }
}
