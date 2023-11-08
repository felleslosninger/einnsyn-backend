package no.einnsyn.apiv3.features.search;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.SimpleQueryStringFlag;
import co.elastic.clients.elasticsearch._types.query_dsl.SimpleQueryStringQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.JsonData;
import jakarta.transaction.Transactional;
import no.einnsyn.apiv3.entities.journalpost.JournalpostService;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeService;
import no.einnsyn.apiv3.features.search.models.SearchRequestParameters;
import no.einnsyn.apiv3.features.search.models.SearchResultItem;
import no.einnsyn.apiv3.responses.ResponseList;

@Service
public class SearchService {

  private final ElasticsearchClient esClient;
  private final JournalpostService journalpostService;
  private final SaksmappeService saksmappeService;

  @Value("${application.elasticsearchIndex}")
  private String elasticsearchIndex;

  @Value("${application.defaultSearchResults:25}")
  private int defaultSearchResults;

  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public SearchService(ElasticsearchClient esClient, JournalpostService journalpostService,
      SaksmappeService saksmappeService) {
    this.esClient = esClient;
    this.journalpostService = journalpostService;
    this.saksmappeService = saksmappeService;
  }


  /**
   * Search for documents matching the given search parameters.
   * 
   * @param searchParams
   * @return
   * @throws Exception
   */
  @Transactional
  public ResponseList<SearchResultItem> search(SearchRequestParameters searchParams)
      throws Exception {
    var searchRequest = getSearchRequest(searchParams);
    try {
      var response = esClient.search(searchRequest, JSONObject.class);
      var hitList = response.hits().hits();

      // Check if there are more results than what we found
      var searchLimit =
          searchParams.getLimit() != null ? searchParams.getLimit() : defaultSearchResults;
      var hasMore = false;
      if (hitList.size() > searchLimit) {
        hasMore = true;
        hitList = hitList.subList(0, searchLimit);
      }

      // Loop through the hits and convert them to SearchResultItems
      var searchResultItemList = hitList.stream().map(hit -> {
        var source = hit.source();
        if (source == null) {
          return null;
        }
        var rawtype = source.get("type");
        String type = null;
        if (rawtype instanceof List) {
          type = (String) ((List<?>) rawtype).get(0);
        } else if (rawtype instanceof String) {
          type = (String) rawtype;
        }
        if ("journalpost".equalsIgnoreCase(type)) {
          var object = journalpostService.fromES(source);
          if (object == null) {
            // TODO: Log error, found non-existing object in elasticsearch
            return null;
          }
          var json = journalpostService.toJSON(object, searchParams.getExpand());
          return new SearchResultItem(json);
        } else if ("saksmappe".equalsIgnoreCase(type)) {
          var object = saksmappeService.fromES(source);
          if (object == null) {
            // TODO: Log error, found non-existing object in elasticsearch
            return null;
          }
          var json = saksmappeService.toJSON(object, searchParams.getExpand());
          return new SearchResultItem(json);
        } else {
          System.err.println("Found unknown type: " + type);
          return null;
        }
      }).filter(object -> object != null).collect(Collectors.toList());
      var responseList = new ResponseList<SearchResultItem>(searchResultItemList);
      responseList.setHasMore(hasMore);

      return responseList;
    } catch (ElasticsearchException e) {
      System.err.println(e.getMessage());
      System.err.println(e.response().toString());
      throw e;
    } catch (Exception e) {
      System.err.println("ERROR:");
      e.printStackTrace();
      throw e;
    }
  }


  /**
   * Static filter for documents published in the last year
   */
  Query gteLastYearFilter = RangeQuery.of(rq -> rq.field("publisertDato")
      .gte(JsonData.of(LocalDate.now().minusYears(1).format(formatter))))._toQuery();


  /**
   * Static filter for documents published in the last year
   */
  Query ltLastYearFilter = RangeQuery.of(rq -> rq.field("publisertDato")
      .lt(JsonData.of(LocalDate.now().minusYears(1).format(formatter))))._toQuery();


  /**
   * Create a query for a search string on the given fields.
   * 
   * @param searchString
   * @param fields
   * @return
   */
  Query getSearchStringQuery(String searchString, String... fields) {
    // @formatter:off
    return SimpleQueryStringQuery.of(r -> r
      .query(searchString)
      .fields(Arrays.asList(fields))
      .defaultOperator(Operator.And)
      .autoGenerateSynonymsPhraseQuery(true)
      .analyzeWildcard(true) // TODO: Do we want/need this?
      .flags( // TODO: Review these flags
        SimpleQueryStringFlag.Phrase, // Enable quoted phrases
        SimpleQueryStringFlag.And, // Enable + operator
        SimpleQueryStringFlag.Or, // Enable \| operator
        SimpleQueryStringFlag.Precedence // Enable parenthesis
      )
    )._toQuery();
    // @formatter:on

  }


  /**
   * Build a ES Query from the given search parameters.
   * 
   * @param searchParams
   * @return
   */
  public Query getQuery(SearchRequestParameters searchParams) {
    var rootBoolQueryBuilder = new BoolQuery.Builder();

    // TODO: startingBefore, endingAfter

    // Add search query
    if (searchParams.getQuery() != null) {
      // Match sensitive fields in documents from the past year
      // @formatter:off
      var recentDocumentsQuery = new BoolQuery.Builder()
        .filter(gteLastYearFilter)
        .must(getSearchStringQuery(searchParams.getQuery(),
          "search_innhold_SENSITIV^1.0",
          "search_tittel_SENSITIV^3.0",
          "search_id^3.0"        
        )).build()._toQuery();
      // @formatter:on

      // Match insensitive fields in documents older than the last year
      // @formatter:off
      var oldDocumentsQuery = new BoolQuery.Builder()
        .filter(ltLastYearFilter)
        .must(getSearchStringQuery(searchParams.getQuery(),
          "search_innhold^1.0",
          "search_tittel^3.0",
          "search_id^3.0"
        )).build()._toQuery();
      // @formatter:on

      // @formatter:off
      rootBoolQueryBuilder
          .should(b -> b.bool(bqb -> bqb.must(recentDocumentsQuery)))
          .should(b -> b.bool(bqb -> bqb.must(oldDocumentsQuery)));
      // @formatter:on
    }

    // Filter by unit IDs
    if (searchParams.getAdministrativEnhet() != null) {
      List<FieldValue> unitFields = searchParams.getAdministrativEnhet().stream()
          .map(unitId -> FieldValue.of(unitId)).collect(Collectors.toList());
      rootBoolQueryBuilder.filter(TermsQuery
          .of(tqb -> tqb.field("administrativEnhet").terms(tqfb -> tqfb.value(unitFields)))
          ._toQuery());
    }
    if (searchParams.getAdministrativEnhetTransitive() != null) {
      List<FieldValue> unitFields = searchParams.getAdministrativEnhetTransitive().stream()
          .map(unitId -> FieldValue.of(unitId)).collect(Collectors.toList());
      rootBoolQueryBuilder.filter(TermsQuery.of(
          tqb -> tqb.field("administrativEnhetTransitive").terms(tqfb -> tqfb.value(unitFields)))
          ._toQuery());
    }

    // Filted by unit IRIs (legacy)
    if (searchParams.getAdministrativEnhetIri() != null) {
      List<FieldValue> unitFields = searchParams.getAdministrativEnhetIri().stream()
          .map(unitId -> FieldValue.of(unitId)).collect(Collectors.toList());
      rootBoolQueryBuilder.filter(TermsQuery
          .of(tqb -> tqb.field("arkivskaper").terms(tqfb -> tqfb.value(unitFields)))._toQuery());
    }
    if (searchParams.getAdministrativEnhetIriTransitive() != null) {
      List<FieldValue> unitFields = searchParams.getAdministrativEnhetIriTransitive().stream()
          .map(unitId -> FieldValue.of(unitId)).collect(Collectors.toList());
      rootBoolQueryBuilder.filter(TermsQuery
          .of(tqb -> tqb.field("arkivskaperTransitive").terms(tqfb -> tqfb.value(unitFields)))
          ._toQuery());
    }

    return rootBoolQueryBuilder.build()._toQuery();
  }


  SearchRequest getSearchRequest(SearchRequestParameters searchParams) {
    var query = getQuery(searchParams);
    var searchRequestBuilder = new SearchRequest.Builder();

    // Add the query to our search source
    searchRequestBuilder.query(query);

    // Sort the results by searchParams.sortBy and searchParams.sortDirection
    var sortOrder = "Desc".equalsIgnoreCase(searchParams.getSortOrder()) ? "Desc" : "Asc";
    SortOptions sort = new SortOptions.Builder()
        .field(f -> f.field(searchParams.getSortBy()).order(SortOrder.valueOf(sortOrder))).build();
    searchRequestBuilder.sort(sort);

    // Limit the number of results
    var size = searchParams.getLimit() != null ? searchParams.getLimit() : defaultSearchResults;
    searchRequestBuilder = searchRequestBuilder.size(size + 1);

    searchRequestBuilder.index(elasticsearchIndex);

    return searchRequestBuilder.build();
  }

}
