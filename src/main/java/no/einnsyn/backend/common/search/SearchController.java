// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.common.search;

import jakarta.validation.Valid;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.common.search.models.SearchParameters;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.error.exceptions.EInnsynException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {
  private final SearchService service;

  public SearchController(SearchService service) {
    this.service = service;
  }

  @GetMapping("/search")
  public ResponseEntity<PaginatedList<BaseDTO>> search(@Valid SearchParameters query)
      throws EInnsynException {
    var responseBody = service.search(query);
    return ResponseEntity.ok().body(responseBody);
  }
}
