package no.einnsyn.backend.common.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import jakarta.servlet.http.HttpServletRequest;
import no.einnsyn.backend.common.search.models.SearchParameters;
import no.einnsyn.backend.entities.journalpost.JournalpostService;
import no.einnsyn.backend.entities.moetemappe.MoetemappeService;
import no.einnsyn.backend.entities.moetesak.MoetesakService;
import no.einnsyn.backend.entities.saksmappe.SaksmappeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SearchSortingConsistencyTest {

  @Mock private JournalpostService journalpostService;
  @Mock private SaksmappeService saksmappeService;
  @Mock private MoetemappeService moetemappeService;
  @Mock private MoetesakService moetesakService;
  @Mock private SearchQueryService searchQueryService;
  @Mock private HttpServletRequest request;

  private SearchService searchService;

  @BeforeEach
  void setUp() {
    searchService =
        new SearchService(
            null,
            journalpostService,
            saksmappeService,
            moetemappeService,
            moetesakService,
            searchQueryService,
            request);

    // Set test values for the SearchService properties
    ReflectionTestUtils.setField(searchService, "elasticsearchIndex", "test-index");
    ReflectionTestUtils.setField(searchService, "defaultSearchLimit", 25);
  }

  @Test
  void testPreferenceHashConsistency() throws Exception {

    // Create identical search parameters
    var searchParams1 = new SearchParameters();
    when(searchQueryService.getQueryBuilder(searchParams1))
        .thenReturn(new BoolQuery.Builder().must(Query.of(q -> q.matchAll(m -> m))));
    searchParams1.setQuery("consistent test query");

    var searchParams2 = new SearchParameters();
    when(searchQueryService.getQueryBuilder(searchParams2))
        .thenReturn(new BoolQuery.Builder().must(Query.of(q -> q.matchAll(m -> m))));
    searchParams2.setQuery("consistent test query");

    // Mock SortByMapper to avoid NullPointerException
    try (var sortByMapperMock = mockStatic(SortByMapper.class)) {
      sortByMapperMock.when(() -> SortByMapper.resolve("score")).thenReturn("_score");
      sortByMapperMock.when(() -> SortByMapper.resolve("id")).thenReturn("_id");

      // Get search requests
      var request1 = searchService.getSearchRequest(searchParams1);
      var request2 = searchService.getSearchRequest(searchParams2);

      // Both requests should have the same preference when sorting by score
      assertEquals(request1.preference(), request2.preference());
      assertNotNull(request1.preference());
    }
  }

  @Test
  void testDifferentQueriesProduceDifferentPreferences() throws Exception {

    // Create different search parameters
    var searchParams1 = new SearchParameters();
    when(searchQueryService.getQueryBuilder(searchParams1))
        .thenReturn(
            new BoolQuery.Builder()
                .must(Query.of(q -> q.match(m -> m.field("title").query("test query 1")))));
    searchParams1.setQuery("consistent test query");

    var searchParams2 = new SearchParameters();
    when(searchQueryService.getQueryBuilder(searchParams2))
        .thenReturn(
            new BoolQuery.Builder()
                .must(Query.of(q -> q.match(m -> m.field("title").query("test query 2")))));
    searchParams2.setQuery("consistent test query");

    // Mock SortByMapper to avoid NullPointerException
    try (var sortByMapperMock = mockStatic(SortByMapper.class)) {
      sortByMapperMock.when(() -> SortByMapper.resolve("score")).thenReturn("_score");
      sortByMapperMock.when(() -> SortByMapper.resolve("id")).thenReturn("_id");

      // Get search requests
      var request1 = searchService.getSearchRequest(searchParams1);
      var request2 = searchService.getSearchRequest(searchParams2);

      // Different queries should produce different preferences
      assertNotEquals(request1.preference(), request2.preference());
      assertNotNull(request1.preference());
      assertNotNull(request2.preference());
    }
  }

  @Test
  void testNoPreferenceWhenNotSortingByScore() throws Exception {
    // Create search parameters with non-score sorting
    var searchParams = new SearchParameters();
    searchParams.setSortBy("created");
    when(searchQueryService.getQueryBuilder(searchParams))
        .thenReturn(new BoolQuery.Builder().must(Query.of(q -> q.matchAll(m -> m))));

    // Mock SortByMapper to avoid NullPointerException
    try (var sortByMapperMock = mockStatic(SortByMapper.class)) {
      sortByMapperMock.when(() -> SortByMapper.resolve("created")).thenReturn("_created");
      sortByMapperMock.when(() -> SortByMapper.resolve("id")).thenReturn("_id");

      // Get search request
      var request = searchService.getSearchRequest(searchParams);

      // Should not have preference set when not sorting by score
      assertNull(request.preference());
    }
  }

  @Test
  void testMultipleSearchRequestsWithScoreSortingHaveSamePreference() throws Exception {
    // Create search parameters for score sorting
    var searchParams = new SearchParameters();
    when(searchQueryService.getQueryBuilder(searchParams))
        .thenReturn(
            new BoolQuery.Builder()
                .must(
                    Query.of(q -> q.match(m -> m.field("content").query("consistent test query")))))
        .thenReturn(
            new BoolQuery.Builder()
                .must(
                    Query.of(q -> q.match(m -> m.field("content").query("consistent test query")))))
        .thenReturn(
            new BoolQuery.Builder()
                .must(
                    Query.of(
                        q -> q.match(m -> m.field("content").query("consistent test query")))));
    searchParams.setQuery("consistent test query");

    // Mock SortByMapper to avoid NullPointerException
    try (var sortByMapperMock = mockStatic(SortByMapper.class)) {
      sortByMapperMock.when(() -> SortByMapper.resolve("score")).thenReturn("_score");
      sortByMapperMock.when(() -> SortByMapper.resolve("id")).thenReturn("_id");

      // Generate multiple search requests
      var request1 = searchService.getSearchRequest(searchParams);
      var request2 = searchService.getSearchRequest(searchParams);
      var request3 = searchService.getSearchRequest(searchParams);

      // All requests should have the same preference
      assertEquals(request1.preference(), request2.preference());
      assertEquals(request2.preference(), request3.preference());
      assertNotNull(request1.preference());
    }
  }

  @Test
  void testNoQuerySortsById() throws Exception {
    // Create identical search parameters
    var searchParams = new SearchParameters();
    when(searchQueryService.getQueryBuilder(searchParams))
        .thenReturn(new BoolQuery.Builder().must(Query.of(q -> q.matchAll(m -> m))));

    // Mock SortByMapper to avoid NullPointerException
    try (var sortByMapperMock = mockStatic(SortByMapper.class)) {
      sortByMapperMock.when(() -> SortByMapper.resolve("score")).thenReturn("_score");
      sortByMapperMock.when(() -> SortByMapper.resolve("id")).thenReturn("_id");

      // Get search requests
      var request1 = searchService.getSearchRequest(searchParams);

      // Both requests should have the same preference when sorting by score
      assertNull(request1.preference());
      assertEquals("_id", request1.sort().get(0).field().field());
    }
  }
}
