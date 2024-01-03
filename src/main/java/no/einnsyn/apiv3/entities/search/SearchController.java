package no.einnsyn.apiv3.entities.search;

import jakarta.validation.Valid;
import no.einnsyn.apiv3.entities.search.models.SearchRequestParameters;
import no.einnsyn.apiv3.entities.search.models.SearchResultItem;
import no.einnsyn.apiv3.responses.ResponseList;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {

  private final SearchService searchService;

  SearchController(SearchService searchService) {
    this.searchService = searchService;
  }

  @GetMapping("/search")
  public ResponseEntity<ResponseList<SearchResultItem>> search(
      @Valid SearchRequestParameters searchParams) {

    try {
      ResponseList<SearchResultItem> responseList = searchService.search(searchParams);
      return ResponseEntity.ok(responseList);
    } catch (Exception e) {
      System.err.println(e); // TODO: Better error handling
      return ResponseEntity.badRequest().build();
    }
  }
}
