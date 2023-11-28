package no.einnsyn.apiv3.entities.search;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.search.models.SearchRequestParameters;
import no.einnsyn.apiv3.entities.search.models.SearchResultItem;
import no.einnsyn.apiv3.responses.ResponseList;

@Slf4j
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
      log.error("Error executing search query", e);
      return ResponseEntity.badRequest().build();
    }
  }
}
