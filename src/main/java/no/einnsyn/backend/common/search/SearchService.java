package no.einnsyn.backend.common.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.common.search.models.SearchParameters;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.base.models.BaseES;
import no.einnsyn.backend.entities.journalpost.JournalpostService;
import no.einnsyn.backend.entities.moetemappe.MoetemappeService;
import no.einnsyn.backend.entities.moetesak.MoetesakService;
import no.einnsyn.backend.entities.saksmappe.SaksmappeService;
import no.einnsyn.backend.error.exceptions.EInnsynException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
  private final Gson gson;
  private final HttpServletRequest request;

  @Value("${application.elasticsearch.index}")
  private String elasticsearchIndex;

  @Value("${application.defaultSearchResults:25}")
  private int defaultSearchLimit;

  public SearchService(
      ElasticsearchClient esClient,
      JournalpostService journalpostService,
      SaksmappeService saksmappeService,
      MoetemappeService moetemappeService,
      MoetesakService moetesakService,
      SearchQueryService searchQueryService,
      Gson gson,
      HttpServletRequest request) {
    this.esClient = esClient;
    this.journalpostService = journalpostService;
    this.saksmappeService = saksmappeService;
    this.moetemappeService = moetemappeService;
    this.moetesakService = moetesakService;
    this.searchQueryService = searchQueryService;
    this.gson = gson;
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

      //
      var responseList = esResponse.hits().hits();

      // No results found
      if (responseList.size() == 0) {
        return new PaginatedList<BaseDTO>(new ArrayList<BaseDTO>());
      }

      var startingAfter = searchParams.getStartingAfter();
      var endingBefore = searchParams.getEndingBefore();
      var limit = searchParams.getLimit() != null ? searchParams.getLimit() : defaultSearchLimit;
      var hasNext = false;
      var hasPrevious = false;
      var uri = request.getRequestURI();
      var uriBuilder = UriComponentsBuilder.fromUriString(uri);
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
        var sortKey = SortByMapper.resolve(searchParams.getSortBy());
        var lastHit = responseList.getLast();
        var lastItem = lastHit.source();
        var nextId = lastItem.get("id").asText();
        var sortValue = sortKey == "_score" ? lastHit.score() : lastItem.get(sortKey).asText("");
        uriBuilder.replaceQueryParam("endingBefore");
        uriBuilder.replaceQueryParam("startingAfter", sortValue + "," + nextId);
        response.setNext(uriBuilder.build().toString());
      }

      if (hasPrevious) {
        var sortKey = SortByMapper.resolve(searchParams.getSortBy());
        var firstHit = responseList.getFirst();
        var firstItem = firstHit.source();
        var prevId = firstItem.get("id").asText();
        var sortValue = sortKey == "_score" ? firstHit.score() : firstItem.get(sortKey).asText("");
        uriBuilder.replaceQueryParam("startingAfter");
        uriBuilder.replaceQueryParam("endingBefore", sortValue + "," + prevId);
        response.setPrevious(uriBuilder.build().toString());
      }

      // Prepare paths to expand
      var expandPaths =
          searchParams.getExpand() != null
              ? new HashSet<String>(searchParams.getExpand())
              : new HashSet<String>();

      // TODO: This should be optimized to only send one request to the database per entity type
      // Loop through the hits and convert them to SearchResultItems
      var searchResultItemList =
          responseList.stream()
              .map(
                  node -> {
                    var rawSource = node.source();
                    var source = gson.fromJson(rawSource.toString(), BaseES.class);

                    // Get the ID as a String
                    var id = source.getId();

                    // Get the entity type
                    var rawtype = source.getType();
                    var entity = rawtype.get(0).toString();

                    // Create a query object for the expand paths
                    var query = new GetParameters();
                    query.setExpand(new ArrayList<String>(expandPaths));

                    // Get the DTO object from the database
                    try {
                      var dto =
                          switch (entity) {
                            case "Journalpost" -> journalpostService.get(id, query);
                            case "Saksmappe" -> saksmappeService.get(id, query);
                            case "Moetemappe" -> moetemappeService.get(id, query);
                            case "Moetesak" -> moetesakService.get(id, query);
                            default -> {
                              log.warn(
                                  "Found document in elasticsearch with unknown type: "
                                      + source.getId()
                                      + " : "
                                      + entity);
                              yield (BaseDTO) null;
                            }
                          };
                      return dto;
                    } catch (EInnsynException e) {
                      log.warn("Found non-existing object in elasticsearch: " + source.getId());
                      return (BaseDTO) null;
                    }
                  })
              .filter(Objects::nonNull)
              .toList();

      response.setItems(searchResultItemList);
      return response;
    } catch (ElasticsearchException e) {
      log.error(e.response().toString());
      throw new EInnsynException("Elasticsearch error", e);
    } catch (IOException e) {
      throw new EInnsynException("Elasticsearch IOException", e);
    }
  }

  SearchRequest getSearchRequest(SearchParameters searchParams) throws EInnsynException {
    var boolQuery = searchQueryService.getQueryBuilder(searchParams).build();
    var query = Query.of(q -> q.bool(boolQuery));
    var searchRequestBuilder = new SearchRequest.Builder();

    // Add the query to our search source
    searchRequestBuilder.query(query);

    // Sort the results by searchParams.sortBy and searchParams.sortDirection
    var sortOrder = searchParams.getSortOrder();
    var sortBy = searchParams.getSortBy();

    // Limit the number of results
    var size = searchParams.getLimit() != null ? searchParams.getLimit() : defaultSearchLimit;
    // Get one more than the limit to check if there are more results left to paginate
    searchRequestBuilder.size(size + 1);
    searchRequestBuilder.index(elasticsearchIndex);

    // Add searchAfter for pagination
    var startingAfter = searchParams.getStartingAfter();
    var endingBefore = searchParams.getEndingBefore();
    if (startingAfter != null && startingAfter.size() > 0) {
      var fieldValueList =
          List.of(
              SortByMapper.toFieldValue(sortBy, startingAfter.get(0)),
              FieldValue.of(startingAfter.get(1)));
      searchRequestBuilder.searchAfter(fieldValueList);
    }

    // We need to reverse the list in order to get endingBefore. Elasticsearch only supports
    // searchAfter
    else if (endingBefore != null && endingBefore.size() > 0) {
      var fieldValueList =
          List.of(
              SortByMapper.toFieldValue(sortBy, endingBefore.get(0)),
              FieldValue.of(endingBefore.get(1)));
      searchRequestBuilder.searchAfter(fieldValueList);
      // Reverse sort order (the reverse it again when returning the result)
      if (sortOrder.equalsIgnoreCase("desc")) {
        sortOrder = "asc";
      } else {
        sortOrder = "desc";
      }
    }

    searchRequestBuilder.sort(getSortOptions(sortBy, sortOrder));
    searchRequestBuilder.sort(getSortOptions("id", sortOrder));

    return searchRequestBuilder.build();
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
                  if (!sort.startsWith("_")) {
                    f.missing(order.equals(SortOrder.Desc) ? "_last" : "_first");
                  }
                  return f;
                }));
  }
}
