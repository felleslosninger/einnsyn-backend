// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.search;

import jakarta.validation.Valid;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.search.models.SearchQueryDTO;
import no.einnsyn.apiv3.entities.search.models.UnionResourceSearch;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("java:S1130")
@RestController
public class SearchController {

  private final SearchService service;

  public SearchController(SearchService service) {
    this.service = service;
  }

  @GetMapping("/search")
  public ResponseEntity<ResultList<UnionResourceSearch>> search(@Valid SearchQueryDTO query)
      throws EInnsynException {
    var responseBody = service.search(query);
    return ResponseEntity.ok().body(responseBody);
  }
}
