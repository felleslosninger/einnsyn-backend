package no.einnsyn.backend.common.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SearchType;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.JsonpUtils;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.exceptions.models.InternalServerErrorException;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.common.search.models.SearchParameters;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.journalpost.JournalpostService;
import no.einnsyn.backend.entities.moetemappe.MoetemappeService;
import no.einnsyn.backend.entities.moetesak.MoetesakService;
import no.einnsyn.backend.entities.saksmappe.SaksmappeService;
import no.einnsyn.backend.utils.id.IdUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class SearchService {

  private final ElasticsearchClient esClient;
  private final JournalpostService journalpostService;
  private final SaksmappeService saksmappeService;
  private final MoetemappeService moetemappeService;
  private final MoetesakService moetesakService;
  private final SearchQueryService searchQueryService;
  private final HttpServletRequest request;

  @Value("${application.elasticsearch.index}")
  private String elasticsearchIndex;

  @Value("${application.defaultSearchResults:25}")
  private int defaultSearchLimit;

  private static final HexFormat HEX_FORMAT = HexFormat.of();
  private static final String SORT_BY_SCORE = "score";
  private static final String RECENT_DOCUMENT_BOOST_STRING =
      """
      {
        "gauss": {
          "publisertDato": {
            "origin": "now",
            "scale": "365d",
            "decay": 0.5
          }
        }
      }
      """;
  private static final FunctionScore RECENT_DOCUMENT_BOOST_FUNCTION =
      FunctionScore.of(f -> f.withJson(new StringReader(RECENT_DOCUMENT_BOOST_STRING)));

  /**
   * Elasticsearch search_type. Intended for tests to avoid shard-local IDF differences causing
   * flaky score ordering.
   */
  @Value("${application.elasticsearch.searchType:query_then_fetch}")
  private String defaultSearchType;

  public SearchService(
      ElasticsearchClient esClient,
      JournalpostService journalpostService,
      SaksmappeService saksmappeService,
      MoetemappeService moetemappeService,
      MoetesakService moetesakService,
      SearchQueryService searchQueryService,
      HttpServletRequest request) {
    this.esClient = esClient;
    this.journalpostService = journalpostService;
    this.saksmappeService = saksmappeService;
    this.moetemappeService = moetemappeService;
    this.moetesakService = moetesakService;
    this.searchQueryService = searchQueryService;
    this.request = request;
  }

  /**
   * Search for documents matching the given search parameters.
   *
   * @param searchParams
   * @return
   * @throws Exception
   */
  @Transactional(readOnly = true)
  public PaginatedList<BaseDTO> search(SearchParameters searchParams) throws EInnsynException {
    var esSearchRequest = getSearchRequest(searchParams);
    try {
      log.debug("search() request: {}", esSearchRequest.toString());
      var esResponse = esClient.search(esSearchRequest, ObjectNode.class);
      log.debug("search() response: {}", esResponse.toString());

      var responseList = esResponse.hits().hits();
      if (responseList.size() == 0) {
        return new PaginatedList<BaseDTO>(new ArrayList<BaseDTO>());
      }

      var startingAfter = searchParams.getStartingAfter();
      var endingBefore = searchParams.getEndingBefore();
      var limit = searchParams.getLimit() != null ? searchParams.getLimit() : defaultSearchLimit;
      var hasNext = false;
      var hasPrevious = false;
      var uri = request.getRequestURI();
      var queryString = request.getQueryString();
      var uriBuilder = UriComponentsBuilder.fromUriString(uri).query(queryString);
      var response = new PaginatedList<BaseDTO>();

      // If startingAfter, we have a previous page.
      if (startingAfter != null) {
        hasPrevious = true;
        if (responseList.size() > limit) {
          hasNext = true;
          responseList = responseList.subList(0, limit);
        }
      }

      // If ending before, we need to reverse the list
      else if (endingBefore != null) {
        hasNext = true;
        if (responseList.size() > limit) {
          hasPrevious = true;
          responseList = responseList.subList(0, limit);
        }
        responseList = responseList.reversed();
      }

      // If neither startingAfter nor endingBefore, but more results than limit, we have a next page
      else if (responseList.size() > limit) {
        hasNext = true;
        responseList = responseList.subList(0, limit);
      }

      if (hasNext) {
        var lastHit = responseList.getLast();
        var startingAfterParam = lastHit.sort().stream().map(FieldValue::_toJsonString).toList();
        uriBuilder.replaceQueryParam("endingBefore");
        uriBuilder.replaceQueryParam("startingAfter");
        uriBuilder.replaceQueryParam("startingAfter", startingAfterParam);
        response.setNext(uriBuilder.build().toString());
      }

      if (hasPrevious) {
        var firstHit = responseList.getFirst();
        var endingBeforeParam = firstHit.sort().stream().map(FieldValue::_toJsonString).toList();
        uriBuilder.replaceQueryParam("startingAfter");
        uriBuilder.replaceQueryParam("endingBefore", endingBeforeParam);
        response.setPrevious(uriBuilder.build().toString());
      }

      // Prepare paths to expand
      var expandPaths =
          searchParams.getExpand() != null
              ? new HashSet<String>(searchParams.getExpand())
              : new HashSet<String>();

      // Loop through the hits and convert them to SearchResultItems
      var searchResultItemList =
          responseList.stream()
              .map(
                  node -> {
                    var id = node.id();
                    var entity = IdUtils.resolveEntity(id);

                    // Create a query object for the expand paths
                    var query = new GetParameters();
                    query.setExpand(new ArrayList<String>(expandPaths));

                    // Get the DTO object from the database
                    try {
                      return switch (entity) {
                        case "Journalpost" -> journalpostService.get(id, query);
                        case "Saksmappe" -> saksmappeService.get(id, query);
                        case "Moetemappe" -> moetemappeService.get(id, query);
                        case "Moetesak" -> moetesakService.get(id, query);
                        default -> {
                          log.warn("Found document in elasticsearch with unknown type: " + id);
                          yield (BaseDTO) null;
                        }
                      };
                    } catch (EInnsynException e) {
                      log.warn("Found non-existing object in elasticsearch: " + id);
                      return (BaseDTO) null;
                    }
                  })
              .filter(Objects::nonNull)
              .toList();

      response.setItems(searchResultItemList);
      return response;
    } catch (ElasticsearchException e) {
      log.error(e.response().toString());
      throw new InternalServerErrorException("Elasticsearch error", e);
    } catch (IOException e) {
      throw new InternalServerErrorException("Elasticsearch IOException", e);
    }
  }

  SearchRequest getSearchRequest(SearchParameters searchParams) throws EInnsynException {
    var sortBy = searchParams.getSortBy() != null ? searchParams.getSortBy() : SORT_BY_SCORE;
    var sortByScore = isSortByScore(searchParams);
    var boolQuery = searchQueryService.getQueryBuilder(searchParams).build();
    var query =
        sortByScore
            ? FunctionScoreQuery.of(
                    f ->
                        f.query(boolQuery._toQuery())
                            .boostMode(FunctionBoostMode.Multiply)
                            .functions(RECENT_DOCUMENT_BOOST_FUNCTION))
                ._toQuery()
            : boolQuery._toQuery();

    var searchRequestBuilder = new SearchRequest.Builder();

    // Add the query to our search source
    searchRequestBuilder.query(query);

    // Sort the results by searchParams.sortBy and searchParams.sortDirection
    var sortOrder = searchParams.getSortOrder();

    // If the request doesn't include a scoring query, sorting by score is meaningless (all docs
    // will get the same score). Fall back to a deterministic id sort so search_after pagination
    // remains stable and we avoid score-related instability/cost.
    if (SORT_BY_SCORE.equals(sortBy) && !sortByScore) {
      sortBy = "id";
    }

    // Ensure correct sort order
    if (SORT_BY_SCORE.equals(sortBy)) {
      // Add preference hash to hit the same shard
      searchRequestBuilder.preference(hashQuery(query));
      if ("dfs_query_then_fetch".equals(defaultSearchType)) {
        searchRequestBuilder.searchType(SearchType.DfsQueryThenFetch);
      }
    }

    // Limit the number of results
    var size = searchParams.getLimit() != null ? searchParams.getLimit() : defaultSearchLimit;
    // Get one more than the limit to check if there are more results left to paginate
    searchRequestBuilder.size(size + 1);
    searchRequestBuilder.index(elasticsearchIndex);

    // Add searchAfter for pagination
    var startingAfter = searchParams.getStartingAfter();
    var endingBefore = searchParams.getEndingBefore();
    if (startingAfter != null && !startingAfter.isEmpty()) {
      var fieldValueList =
          List.of(
              SortByMapper.toFieldValue(sortBy, startingAfter.get(0)),
              FieldValue.of(startingAfter.get(1)));
      searchRequestBuilder.searchAfter(fieldValueList);
    }

    // We need to reverse the list in order to get endingBefore. Elasticsearch only supports
    // searchAfter
    else if (endingBefore != null && !endingBefore.isEmpty()) {
      var fieldValueList =
          List.of(
              SortByMapper.toFieldValue(sortBy, endingBefore.get(0)),
              FieldValue.of(endingBefore.get(1)));
      searchRequestBuilder.searchAfter(fieldValueList);
      // Reverse sort order (the reverse it again when returning the result)
      if ("desc".equalsIgnoreCase(sortOrder)) {
        sortOrder = "asc";
      } else {
        sortOrder = "desc";
      }
    }

    searchRequestBuilder.sort(getSortOptions(sortBy, sortOrder));
    searchRequestBuilder.sort(getSortOptions("id", sortOrder));

    // We only need the ID of each match, so don't fetch sources
    searchRequestBuilder.source(b -> b.fetch(false));

    // We don't need total hits for pagination
    searchRequestBuilder.trackTotalHits(track -> track.enabled(false));

    return searchRequestBuilder.build();
  }

  private boolean isSortByScore(SearchParameters searchParams) {
    return SORT_BY_SCORE.equals(searchParams.getSortBy())
        && (StringUtils.hasText(searchParams.getQuery())
            || !CollectionUtils.isEmpty(searchParams.getKorrespondansepartNavn())
            || !CollectionUtils.isEmpty(searchParams.getTittel())
            || !CollectionUtils.isEmpty(searchParams.getSkjermingshjemmel()));
  }

  SortOptions getSortOptions(String sortBy, String sortOrder) {
    var sort = SortByMapper.resolve(sortBy);
    var order = "desc".equalsIgnoreCase(sortOrder) ? SortOrder.Desc : SortOrder.Asc;
    return SortOptions.of(
        b ->
            b.field(
                f -> {
                  f.field(sort);
                  f.order(order);
                  // .missing can't be added to built-in fields like _score
                  if (sort != null && !sort.startsWith("_")) {
                    f.missing("_last");
                  }
                  return f;
                }));
  }

  /**
   * Creates a hash of the query string to use as preference for consistent sorting.
   *
   * @param query the Elasticsearch query
   * @return hashed query string
   */
  private String hashQuery(Query query) {
    var jsonString = JsonpUtils.toJsonString(query, new JacksonJsonpMapper());
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      var hash = digest.digest(jsonString.getBytes(StandardCharsets.UTF_8));
      return HEX_FORMAT.formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      log.warn("SHA-256 algorithm not available, using query toString as preference", e);
      return query.toString();
    }
  }
}
