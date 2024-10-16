// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.search;

import jakarta.validation.Valid;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.search.models.SearchQueryDTO;
import no.einnsyn.apiv3.entities.search.models.SearchSearchResponseDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
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
