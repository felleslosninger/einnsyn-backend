package no.einnsyn.backend.common.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.SimpleQueryStringFlag;
import co.elastic.clients.elasticsearch._types.query_dsl.SimpleQueryStringQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import no.einnsyn.backend.authentication.AuthenticationService;
import no.einnsyn.backend.common.exceptions.models.BadRequestException;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.queryparameters.models.FilterParameters;
import no.einnsyn.backend.entities.enhet.EnhetService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
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

  private String toIsoDateTime(String dateString, DateBoundary boundary) {
    if (dateString == null) {
      return null;
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

  /**
   * Resolve IDs from identifiers like orgnummer, email, ...
   *
   * @param enhetIdentifiers
   * @return
   * @throws BadRequestException
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
   * @param bqb
   * @param list
   */
  private void addFilter(BoolQuery.Builder bqb, String propertyName, List<? extends String> list) {
    if (list != null && !list.isEmpty()) {
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
  private void addMustNot(BoolQuery.Builder bqb, String propertyName, List<? extends String> list) {
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
   * @param filterParameters
   * @throws EInnsynException
   */
  public BoolQuery.Builder getQueryBuilder(FilterParameters filterParameters)
      throws EInnsynException {
    return getQueryBuilder(filterParameters, false);
  }

  /**
   * Build a ES Query from the given search parameters.
   *
   * @param filterParameters
   * @param excludeHiddenEnhets
   * @param filterSensitiveFields
   * @return
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
                  queryString, List.of("search_tittel^3.0", "search_tittel_SENSITIV^3.0"))
              : getSearchStringQuery(
                  queryString,
                  List.of("search_tittel^3.0"),
                  List.of("search_tittel_SENSITIV^3.0")));
    }

    // Filter by tittel
    if (filterParameters.getTittel() != null) {
      for (var tittel : filterParameters.getTittel()) {
        if (StringUtils.hasText(tittel)) {
          rootBoolQueryBuilder.filter(
              uncensored
                  ? getSearchStringQuery(tittel, List.of("search_tittel", "search_tittel_SENSITIV"))
                  : getSearchStringQuery(
                      tittel, List.of("search_tittel"), List.of("search_tittel_SENSITIV")));
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
      var date = toIsoDateTime(filterParameters.getPublisertDatoTo(), DateBoundary.END_OF_DAY);
      rootBoolQueryBuilder.filter(
          RangeQuery.of(r -> r.date(d -> d.field("publisertDato").lte(date)))._toQuery());
    }

    // Filter by publisertDatoFrom
    if (filterParameters.getPublisertDatoFrom() != null) {
      var date = toIsoDateTime(filterParameters.getPublisertDatoFrom(), DateBoundary.NONE);
      rootBoolQueryBuilder.filter(
          RangeQuery.of(r -> r.date(d -> d.field("publisertDato").gte(date)))._toQuery());
    }

    // Filter by oppdatertDatoTo
    if (filterParameters.getOppdatertDatoTo() != null) {
      var date = toIsoDateTime(filterParameters.getOppdatertDatoTo(), DateBoundary.END_OF_DAY);
      rootBoolQueryBuilder.filter(
          RangeQuery.of(r -> r.date(d -> d.field("oppdatertDato").lte(date)))._toQuery());
    }

    // Filter by oppdatertDatoFrom
    if (filterParameters.getOppdatertDatoFrom() != null) {
      var date = toIsoDateTime(filterParameters.getOppdatertDatoFrom(), DateBoundary.NONE);
      rootBoolQueryBuilder.filter(
          RangeQuery.of(r -> r.date(d -> d.field("oppdatertDato").gte(date)))._toQuery());
    }

    // Filter by dokumentetsDatoTo
    if (filterParameters.getDokumentetsDatoTo() != null) {
      var date = toIsoDateTime(filterParameters.getDokumentetsDatoTo(), DateBoundary.END_OF_DAY);
      rootBoolQueryBuilder.filter(
          RangeQuery.of(r -> r.date(d -> d.field("dokumentetsDato").lte(date)))._toQuery());
    }

    // Filter by dokumentetsDatoFrom
    if (filterParameters.getDokumentetsDatoFrom() != null) {
      var date = toIsoDateTime(filterParameters.getDokumentetsDatoFrom(), DateBoundary.NONE);
      rootBoolQueryBuilder.filter(
          RangeQuery.of(r -> r.date(d -> d.field("dokumentetsDato").gte(date)))._toQuery());
    }

    // Filter by journaldatoTo
    if (filterParameters.getJournaldatoTo() != null) {
      var date = toIsoDateTime(filterParameters.getJournaldatoTo(), DateBoundary.END_OF_DAY);
      rootBoolQueryBuilder.filter(
          RangeQuery.of(r -> r.date(d -> d.field("journaldato").lte(date)))._toQuery());
    }

    // Filter by journaldatoFrom
    if (filterParameters.getJournaldatoFrom() != null) {
      var date = toIsoDateTime(filterParameters.getJournaldatoFrom(), DateBoundary.NONE);
      rootBoolQueryBuilder.filter(
          RangeQuery.of(r -> r.date(d -> d.field("journaldato").gte(date)))._toQuery());
    }

    // Filter by moetedatoTo
    if (filterParameters.getMoetedatoTo() != null) {
      var date = toIsoDateTime(filterParameters.getMoetedatoTo(), DateBoundary.END_OF_DAY);
      rootBoolQueryBuilder.filter(
          RangeQuery.of(r -> r.date(d -> d.field("moetedato").lte(date)))._toQuery());
    }

    // Filter by moetedatoFrom
    if (filterParameters.getMoetedatoFrom() != null) {
      var date = toIsoDateTime(filterParameters.getMoetedatoFrom(), DateBoundary.NONE);
      rootBoolQueryBuilder.filter(
          RangeQuery.of(r -> r.date(d -> d.field("moetedato").gte(date)))._toQuery());
    }

    // Filter by fulltext
    if (filterParameters.getFulltext() != null && filterParameters.getFulltext()) {
      rootBoolQueryBuilder.filter(
          TermQuery.of(tqb -> tqb.field("fulltext").value(true))._toQuery());
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
   * Get a sensitive query that handles uncensored/censored searches.
   *
   * @param queryString
   * @param sensitiveFields
   * @param nonSensitiveFields
   * @return
   */
  private static Query getSearchStringQuery(
      String queryString, List<String> sensitiveFields, List<String> nonSensitiveFields) {
    var boolQueryBuilder = new BoolQuery.Builder();
    boolQueryBuilder.minimumShouldMatch("1");

    // Match non-sensitive fields for all documents
    boolQueryBuilder.should(getSearchStringQuery(queryString, nonSensitiveFields));

    // Match sensitive fields for documents from the past year only
    var lastYear = ZonedDateTime.now().minusYears(1).format(FORMATTER);
    var gteLastYear = RangeQuery.of(r -> r.date(d -> d.field("publisertDato").gte(lastYear)));
    var recentDocumentsQuery =
        new BoolQuery.Builder()
            .filter(q -> q.range(gteLastYear))
            .must(getSearchStringQuery(queryString, sensitiveFields))
            .build();
    boolQueryBuilder.should(b -> b.bool(recentDocumentsQuery));

    return boolQueryBuilder.build()._toQuery();
  }

  /**
   * Create a query for a search string on the given fields.
   *
   * @param searchString
   * @param fields
   * @return
   */
  private static Query getSearchStringQuery(String searchString, List<String> fields) {
    return SimpleQueryStringQuery.of(
            r ->
                r.query(searchString)
                    .fields(fields)
                    .defaultOperator(Operator.Or)
                    // If less than 2 tokens, all must match; if more than 2 tokens, 75% must match
                    .minimumShouldMatch("2<-25%")
                    .autoGenerateSynonymsPhraseQuery(true)
                    .analyzeWildcard(true)
                    .flags(
                        SimpleQueryStringFlag.Phrase, // Enable quoted phrases
                        SimpleQueryStringFlag.And, // Enable + operator
                        SimpleQueryStringFlag.Or, // Enable \| operator
                        SimpleQueryStringFlag.Precedence, // Enable parenthesis
                        SimpleQueryStringFlag.Prefix) // Enable wildcard *
            )
        ._toQuery();
  }
}
