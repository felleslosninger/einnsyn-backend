package no.einnsyn.backend.configuration;

import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import java.time.Duration;
import java.time.ZonedDateTime;
import no.einnsyn.backend.authentication.AuthenticationConfiguration;
import no.einnsyn.backend.common.statistics.models.StatisticsResponse;
import no.einnsyn.backend.entities.dokumentobjekt.DokumentobjektDownloadController;
import org.springframework.core.MethodParameter;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Makes successful GETs cacheable: anonymous responses briefly, and statistics until midnight, when
 * the default aggregation range moves. Responses that are not marked here keep Spring Security's
 * no-store.
 */
@ControllerAdvice(annotations = RestController.class)
public class CacheControlAdvice implements ResponseBodyAdvice<Object> {

  private static final Duration ANONYMOUS_MAX_AGE = Duration.ofMinutes(2);

  // Keeps authenticated requests from matching another caller's cached copy
  private static final String VARY =
      String.join(
          ", ",
          HttpHeaders.AUTHORIZATION,
          AuthenticationConfiguration.API_KEY_HEADER,
          AuthenticationConfiguration.ACTING_AS_HEADER);

  @Override
  public boolean supports(
      MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    // Each download is counted
    return returnType.getContainingClass() != DokumentobjektDownloadController.class;
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter returnType,
      MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request,
      ServerHttpResponse response) {

    // Non-GETs, non-successful responses, and downloads should not be
    if (!HttpMethod.GET.equals(request.getMethod())
        || !(response instanceof ServletServerHttpResponse servletResponse)
        || !HttpStatusCode.valueOf(servletResponse.getServletResponse().getStatus())
            .is2xxSuccessful()) {
      return body;
    }

    var headers = response.getHeaders();
    headers.add(HttpHeaders.VARY, VARY);
    if (body instanceof StatisticsResponse statisticsResponse && !isHourly(statisticsResponse)) {
      headers.setCacheControl(CacheControl.maxAge(untilMidnight()).cachePublic());
    } else if (isAnonymous(request)) {
      headers.setCacheControl(CacheControl.maxAge(ANONYMOUS_MAX_AGE).cachePublic());
    }
    return body;
  }

  private static boolean isHourly(StatisticsResponse statisticsResponse) {
    var metadata = statisticsResponse.getMetadata();
    return metadata != null
        && CalendarInterval.Hour.jsonValue().equals(metadata.getAggregateInterval());
  }

  private static boolean isAnonymous(ServerHttpRequest request) {
    var requestHeaders = request.getHeaders();
    return !requestHeaders.containsHeader(HttpHeaders.AUTHORIZATION)
        && !requestHeaders.containsHeader(AuthenticationConfiguration.API_KEY_HEADER);
  }

  private static Duration untilMidnight() {
    var now = ZonedDateTime.now();
    return Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(now.getZone()));
  }
}
