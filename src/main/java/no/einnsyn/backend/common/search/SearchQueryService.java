package no.einnsyn.backend.common.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.SimpleQueryStringFlag;
import co.elastic.clients.elasticsearch._types.query_dsl.SimpleQueryStringQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import no.einnsyn.backend.common.queryparameters.models.FilterParameters;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.error.exceptions.EInnsynException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SearchQueryService {

  private final EnhetService enhetService;

  public SearchQueryService(EnhetService enhetService) {
    this.enhetService = enhetService;
  }

  public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  static final List<String> allowedEntities =
      List.of("Journalpost", "Saksmappe", "Moetemappe", "Moetesak");

  /**
   * @param bqb
   * @param list
   */
  void addFilter(BoolQuery.Builder bqb, String propertyName, List<String> list) {
    if (!list.isEmpty()) {
      var fieldValueList = list.stream().map(FieldValue::of).toList();
      bqb.filter(
          TermsQuery.of(tqb -> tqb.field(propertyName).terms(tqfb -> tqfb.value(fieldValueList)))
              ._toQuery());
    }
  }

  /**
   * @param bqb
   * @param list
   */
  void addMustNot(BoolQuery.Builder bqb, String propertyName, List<String> list) {
    if (!list.isEmpty()) {
      var fieldValueList = list.stream().map(FieldValue::of).toList();
      bqb.mustNot(
          TermsQuery.of(tqb -> tqb.field(propertyName).terms(tqfb -> tqfb.value(fieldValueList)))
              ._toQuery());
    }
  }

  /**
   * Build a ES Query from the given search parameters.
   *
   * @param filterParameters
   * @throws EInnsynException
   */
  public BoolQuery.Builder getQueryBuilder(FilterParameters filterParameters)
      throws EInnsynException {
    return getQueryBuilder(filterParameters, true, true);
  }

  /**
   * Build a ES Query from the given search parameters.
   *
   * @param filterParameters
   * @param excludeHiddenEnhets
   * @param filterSensitiveFields
   * @return
   */
  public BoolQuery.Builder getQueryBuilder(
      FilterParameters filterParameters, boolean excludeHiddenEnhets, boolean filterSensitiveFields)
      throws EInnsynException {
    var rootBoolQueryBuilder = new BoolQuery.Builder();

    // Filter by entity. We don't want unexpected entities (Innsynskrav, Downloads, ...), so we'll
    // always filter by entities.
    if (filterParameters.getEntity() != null) {
      addFilter(rootBoolQueryBuilder, "type", filterParameters.getEntity());
    } else {
      addFilter(rootBoolQueryBuilder, "type", allowedEntities);
    }

    // Exclude hidden enhets
    if (excludeHiddenEnhets) {
      var hiddenEnhetList = enhetService.findHidden();
      // TODO: Remove enhets the requested user is authenticated for
      var idList = hiddenEnhetList.stream().map(e -> e.getId()).toList();
      addMustNot(rootBoolQueryBuilder, "administrativEnhetTransitive", idList);
    }

    // Add search query
    var queryString = filterParameters.getQuery();
    if (StringUtils.hasText(queryString)) {
      // Make sure documents matches one of the "should" filters
      rootBoolQueryBuilder.minimumShouldMatch("1");

      // Filter by sensitive fields within the last year, and non-sensitive fields for older
      // documents
      if (filterSensitiveFields) {
        // Match sensitive fields in documents from the past year
        var lastYear = LocalDate.now().minusYears(1).format(formatter);
        var gteLastYear = RangeQuery.of(r -> r.date(d -> d.field("publisertDato").gte(lastYear)));
        var recentDocumentsQuery =
            new BoolQuery.Builder()
                .filter(q -> q.range(gteLastYear))
                .must(
                    getSearchStringQuery(
                        queryString,
                        "search_innhold_SENSITIV^1.0",
                        "search_tittel_SENSITIV^3.0",
                        "search_id^3.0"))
                .build();

        // Match non-sensitive fields in documents older than the last year
        var ltLastYear = RangeQuery.of(r -> r.date(d -> d.field("publisertDato").lt(lastYear)));
        var oldDocumentsQuery =
            new BoolQuery.Builder()
                .filter(q -> q.range(ltLastYear))
                .must(
                    getSearchStringQuery(
                        queryString, "search_innhold^1.0", "search_tittel^3.0", "search_id^3.0"))
                .build();

        rootBoolQueryBuilder
            .should(b -> b.bool(recentDocumentsQuery))
            .should(b -> b.bool(oldDocumentsQuery));
      }
      // Match both sensitive and non-sensitive fields
      else {
        // Match all fields
        rootBoolQueryBuilder.should(
            getSearchStringQuery(
                queryString,
                "search_innhold^1.0",
                "search_tittel^3.0",
                "search_id^3.0",
                "search_innhold_SENSITIV^1.0",
                "search_tittel_SENSITIV^3.0",
                "search_id_SENSITIV^3.0"));
      }
    }

    // Matches against administrativEnhet or children
    if (filterParameters.getAdministrativEnhet() != null) {
      var enhetList = filterParameters.getAdministrativEnhet();
      addFilter(rootBoolQueryBuilder, "administrativEnhetTransitive", enhetList);
    }

    // Exact matches against administrativEnhet
    if (filterParameters.getAdministrativEnhetExact() != null) {
      var enhetList = filterParameters.getAdministrativEnhetExact();
      addFilter(rootBoolQueryBuilder, "administrativEnhet", enhetList);
    }

    // Exclude documents from given administrativEnhet or children
    if (filterParameters.getExcludeAdministrativEnhet() != null) {
      var enhetList = filterParameters.getExcludeAdministrativEnhet();
      addMustNot(rootBoolQueryBuilder, "administrativEnhetTransitive", enhetList);
    }

    // Exclude documents from given administrativEnhet
    if (filterParameters.getExcludeAdministrativEnhetExact() != null) {
      var enhetList = filterParameters.getExcludeAdministrativEnhetExact();
      addMustNot(rootBoolQueryBuilder, "administrativEnhet", enhetList);
    }

    // Filter by publisertDatoBefore
    if (filterParameters.getPublisertDatoBefore() != null) {
      var date = LocalDate.parse(filterParameters.getPublisertDatoBefore()).format(formatter);
      rootBoolQueryBuilder.filter(
          RangeQuery.of(r -> r.date(d -> d.field("publisertDato").lt(date)))._toQuery());
    }

    // Filter by publisertDatoAfter
    if (filterParameters.getPublisertDatoAfter() != null) {
      var date = LocalDate.parse(filterParameters.getPublisertDatoAfter()).format(formatter);
      rootBoolQueryBuilder.filter(
          RangeQuery.of(r -> r.date(d -> d.field("publisertDato").gte(date)))._toQuery());
    }

    // Filter by oppdatertDatoBefore
    if (filterParameters.getOppdatertDatoBefore() != null) {
      var date = LocalDate.parse(filterParameters.getOppdatertDatoBefore()).format(formatter);
      rootBoolQueryBuilder.filter(
          RangeQuery.of(r -> r.date(d -> d.field("opprettetDato").lt(date)))._toQuery());
    }

    // Filter by oppdatertDatoAfter
    if (filterParameters.getOppdatertDatoAfter() != null) {
      var date = LocalDate.parse(filterParameters.getOppdatertDatoAfter()).format(formatter);
      rootBoolQueryBuilder.filter(
          RangeQuery.of(r -> r.date(d -> d.field("opprettetDato").gte(date)))._toQuery());
    }

    // Filter by moetedatoBefore
    if (filterParameters.getMoetedatoBefore() != null) {
      var date = LocalDate.parse(filterParameters.getMoetedatoBefore()).format(formatter);
      rootBoolQueryBuilder.filter(
          RangeQuery.of(r -> r.date(d -> d.field("moetedato").lt(date)))._toQuery());
    }

    // Filter by moetedatoAfter
    if (filterParameters.getMoetedatoAfter() != null) {
      var date = LocalDate.parse(filterParameters.getMoetedatoAfter()).format(formatter);
      rootBoolQueryBuilder.filter(
          RangeQuery.of(r -> r.date(d -> d.field("moetedato").gte(date)))._toQuery());
    }

    // Get specific IDs
    if (filterParameters.getIds() != null) {
      var ids = filterParameters.getIds();
      addFilter(rootBoolQueryBuilder, "id", ids);
    }

    return rootBoolQueryBuilder;
  }

  /**
   * Create a query for a search string on the given fields.
   *
   * @param searchString
   * @param fields
   * @return
   */
  static Query getSearchStringQuery(String searchString, String... fields) {
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
}
