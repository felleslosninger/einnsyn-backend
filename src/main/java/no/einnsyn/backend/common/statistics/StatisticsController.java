// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.common.statistics;

import jakarta.validation.Valid;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.statistics.models.StatisticsParameters;
import no.einnsyn.backend.common.statistics.models.StatisticsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatisticsController {
  private final StatisticsService service;

  public StatisticsController(StatisticsService service) {
    this.service = service;
  }

  @GetMapping("/statistics")
  public ResponseEntity<StatisticsResponse> query(@Valid StatisticsParameters query)
      throws EInnsynException {
    var responseBody = service.query(query);
    return ResponseEntity.ok().body(responseBody);
  }
}
