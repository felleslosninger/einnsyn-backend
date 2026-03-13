package no.einnsyn.backend.common.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import no.einnsyn.backend.authentication.AuthenticationService;
import no.einnsyn.backend.common.exceptions.models.BadRequestException;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.queryparameters.models.FilterParameters;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.validation.isodatetime.RelativeDateMath;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@SuppressWarnings("java:S1192") // Allow string literals
public class SearchQueryService {

  public enum DateBoundary {
    NONE,
    START_OF_DAY,
    END_OF_DAY
  }

  private static final List<String> allowedEntities =
      List.of("Journalpost", "Saksmappe", "Moetemappe", "Moetesak");
  public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
  private static final ZoneId NORWEGIAN_ZONE = ZoneId.of("Europe/Oslo");

  private final AuthenticationService authenticationService;
  private final EnhetService enhetService;

  public SearchQueryService(
      AuthenticationService authenticationService, EnhetService enhetService) {
    this.authenticationService = authenticationService;
    this.enhetService = enhetService;
  }

  private String toElasticsearchDateValue(String dateString, DateBoundary boundary)
      throws BadRequestException {
    if (dateString == null) {
      return null;
    }

    // Relative date math is passed through as-is. Callers must use rounding explicitly when they
    // want day-style boundaries, e.g. "now/d" or "now-1d/d".
    if (RelativeDateMath.isValid(dateString)) {
      return dateString;
    }
    if (dateString.startsWith("now")) {
      throw new BadRequestException("Invalid search query.");
    }

    ZonedDateTime zonedDateTime;

    // DateTime
    if (dateString.contains("T")) {
      // Try parsing zoned first; if no zone/offset is present, assume system default zone
      try {
        zonedDateTime = ZonedDateTime.parse(dateString);
      } catch (DateTimeParseException e) {
        var localDateTime = LocalDateTime.parse(dateString);
        zonedDateTime = localDateTime.atZone(NORWEGIAN_ZONE);
      }
    }

    // Date (no timestamp)
    else {
      zonedDateTime = LocalDate.parse(dateString).atStartOfDay(NORWEGIAN_ZONE);
      // Adjust to start or end of day if needed
      zonedDateTime =
          switch (boundary) {
            case START_OF_DAY -> zonedDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0);
            case END_OF_DAY ->
                zonedDateTime.withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);
            case NONE -> zonedDateTime;
          };
    }

    return zonedDateTime.format(FORMATTER);
  }

  private Query getDateRangeQuery(
      String fieldName,
      String originalDateString,
      String elasticsearchDateValue,
      boolean inclusiveTo) {
    return RangeQuery.of(
            r ->
                r.date(
                    d -> {
                      d.field(fieldName);
                      if (inclusiveTo) {
                        d.lte(elasticsearchDateValue);
                      } else {
                        d.gte(elasticsearchDateValue);
                      }
                      if (RelativeDateMath.isValid(originalDateString)) {
                        d.timeZone(NORWEGIAN_ZONE.getId());
                      }
                      return d;
                    }))
        ._toQuery();
  }

  /**
   * Resolve IDs from identifiers like orgnummer, email, ...
   *
   * @param enhetIdentifiers the list of identifiers to resolve
   * @return the list of resolved Enhet IDs
   * @throws BadRequestException if an Enhet is not found
   */
  private List<String> resolveEnhetIds(List<String> enhetIdentifiers) throws BadRequestException {
    var enhetIds = new ArrayList<String>(enhetIdentifiers.size());
    for (var identifier : enhetIdentifiers) {
      var enhetId = enhetService.resolveId(identifier);
      if (enhetId == null) {
        throw new BadRequestException("Enhet not found: " + identifier);
      }
      enhetIds.add(enhetId);
    }
    return enhetIds;
  }

  /**
   * Adds a filter to the bool query.
   *
   * @param bqb the bool query builder
   * @param propertyName the name of the property to filter on
   * @param list the list of values to filter by
   */
  private void addFilter(BoolQuery.Builder bqb, String propertyName, List<String> list) {
    if (list != null && !list.isEmpty()) {
      var fieldValueList = list.stream().map(FieldValue::of).toList();
      bqb.filter(
          TermsQuery.of(tqb -> tqb.field(propertyName).terms(tqfb -> tqfb.value(fieldValueList)))
              ._toQuery());
    }
  }

  /**
   * Adds a must-not clause to the bool query.
   *
   * @param bqb the bool query builder
   * @param propertyName the name of the property to exclude
   * @param list the list of values to exclude
   */
  private void addMustNot(BoolQuery.Builder bqb, String propertyName, List<String> list) {
    if (list != null && !list.isEmpty()) {
      var fieldValueList = list.stream().map(FieldValue::of).toList();
      bqb.mustNot(
          TermsQuery.of(tqb -> tqb.field(propertyName).terms(tqfb -> tqfb.value(fieldValueList)))
              ._toQuery());
    }
  }

  /**
   * Build a ES Query from the given search parameters.
   *
   * @param filterParameters the filter parameters
   * @return the bool query builder
   * @throws EInnsynException if an error occurs
   */
  public BoolQuery.Builder getQueryBuilder(FilterParameters filterParameters)
      throws EInnsynException {
    return getQueryBuilder(filterParameters, false);
  }

  /**
   * Build a ES Query from the given search parameters.
   *
   * @param filterParameters the filter parameters
   * @param uncensored whether to exclude sensitive fields or not
   * @return the bool query builder
   * @throws EInnsynException if an error occurs
   */
  public BoolQuery.Builder getQueryBuilder(FilterParameters filterParameters, boolean uncensored)
      throws EInnsynException {
    var rootBoolQueryBuilder = new BoolQuery.Builder();

    // Filter by entity. We don't want unexpected entities (Innsynskrav, Downloads, ...), so we'll
    // always filter by entities.
    if (filterParameters.getEntity() != null) {
      addFilter(rootBoolQueryBuilder, "type", filterParameters.getEntity());
    } else {
      addFilter(rootBoolQueryBuilder, "type", allowedEntities);
    }

    // Exclude hidden enhets and unaccessible documents
    if (!uncensored) {
      var authenticatedEnhetId = authenticationService.getEnhetId();
      var authenticatedSubtreeIdList = enhetService.getSubtreeIdList(authenticatedEnhetId);

      // Filter hidden enhets that the user is not authenticated for
      var hiddenEnhetList = enhetService.findHidden();
      var hiddenIdList =
          hiddenEnhetList.stream()
              .map(e -> e.getId())
              .filter(e -> !authenticatedSubtreeIdList.contains(e))
              .toList();
      if (!hiddenIdList.isEmpty()) {
        addMustNot(rootBoolQueryBuilder, "administrativEnhetTransitive", hiddenIdList);
      }
    }

    // Exclude unaccessible documents
    if (!uncensored) {
      var authenticatedEnhetId = authenticationService.getEnhetId();
      var accessibleAfterBoolQueryBuilder = new BoolQuery.Builder();
      accessibleAfterBoolQueryBuilder.minimumShouldMatch("1");

      // Allow documents with a valid accessibleAfter
      accessibleAfterBoolQueryBuilder.should(
          RangeQuery.of(r -> r.date(d -> d.field("accessibleAfter").lte("now")))._toQuery());

      // If logged in, allow documents with a valid administrativEnhet
      if (authenticatedEnhetId != null) {
        var authenticatedEnhetFieldValues = List.of(FieldValue.of(authenticatedEnhetId));
        accessibleAfterBoolQueryBuilder.should(
            new TermsQuery.Builder()
                .field("administrativEnhetTransitive")
                .terms(new TermsQueryField.Builder().value(authenticatedEnhetFieldValues).build())
                .build()
                ._toQuery());
      }

      rootBoolQueryBuilder.filter(accessibleAfterBoolQueryBuilder.build()._toQuery());
    }

    // Filter by search query
    var queryString = filterParameters.getQuery();
    if (StringUtils.hasText(queryString)) {
      rootBoolQueryBuilder.must(
          uncensored
              ? getSearchStringQuery(
                  queryString,
                  List.of(
                      "search_id",
                      "search_innhold",
                      "search_innhold_SENSITIV",
                      "search_tittel^3",
                      "search_tittel_SENSITIV^3"),
                  3.0f,
                  1.0f)
              : getSearchStringQuery(
                  queryString,
                  List.of("search_id", "search_innhold_SENSITIV", "search_tittel_SENSITIV^3"),
                  List.of("search_id", "search_innhold", "search_tittel^3"),
                  3.0f,
                  2.0f));
    }

    // Filter by tittel
    if (filterParameters.getTittel() != null) {
      for (var tittel : filterParameters.getTittel()) {
        if (StringUtils.hasText(tittel)) {
          rootBoolQueryBuilder.filter(
              uncensored
                  ? getSearchStringQuery(tittel, List.of("search_tittel", "search_tittel_SENSITIV"))
                  : getSearchStringQuery(
                      tittel, List.of("search_tittel_SENSITIV"), List.of("search_tittel")));
        }
      }
    }

    // Filter by skjermingshjemmel
    if (filterParameters.getSkjermingshjemmel() != null) {
      for (var skjermingshjemmel : filterParameters.getSkjermingshjemmel()) {
        if (StringUtils.hasText(skjermingshjemmel)) {
          rootBoolQueryBuilder.filter(
              getSearchStringQuery(skjermingshjemmel, List.of("skjerming.skjermingshjemmel")));
        }
      }
    }

    // Filter by korrespondansepartNavn
    if (filterParameters.getKorrespondansepartNavn() != null) {
      for (var korrespondansepartNavn : filterParameters.getKorrespondansepartNavn()) {
        if (StringUtils.hasText(korrespondansepartNavn)) {
          rootBoolQueryBuilder.filter(
              getSearchStringQuery(
                  korrespondansepartNavn,
                  List.of("korrespondansepart.korrespondansepartNavn_SENSITIV"),
                  List.of("korrespondansepart.korrespondansepartNavn")));
        }
      }
    }

    // Filter by saksaar
    addFilter(rootBoolQueryBuilder, "saksaar", filterParameters.getSaksaar());

    // Filter by sakssekvensnummer
    addFilter(rootBoolQueryBuilder, "sakssekvensnummer", filterParameters.getSakssekvensnummer());

    // Filter by saksnummer
    addFilter(rootBoolQueryBuilder, "saksnummer", filterParameters.getSaksnummer());

    // Filter by journalpostnummer
    addFilter(rootBoolQueryBuilder, "journalpostnummer", filterParameters.getJournalpostnummer());

    // Filter by journalsekvensnummer
    addFilter(
        rootBoolQueryBuilder, "journalsekvensnummer", filterParameters.getJournalsekvensnummer());

    // Filter by moetesaksaar
    addFilter(rootBoolQueryBuilder, "møtesaksår", filterParameters.getMoetesaksaar());

    // Filter by moetesakssekvensnummer
    addFilter(
        rootBoolQueryBuilder,
        "møtesakssekvensnummer",
        filterParameters.getMoetesakssekvensnummer());

    // Matches against administrativEnhet or children
    if (filterParameters.getAdministrativEnhet() != null) {
      var enhetList = resolveEnhetIds(filterParameters.getAdministrativEnhet());
      addFilter(rootBoolQueryBuilder, "administrativEnhetTransitive", enhetList);
    }

    // Exact matches against administrativEnhet
    if (filterParameters.getAdministrativEnhetExact() != null) {
      var enhetList = resolveEnhetIds(filterParameters.getAdministrativEnhetExact());
      addFilter(rootBoolQueryBuilder, "administrativEnhet", enhetList);
    }

    // Exclude documents from given administrativEnhet or children
    if (filterParameters.getExcludeAdministrativEnhet() != null) {
      var enhetList = resolveEnhetIds(filterParameters.getExcludeAdministrativEnhet());
      addMustNot(rootBoolQueryBuilder, "administrativEnhetTransitive", enhetList);
    }

    // Exclude documents from given administrativEnhet
    if (filterParameters.getExcludeAdministrativEnhetExact() != null) {
      var enhetList = resolveEnhetIds(filterParameters.getExcludeAdministrativEnhetExact());
      addMustNot(rootBoolQueryBuilder, "administrativEnhet", enhetList);
    }

    // Filter by publisertDatoTo
    if (filterParameters.getPublisertDatoTo() != null) {
      var originalDate = filterParameters.getPublisertDatoTo();
      var date = toElasticsearchDateValue(originalDate, DateBoundary.END_OF_DAY);
      rootBoolQueryBuilder.filter(getDateRangeQuery("publisertDato", originalDate, date, true));
    }

    // Filter by publisertDatoFrom
    if (filterParameters.getPublisertDatoFrom() != null) {
      var originalDate = filterParameters.getPublisertDatoFrom();
      var date = toElasticsearchDateValue(originalDate, DateBoundary.NONE);
      rootBoolQueryBuilder.filter(getDateRangeQuery("publisertDato", originalDate, date, false));
    }

    // Filter by oppdatertDatoTo
    if (filterParameters.getOppdatertDatoTo() != null) {
      var originalDate = filterParameters.getOppdatertDatoTo();
      var date = toElasticsearchDateValue(originalDate, DateBoundary.END_OF_DAY);
      rootBoolQueryBuilder.filter(getDateRangeQuery("oppdatertDato", originalDate, date, true));
    }

    // Filter by oppdatertDatoFrom
    if (filterParameters.getOppdatertDatoFrom() != null) {
      var originalDate = filterParameters.getOppdatertDatoFrom();
      var date = toElasticsearchDateValue(originalDate, DateBoundary.NONE);
      rootBoolQueryBuilder.filter(getDateRangeQuery("oppdatertDato", originalDate, date, false));
    }

    // Filter by dokumentetsDatoTo
    if (filterParameters.getDokumentetsDatoTo() != null) {
      var originalDate = filterParameters.getDokumentetsDatoTo();
      var date = toElasticsearchDateValue(originalDate, DateBoundary.END_OF_DAY);
      rootBoolQueryBuilder.filter(getDateRangeQuery("dokumentetsDato", originalDate, date, true));
    }

    // Filter by dokumentetsDatoFrom
    if (filterParameters.getDokumentetsDatoFrom() != null) {
      var originalDate = filterParameters.getDokumentetsDatoFrom();
      var date = toElasticsearchDateValue(originalDate, DateBoundary.NONE);
      rootBoolQueryBuilder.filter(getDateRangeQuery("dokumentetsDato", originalDate, date, false));
    }

    // Filter by journaldatoTo
    if (filterParameters.getJournaldatoTo() != null) {
      var originalDate = filterParameters.getJournaldatoTo();
      var date = toElasticsearchDateValue(originalDate, DateBoundary.END_OF_DAY);
      rootBoolQueryBuilder.filter(getDateRangeQuery("journaldato", originalDate, date, true));
    }

    // Filter by journaldatoFrom
    if (filterParameters.getJournaldatoFrom() != null) {
      var originalDate = filterParameters.getJournaldatoFrom();
      var date = toElasticsearchDateValue(originalDate, DateBoundary.NONE);
      rootBoolQueryBuilder.filter(getDateRangeQuery("journaldato", originalDate, date, false));
    }

    // Filter by moetedatoTo
    if (filterParameters.getMoetedatoTo() != null) {
      var originalDate = filterParameters.getMoetedatoTo();
      var date = toElasticsearchDateValue(originalDate, DateBoundary.END_OF_DAY);
      rootBoolQueryBuilder.filter(getDateRangeQuery("moetedato", originalDate, date, true));
    }

    // Filter by moetedatoFrom
    if (filterParameters.getMoetedatoFrom() != null) {
      var originalDate = filterParameters.getMoetedatoFrom();
      var date = toElasticsearchDateValue(originalDate, DateBoundary.NONE);
      rootBoolQueryBuilder.filter(getDateRangeQuery("moetedato", originalDate, date, false));
    }

    // Filter by standardDatoTo
    if (filterParameters.getStandardDatoTo() != null) {
      var originalDate = filterParameters.getStandardDatoTo();
      var date = toElasticsearchDateValue(originalDate, DateBoundary.END_OF_DAY);
      rootBoolQueryBuilder.filter(getDateRangeQuery("standardDato", originalDate, date, true));
    }

    // Filter by standardDatoFrom
    if (filterParameters.getStandardDatoFrom() != null) {
      var originalDate = filterParameters.getStandardDatoFrom();
      var date = toElasticsearchDateValue(originalDate, DateBoundary.NONE);
      rootBoolQueryBuilder.filter(getDateRangeQuery("standardDato", originalDate, date, false));
    }

    // Filter by fulltext
    if (filterParameters.getFulltext() != null) {
      rootBoolQueryBuilder.filter(
          TermQuery.of(tqb -> tqb.field("fulltext").value(filterParameters.getFulltext()))
              ._toQuery());
    }

    // Filter by journalposttype
    if (filterParameters.getJournalposttype() != null) {
      addFilter(rootBoolQueryBuilder, "journalposttype", filterParameters.getJournalposttype());
    }

    // Get specific IDs
    addFilter(rootBoolQueryBuilder, "id", filterParameters.getIds());

    return rootBoolQueryBuilder;
  }

  /**
   * /** Get a sensitive query that handles uncensored/censored searches.
   *
   * @param queryString the query string to search for
   * @param sensitiveFields the list of sensitive fields
   * @param nonSensitiveFields the list of non-sensitive fields
   * @return the constructed query
   */
  private static Query getSearchStringQuery(
      String queryString, List<String> sensitiveFields, List<String> nonSensitiveFields) {
    return getSearchStringQuery(queryString, sensitiveFields, nonSensitiveFields, 1.0f, 1.0f);
  }

  /**
   * Get a sensitive query that handles uncensored/censored searches.
   *
   * @param queryString the search query string
   * @param sensitiveFields the list of sensitive field names to search in
   * @param nonSensitiveFields the list of non-sensitive field names to search in
   * @param exactBoost the boost factor for exact matches
   * @param looseBoost the boost factor for loose matches
   * @return the constructed query
   */
  private static Query getSearchStringQuery(
      String queryString,
      List<String> sensitiveFields,
      List<String> nonSensitiveFields,
      float exactBoost,
      float looseBoost) {
    var boolQueryBuilder = new BoolQuery.Builder();
    boolQueryBuilder.minimumShouldMatch("1");

    // Match sensitive fields for documents from the past year only
    // Round to start of day to ensure consistent query hashing for preference-based shard routing
    var lastYear =
        ZonedDateTime.now(NORWEGIAN_ZONE)
            .truncatedTo(ChronoUnit.DAYS)
            .minusYears(1)
            .format(FORMATTER);
    var gteLastYear =
        RangeQuery.of(r -> r.date(d -> d.field("publisertDato").gte(lastYear)))._toQuery();

    // For recent documents, evaluate the query against the union of sensitive and non-sensitive
    // fields so negation semantics (e.g. -word) are applied across both field groups.
    var recentFields = new LinkedHashSet<String>(nonSensitiveFields);
    recentFields.addAll(sensitiveFields);
    var recentDocumentsQuery =
        new BoolQuery.Builder()
            .filter(gteLastYear)
            .must(
                getSearchStringQuery(
                    queryString, List.copyOf(recentFields), exactBoost, looseBoost))
            .build();

    // For older (or missing publisertDato) documents, only evaluate non-sensitive fields.
    var olderDocumentsQuery =
        new BoolQuery.Builder()
            .mustNot(gteLastYear)
            .must(getSearchStringQuery(queryString, nonSensitiveFields, exactBoost, looseBoost))
            .build();
    boolQueryBuilder.should(b -> b.bool(recentDocumentsQuery));
    boolQueryBuilder.should(b -> b.bool(olderDocumentsQuery));

    return boolQueryBuilder.build()._toQuery();
  }

  /**
   * A direct wrapper around SearchQueryParser that doesn't consider sensitive fields.
   *
   * @param searchString the search string
   * @param fields the fields to search in
   * @return the constructed query
   */
  private static Query getSearchStringQuery(String searchString, List<String> fields) {
    return SearchQueryParser.parse(searchString, fields);
  }

  /**
   * A direct wrapper around SearchQueryParser that doesn't consider sensitive fields.
   *
   * @param searchString the search query string
   * @param fields the list of field names to search in
   * @param exactBoost the boost factor for exact matches
   * @param looseBoost the boost factor for loose matches
   * @return the constructed query
   */
  private static Query getSearchStringQuery(
      String searchString, List<String> fields, float exactBoost, float looseBoost) {
    return SearchQueryParser.parse(searchString, fields, exactBoost, looseBoost);
  }
}
