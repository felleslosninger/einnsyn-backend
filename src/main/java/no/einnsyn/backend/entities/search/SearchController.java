// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.search;

import jakarta.validation.Valid;
import no.einnsyn.backend.common.resultlist.ResultList;
import no.einnsyn.backend.entities.search.models.SearchQueryDTO;
import no.einnsyn.backend.entities.search.models.SearchSearchResponseDTO;
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
  public ResponseEntity<ResultList<SearchSearchResponseDTO>> search(@Valid SearchQueryDTO query)
      throws EInnsynException {
    var responseBody = service.search(query);
    return ResponseEntity.ok().body(responseBody);
  }
}
