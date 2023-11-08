package no.einnsyn.apiv3.features.search;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import no.einnsyn.apiv3.features.search.models.SearchRequestParameters;
import no.einnsyn.apiv3.features.search.models.SearchResultItem;
import no.einnsyn.apiv3.responses.ResponseList;

@RestController
public class SearchController {

  private final SearchService searchService;

  SearchController(SearchService searchService) {
    this.searchService = searchService;
  }

  @GetMapping("/search")
  public ResponseEntity<ResponseList<SearchResultItem>> getJournalpost(
      @Valid SearchRequestParameters searchParams) {

    try {
      // TODO: Should we allow "expand" param here?
      ResponseList<SearchResultItem> responseList = searchService.search(searchParams);
      return ResponseEntity.ok(responseList);
    } catch (Exception e) {
      System.err.println(e); // TODO: Better error handling
      return ResponseEntity.badRequest().build();
    }
  }
}
