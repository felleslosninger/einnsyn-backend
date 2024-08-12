package no.einnsyn.apiv3.entities.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.journalpost.JournalpostService;
import no.einnsyn.apiv3.entities.moetemappe.MoetemappeService;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeService;
import no.einnsyn.apiv3.entities.search.models.SearchQueryDTO;
import no.einnsyn.apiv3.entities.search.models.SearchSearchResponseDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
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

  @Value("${application.elasticsearchIndex}")
  private String elasticsearchIndex;

  @Value("${application.defaultSearchResults:25}")
  private int defaultSearchResults;

  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public SearchService(
      ElasticsearchClient esClient,
      JournalpostService journalpostService,
      SaksmappeService saksmappeService,
      MoetemappeService moetemappeService) {
    this.esClient = esClient;
    this.journalpostService = journalpostService;
    this.saksmappeService = saksmappeService;
    this.moetemappeService = moetemappeService;
  }

  /**
   * Search for documents matching the given search parameters.
   *
   * @param searchParams
   * @return
   * @throws Exception
   */
  @Transactional
  public ResultList<SearchSearchResponseDTO> search(SearchQueryDTO searchParams)
      throws EInnsynException {
    var searchRequest = getSearchRequest(searchParams);
    try {
      var response = esClient.search(searchRequest, JSONObject.class);
      var hitList = response.hits().hits();

      // Check if there are more results than what we found
      var searchLimit =
          searchParams.getLimit() != null ? searchParams.getLimit() : defaultSearchResults;
      var expandPaths = new HashSet<String>(searchParams.getExpand());
      if (hitList.size() > searchLimit) {
        hitList = hitList.subList(0, searchLimit);
      }

      // Loop through the hits and convert them to SearchResultItems
      var searchResultItemList =
          hitList.stream()
              .map(
                  hit -> {
                    var source = hit.source();
                    if (source == null) {
                      return null;
                    }

                    var rawtype = source.get("type");
                    String type = null;
                    if (rawtype instanceof List<?> listType) {
                      type = (String) listType.get(0);
                    } else if (rawtype instanceof String stringType) {
                      type = stringType;
                    }

                    var id = source.getString("id");
                    var query = new BaseGetQueryDTO();
                    query.setExpand(new ArrayList<>(expandPaths));

                    try {
                      if ("journalpost".equalsIgnoreCase(type)) {
                        return new SearchSearchResponseDTO(journalpostService.get(id, query));
                      } else if ("saksmappe".equalsIgnoreCase(type)) {
                        return new SearchSearchResponseDTO(saksmappeService.get(id, query));
                      } else if ("moetemappe".equalsIgnoreCase(type)) {
                        return new SearchSearchResponseDTO(moetemappeService.get(id, query));
                      } else {
                        log.warn(
                            "Found document in elasticsearch with unknown type: "
                                + source.get("id")
                                + " : "
                                + type);
                        return null;
                      }
                    } catch (EInnsynException e) {
                      log.warn(
                          "Found non-existing moetemappe in elasticsearch: " + source.get("id"));
                      return null;
                    }
                  })
              .filter(Objects::nonNull)
              .toList();

      return new ResultList<>(searchResultItemList);
    } catch (Exception e) {
      throw new EInnsynException("Elasticsearch error", e);
    }
  }

  /** Static filter for documents published in the last year */
  Query gteLastYearFilter =
      RangeQuery.of(
              r ->
                  r.date(
                      d ->
                          d.field("publisertDato")
                              .gte(LocalDate.now().minusYears(1).format(formatter))))
          ._toQuery();

  /** Static filter for documents published in the last year */
  Query ltLastYearFilter =
      RangeQuery.of(
              r ->
                  r.date(
                      d ->
                          d.field("publisertDato")
                              .lt(LocalDate.now().minusYears(1).format(formatter))))
          ._toQuery();

  /**
   * Create a query for a search string on the given fields.
   *
   * @param searchString
   * @param fields
   * @return
   */
  Query getSearchStringQuery(String searchString, String... fields) {
    return SimpleQueryStringQuery.of(
            r ->
                r.query(searchString)
                    .fields(Arrays.asList(fields))
                    .defaultOperator(Operator.And)
                    .autoGenerateSynonymsPhraseQuery(true)
                    .analyzeWildcard(true) // TODO: Do we want/need this?
                    .flags( // TODO: Review these flags
                        SimpleQueryStringFlag.Phrase, // Enable quoted phrases
                        SimpleQueryStringFlag.And, // Enable + operator
                        SimpleQueryStringFlag.Or, // Enable \| operator
                        SimpleQueryStringFlag.Precedence // Enable parenthesis
                        ))
        ._toQuery();
  }

  /**
   * Build a ES Query from the given search parameters.
   *
   * @param searchParams
   * @return
   */
  public Query getQuery(SearchQueryDTO searchParams) {
    var rootBoolQueryBuilder = new BoolQuery.Builder();

    // TODO: startingBefore, endingAfter

    // Add search query
    if (searchParams.getQuery() != null) {
      // Match sensitive fields in documents from the past year
      var recentDocumentsQuery =
          new BoolQuery.Builder()
              .filter(gteLastYearFilter)
              .must(
                  getSearchStringQuery(
                      searchParams.getQuery(),
                      "search_innhold_SENSITIV^1.0",
                      "search_tittel_SENSITIV^3.0",
                      "search_id^3.0"))
              .build()
              ._toQuery();

      // Match insensitive fields in documents older than the last year
      var oldDocumentsQuery =
          new BoolQuery.Builder()
              .filter(ltLastYearFilter)
              .must(
                  getSearchStringQuery(
                      searchParams.getQuery(),
                      "search_innhold^1.0",
                      "search_tittel^3.0",
                      "search_id^3.0"))
              .build()
              ._toQuery();

      rootBoolQueryBuilder
          .should(b -> b.bool(bqb -> bqb.must(recentDocumentsQuery)))
          .should(b -> b.bool(bqb -> bqb.must(oldDocumentsQuery)));
    }

    // Filter by unit IDs (only works for documents indexed by the API)
    if (searchParams.getAdministrativEnhetId() != null) {
      var unitFields = searchParams.getAdministrativEnhetId().stream().map(FieldValue::of).toList();
      rootBoolQueryBuilder.filter(
          TermsQuery.of(
                  tqb -> tqb.field("administrativEnhet").terms(tqfb -> tqfb.value(unitFields)))
              ._toQuery());
    }
    if (searchParams.getAdministrativEnhetTransitiveId() != null) {
      var unitFields =
          searchParams.getAdministrativEnhetTransitiveId().stream()
              .map(FieldValue::of)
              .collect(Collectors.toList());
      rootBoolQueryBuilder.filter(
          TermsQuery.of(
                  tqb ->
                      tqb.field("administrativEnhetTransitive")
                          .terms(tqfb -> tqfb.value(unitFields)))
              ._toQuery());
    }

    // Filted by unit IRIs (legacy)
    if (searchParams.getAdministrativEnhetIri() != null) {
      var unitFields =
          searchParams.getAdministrativEnhetIri().stream()
              .map(FieldValue::of)
              .collect(Collectors.toList());
      rootBoolQueryBuilder.filter(
          TermsQuery.of(tqb -> tqb.field("arkivskaper").terms(tqfb -> tqfb.value(unitFields)))
              ._toQuery());
    }
    if (searchParams.getAdministrativEnhetTransitiveIri() != null) {
      var unitFields =
          searchParams.getAdministrativEnhetTransitiveIri().stream()
              .map(FieldValue::of)
              .collect(Collectors.toList());
      rootBoolQueryBuilder.filter(
          TermsQuery.of(
                  tqb -> tqb.field("arkivskaperTransitive").terms(tqfb -> tqfb.value(unitFields)))
              ._toQuery());
    }

    // Filter by type
    if (searchParams.getResource() != null) {
      var type = searchParams.getResource().toLowerCase();
      type = StringUtils.capitalize(type);
      var typeList = List.of(FieldValue.of(type));
      rootBoolQueryBuilder.filter(
          TermsQuery.of(tqb -> tqb.field("type").terms(tqfb -> tqfb.value(typeList)))._toQuery());
    }

    return rootBoolQueryBuilder.build()._toQuery();
  }

  SearchRequest getSearchRequest(SearchQueryDTO searchParams) {
    var query = getQuery(searchParams);
    var searchRequestBuilder = new SearchRequest.Builder();

    // Add the query to our search source
    searchRequestBuilder.query(query);

    // Sort the results by searchParams.sortBy and searchParams.sortDirection
    var sortOrder = "Desc".equalsIgnoreCase(searchParams.getSortOrder()) ? "Desc" : "Asc";
    var sort =
        new SortOptions.Builder()
            .field(f -> f.field(searchParams.getSortBy()).order(SortOrder.valueOf(sortOrder)))
            .build();
    searchRequestBuilder.sort(sort);

    // Limit the number of results
    var size = searchParams.getLimit() != null ? searchParams.getLimit() : defaultSearchResults;
    searchRequestBuilder.size(size + 1);
    searchRequestBuilder.index(elasticsearchIndex);

    return searchRequestBuilder.build();
  }
}
