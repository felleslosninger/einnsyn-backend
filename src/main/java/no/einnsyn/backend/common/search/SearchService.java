package no.einnsyn.backend.common.search;

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
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.SearchParameters;
import no.einnsyn.backend.common.responses.models.ListResponseBody;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.journalpost.JournalpostService;
import no.einnsyn.backend.entities.moetemappe.MoetemappeService;
import no.einnsyn.backend.entities.moetesak.MoetesakService;
import no.einnsyn.backend.entities.saksmappe.SaksmappeService;
import no.einnsyn.backend.error.exceptions.EInnsynException;
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
  private final MoetesakService moetesakService;

  @Value("${application.elasticsearch.index}")
  private String elasticsearchIndex;

  @Value("${application.defaultSearchResults:25}")
  private int defaultSearchResults;

  static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public SearchService(
      ElasticsearchClient esClient,
      JournalpostService journalpostService,
      SaksmappeService saksmappeService,
      MoetemappeService moetemappeService,
      MoetesakService moetesakService) {
    this.esClient = esClient;
    this.journalpostService = journalpostService;
    this.saksmappeService = saksmappeService;
    this.moetemappeService = moetemappeService;
    this.moetesakService = moetesakService;
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
              .<BaseDTO>map(
                  hit -> {
                    // Extract the source from the hit
                    var source = hit.source();
                    if (source == null) {
                      return (BaseDTO) null;
                    }

                    // Get the entity type
                    var rawtype = source.get("type");
                    var entity =
                        switch (rawtype) {
                          case List<?> listType -> listType.get(0).toString();
                          case String stringType -> stringType;
                          default -> "";
                        };

                    // Get the ID as a String
                    var id = source.getString("id");

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
                                      + source.get("id")
                                      + " : "
                                      + entity);
                              yield (BaseDTO) null;
                            }
                          };
                      return dto;
                    } catch (EInnsynException e) {
                      log.warn("Found non-existing object in elasticsearch: " + source.get("id"));
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

  /** Static filter for documents published in the last year */
  static final Query gteLastYearFilter =
      RangeQuery.of(
              r ->
                  r.date(
                      d ->
                          d.field("publisertDato")
                              .gte(LocalDate.now().minusYears(1).format(formatter))))
          ._toQuery();

  /** Static filter for documents published in the last year */
  static final Query ltLastYearFilter =
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
  public Query getQuery(SearchParameters searchParams) {
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

    // Matches against administrativEnhet or children
    if (searchParams.getAdministrativEnhet() != null) {
      var unitFields = searchParams.getAdministrativEnhet().stream().map(FieldValue::of).toList();
      rootBoolQueryBuilder.filter(
          TermsQuery.of(
                  tqb ->
                      tqb.field("administrativEnhetTransitive")
                          .terms(tqfb -> tqfb.value(unitFields)))
              ._toQuery());
    }

    // Exact matches against administrativEnhet
    if (searchParams.getAdministrativEnhetExact() != null) {
      var unitFields =
          searchParams.getAdministrativEnhetExact().stream().map(FieldValue::of).toList();
      rootBoolQueryBuilder.filter(
          TermsQuery.of(
                  tqb -> tqb.field("administrativEnhet").terms(tqfb -> tqfb.value(unitFields)))
              ._toQuery());
    }

    // Exclude documents from given administrativEnhet or children
    if (searchParams.getExcludeAdministrativEnhet() != null) {
      var unitFields =
          searchParams.getExcludeAdministrativEnhet().stream().map(FieldValue::of).toList();
      rootBoolQueryBuilder.mustNot(
          TermsQuery.of(
                  tqb ->
                      tqb.field("administrativEnhetTransitive")
                          .terms(tqfb -> tqfb.value(unitFields)))
              ._toQuery());
    }

    // Exclude documents from given administrativEnhet
    if (searchParams.getExcludeAdministrativEnhetExact() != null) {
      var unitFields =
          searchParams.getExcludeAdministrativEnhetExact().stream().map(FieldValue::of).toList();
      rootBoolQueryBuilder.mustNot(
          TermsQuery.of(
                  tqb -> tqb.field("administrativEnhet").terms(tqfb -> tqfb.value(unitFields)))
              ._toQuery());
    }

    // Filter by ID
    if (searchParams.getIds() != null) {
      var idList = searchParams.getIds().stream().map(FieldValue::of).toList();
      rootBoolQueryBuilder.filter(
          TermsQuery.of(tqb -> tqb.field("id").terms(tqfb -> tqfb.value(idList)))._toQuery());
    }

    // Filter by entity
    if (searchParams.getEntity() != null) {
      var type = searchParams.getEntity().toLowerCase();
      type = StringUtils.capitalize(type);
      var typeList = List.of(FieldValue.of(type));
      rootBoolQueryBuilder.filter(
          TermsQuery.of(tqb -> tqb.field("type").terms(tqfb -> tqfb.value(typeList)))._toQuery());
    }

    // Filter by publisertDatoBefore
    if (searchParams.getPublisertDatoBefore() != null) {
      var date = LocalDate.parse(searchParams.getPublisertDatoBefore()).format(formatter);
      rootBoolQueryBuilder.filter(
          RangeQuery.of(r -> r.date(d -> d.field("publisertDato").lt(date)))._toQuery());
    }

    // Filter by publisertDatoAfter
    if (searchParams.getPublisertDatoAfter() != null) {
      var date = LocalDate.parse(searchParams.getPublisertDatoAfter()).format(formatter);
      rootBoolQueryBuilder.filter(
          RangeQuery.of(r -> r.date(d -> d.field("publisertDato").gte(date)))._toQuery());
    }

    // Filter by oppdatertDatoBefore
    if (searchParams.getOppdatertDatoBefore() != null) {
      var date = LocalDate.parse(searchParams.getOppdatertDatoBefore()).format(formatter);
      rootBoolQueryBuilder.filter(
          RangeQuery.of(r -> r.date(d -> d.field("opprettetDato").lt(date)))._toQuery());
    }

    // Filter by oppdatertDatoAfter
    if (searchParams.getOppdatertDatoAfter() != null) {
      var date = LocalDate.parse(searchParams.getOppdatertDatoAfter()).format(formatter);
      rootBoolQueryBuilder.filter(
          RangeQuery.of(r -> r.date(d -> d.field("opprettetDato").gte(date)))._toQuery());
    }

    // Filter by moetedatoBefore
    if (searchParams.getMoetedatoBefore() != null) {
      var date = LocalDate.parse(searchParams.getMoetedatoBefore()).format(formatter);
      rootBoolQueryBuilder.filter(
          RangeQuery.of(r -> r.date(d -> d.field("moetedato").lt(date)))._toQuery());
    }

    // Filter by moetedatoAfter
    if (searchParams.getMoetedatoAfter() != null) {
      var date = LocalDate.parse(searchParams.getMoetedatoAfter()).format(formatter);
      rootBoolQueryBuilder.filter(
          RangeQuery.of(r -> r.date(d -> d.field("moetedato").gte(date)))._toQuery());
    }

    return rootBoolQueryBuilder.build()._toQuery();
  }

  SearchRequest getSearchRequest(SearchParameters searchParams) {
    var query = getQuery(searchParams);
    var searchRequestBuilder = new SearchRequest.Builder();

    // Add the query to our search source
    searchRequestBuilder.query(query);

    // Sort the results by searchParams.sortBy and searchParams.sortDirection
    var sortOrder =
        "Desc".equalsIgnoreCase(searchParams.getSortOrder()) ? SortOrder.Desc : SortOrder.Asc;
    var sort =
        new SortOptions.Builder()
            .field(f -> f.field(searchParams.getSortOrder()).order(sortOrder))
            .build();
    searchRequestBuilder.sort(sort);

    // Limit the number of results
    var size = searchParams.getLimit() != null ? searchParams.getLimit() : defaultSearchResults;
    searchRequestBuilder.size(size + 1);
    searchRequestBuilder.index(elasticsearchIndex);

    return searchRequestBuilder.build();
  }
}
