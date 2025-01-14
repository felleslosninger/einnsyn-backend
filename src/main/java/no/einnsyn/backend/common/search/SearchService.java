package no.einnsyn.backend.common.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.responses.models.ListResponseBody;
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

@Slf4j
@Service
public class SearchService {

  private final ElasticsearchClient esClient;
  private final JournalpostService journalpostService;
  private final SaksmappeService saksmappeService;
  private final MoetemappeService moetemappeService;
  private final MoetesakService moetesakService;
  private final Gson gson;

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
      Gson gson) {
    this.esClient = esClient;
    this.journalpostService = journalpostService;
    this.saksmappeService = saksmappeService;
    this.moetemappeService = moetemappeService;
    this.moetesakService = moetesakService;
    this.gson = gson;
  }

  /**
   * Search for documents matching the given search parameters.
   *
   * @param searchParams
   * @return
   * @throws Exception
   */
  @Transactional(readOnly = true)
  public ListResponseBody<BaseDTO> search(SearchParameters searchParams) throws EInnsynException {
    var searchRequest = getSearchRequest(searchParams);
    try {
      var response = esClient.search(searchRequest, ObjectNode.class);
      log.debug("Search response: {}", response.toString());

      var hitList = response.hits().hits();

      // Check if there are more results than what we found
      var searchLimit =
          searchParams.getLimit() != null ? searchParams.getLimit() : defaultSearchLimit;
      var expandPaths =
          searchParams.getExpand() != null
              ? new HashSet<String>(searchParams.getExpand())
              : new HashSet<String>();

      // Remove extra items gotten for pagination
      if (hitList.size() > searchLimit) {
        hitList = hitList.subList(0, searchLimit);
      }

      // TODO: This should be optimized to only send one request to the database per entity type
      // Loop through the hits and convert them to SearchResultItems
      var searchResultItemList =
          hitList.stream()
              .<BaseDTO>map(
                  hit -> {
                    // Extract the source from the hit
                    var rawSource = hit.source();
                    var source = gson.fromJson(rawSource.toString(), BaseES.class);

                    // Get the entity type
                    var rawtype = source.getType();
                    var entity = rawtype.get(0).toString();

                    // Get the ID as a String
                    var id = source.getId();

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

      return new ListResponseBody<BaseDTO>(searchResultItemList);
    } catch (Exception e) {
      throw new EInnsynException("Elasticsearch error", e);
    }
  }

  SearchRequest getSearchRequest(SearchParameters searchParams) {
    var boolQuery = SearchQueryBuilder.getQueryBuilder(searchParams).build();
    var query = Query.of(q -> q.bool(boolQuery));
    var searchRequestBuilder = new SearchRequest.Builder();

    // Add the query to our search source
    log.debug("Search query: {}", query.toString());
    searchRequestBuilder.query(query);

    // Sort the results by searchParams.sortBy and searchParams.sortDirection
    var sortOrder = searchParams.getSortOrder();
    var sortBy = searchParams.getSortBy();
    searchRequestBuilder.sort(getSortOptions(sortBy, sortOrder));

    // Limit the number of results
    var size = searchParams.getLimit() != null ? searchParams.getLimit() : defaultSearchLimit;
    // Get two more than the limit to check if there are more results left to paginate (next and
    // previous)
    searchRequestBuilder.size(size + 2);
    searchRequestBuilder.index(elasticsearchIndex);

    // Add searchAfter for pagination
    var startingAfter = searchParams.getStartingAfter();
    var endingBefore = searchParams.getEndingBefore();
    if (startingAfter != null && startingAfter.size() > 0) {
      var fieldValueList = startingAfter.stream().map(FieldValue::of).toList();
      searchRequestBuilder.searchAfter(fieldValueList);
    }
    // We need to reverse the list in order to get endingBefore. Elasticsearch only supports
    // searchAfter
    else if (endingBefore != null && endingBefore.size() > 0) {
      var fieldValueList = endingBefore.stream().map(FieldValue::of).toList();
      searchRequestBuilder.searchAfter(fieldValueList);
      // Reverse sort order (the reverse it again when returning the result)
      if (sortOrder.equalsIgnoreCase("desc")) {
        searchRequestBuilder.sort(getSortOptions(searchParams.getSortBy(), "asc"));
      } else {
        searchRequestBuilder.sort(getSortOptions(searchParams.getSortBy(), "desc"));
      }
    }

    return searchRequestBuilder.build();
  }

  SortOptions getSortOptions(String sortBy, String sortOrder) {
    var sort = SortByMapper.resolve(sortBy);
    var order = "desc".equalsIgnoreCase(sortOrder) ? SortOrder.Desc : SortOrder.Asc;
    return SortOptions.of(b -> b.field(f -> f.field(sort).order(order)));
  }
}
