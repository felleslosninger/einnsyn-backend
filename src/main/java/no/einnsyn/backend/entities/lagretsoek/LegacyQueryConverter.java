package no.einnsyn.backend.entities.lagretsoek;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.search.models.SearchParameters;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.entities.lagretsoek.models.LegacyQuery;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class LegacyQueryConverter {

  // Regex patterns
  private static final String WHITESPACE_SPLIT_PATTERN = "\\s+";
  private static final String SPECIAL_CHARS_ESCAPE_PATTERN = "([+\"|*()~\\\\])";
  private static final String ESCAPE_REPLACEMENT = "\\\\$1";
  private static final String DATE_MATH_SUFFIX = "||/";

  private final ObjectMapper objectMapper;
  private final EnhetService enhetService;

  public LegacyQueryConverter(ObjectMapper objectMapper, EnhetService enhetService) {
    this.objectMapper = objectMapper;
    this.enhetService = enhetService;
  }

  public SearchParameters convertLegacyQuery(String legacyQuery) throws EInnsynException {
    var query = parseLegacyQuery(legacyQuery);
    var searchParameters = new SearchParameters();

    setSimpleQuery(query, searchParameters);
    processSearchTerms(query, searchParameters);
    setExternalIds(query, searchParameters);
    setLimit(query, searchParameters);
    processFilters(query, searchParameters);
    processSortParameters(query, searchParameters);

    return searchParameters;
  }

  private LegacyQuery parseLegacyQuery(String legacyQuery) throws EInnsynException {
    try {
      return objectMapper.readValue(legacyQuery, LegacyQuery.class);
    } catch (JsonProcessingException e) {
      throw new EInnsynException(
          "Could not parse legacy query: " + e.getMessage(), e, "legacyQueryConversionError");
    }
  }

  private void setSimpleQuery(LegacyQuery query, SearchParameters searchParameters) {
    if (StringUtils.hasText(query.getSearchTerm())) {
      searchParameters.setQuery(query.getSearchTerm());
    }
  }

  private void setExternalIds(LegacyQuery query, SearchParameters searchParameters) {
    if (query.getIds() != null && !query.getIds().isEmpty()) {
      searchParameters.setExternalIds(query.getIds());
    }
  }

  private void setLimit(LegacyQuery query, SearchParameters searchParameters) {
    var clampedSize = Math.clamp(query.getSize(), 1, 100);
    searchParameters.setLimit(clampedSize);

    // Note: offset-based pagination from legacy queries cannot be directly converted
    // to the cursor-based pagination system. Offset is ignored.
    if (query.getOffset() > 0) {
      log.warn(
          "Legacy query contains offset={}, which cannot be converted to cursor-based pagination."
              + " Offset will be ignored.",
          query.getOffset());
    }
  }

  /**
   * Resolves an enhet IRI or ID to an internal enhet ID. Returns null if the enhet cannot be found.
   *
   * @param iri The IRI or ID to resolve
   * @return The internal enhet ID, or null if not found
   */
  private String resolveEnhetId(String iri) {
    try {
      var enhet = enhetService.findById(iri);
      if (enhet == null) {
        log.warn("Could not resolve enhet IRI/ID: {}", iri);
        return null;
      }
      return enhet.getId();
    } catch (Exception e) {
      log.warn("Error resolving enhet IRI/ID: {}", iri, e);
      return null;
    }
  }

  @SuppressWarnings("java:S1192") // Easier to handle mappings with string literals
  private String mapTypeToEntity(String legacyType) {
    return switch (legacyType) {
      case "Journalpost" -> "Journalpost";
      case "Moetemappe" -> "Moetemappe";
      case "Moetesak" -> "Moetesak";
      case "Møtesaksregistrering" -> "Moetesak";
      case "KommerTilBehandlingMøtesaksregistrering" -> "Moetesak";
      case "Saksmappe" -> "Saksmappe";
      default -> {
        log.warn("Unknown legacy type: {}", legacyType);
        yield null;
      }
    };
  }

  /**
   * Processes Elasticsearch date math suffixes and performs the corresponding date rounding. Legacy
   * queries contain date math suffixes (e.g., "||/d", "||/M", "||/y") which need to be properly
   * handled for compatibility with the new search API.
   *
   * <p>Note: This only handles rounding operations on exact dates, relative dates are kept as-is.
   */
  private String stripESDateMathSuffix(String dateTimeString) {
    if (dateTimeString == null) {
      return null;
    }

    // Check if there's a date math suffix
    if (!dateTimeString.contains(DATE_MATH_SUFFIX)) {
      return dateTimeString;
    }

    // Extract the date part and the rounding unit
    var suffixIndex = dateTimeString.indexOf(DATE_MATH_SUFFIX);
    var datePartOnly = dateTimeString.substring(0, suffixIndex);
    var roundingUnit =
        dateTimeString.substring(suffixIndex + DATE_MATH_SUFFIX.length()); // Get character(s) after
    // "||/"

    try {
      // Parse as datetime or date
      var hasTimeComponent = datePartOnly.contains("T");
      var dateTime =
          hasTimeComponent
              ? LocalDateTime.parse(datePartOnly)
              : LocalDate.parse(datePartOnly).atStartOfDay();

      // Apply rounding based on the unit
      var rounded =
          switch (roundingUnit) {
            case "d" -> dateTime.toLocalDate().atStartOfDay();
            case "M" -> dateTime.withDayOfMonth(1).toLocalDate().atStartOfDay();
            case "y" -> dateTime.withDayOfYear(1).toLocalDate().atStartOfDay();
            case "w" ->
                dateTime
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .toLocalDate()
                    .atStartOfDay();
            case "h", "H" -> dateTime.withMinute(0).withSecond(0).withNano(0);
            case "m" -> dateTime.withSecond(0).withNano(0);
            case "s" -> dateTime.withNano(0);
            default -> {
              log.warn("Unknown date math rounding unit: {}", roundingUnit);
              yield dateTime;
            }
          };

      // Return as date-only if the result is at start of day, otherwise include time
      if (rounded.toLocalTime().equals(LocalTime.MIDNIGHT)) {
        return rounded.toLocalDate().toString();
      } else {
        return rounded.toString();
      }

    } catch (DateTimeParseException e) {
      log.warn("Could not parse date string for rounding: {}", datePartOnly, e);
      // Return the stripped value as fallback
      return datePartOnly;
    }
  }

  /** Maps legacy sort field names to new sort field names. */
  @SuppressWarnings("java:S1192") // Easier to handle mappings with string literals
  private String mapField(String legacyFieldName) {
    return switch (legacyFieldName) {
      case "arkivskaperSorteringNavn" -> "administrativEnhetNavn";
      case "dokumentetsDato" -> "dokumentetsDato";
      case "journaldato" -> "journaldato";
      case "journalpostnummer" -> "journalpostnummer";
      case "journalpostnummer_sort" -> "journalpostnummer";
      case "journalposttype" -> "journalposttype";
      case "korrespondansepart.korrespondansepartNavn" -> "korrespondansepartNavn";
      case "moetedato" -> "moetedato";
      case "publisertDato" -> "publisertDato";
      case "sakssekvensnummer_sort" -> "sakssekvensnummer";
      case "skjerming.skjermingshjemmel" -> "skjermingshjemmel";
      case "_score" -> "score";
      case "search_korrespodansepart_sort" -> "korrespondansepartNavn";
      case "search_saksaar" -> "saksaar";
      case "search_sakssekvensnummer" -> "sakssekvensnummer";
      case "search_tittel" -> "tittel";
      case "search_tittel_sort" -> "tittel";
      case "standardDato" -> "standardDato";
      default -> {
        log.warn("Unknown legacy sort field: {}", legacyFieldName);
        yield null;
      }
    };
  }

  private void processSearchTerms(LegacyQuery query, SearchParameters searchParameters) {
    if (query.getSearchTerms() == null) {
      return;
    }

    for (var searchTerm : query.getSearchTerms()) {
      processSearchTerm(searchTerm, searchParameters);
    }
  }

  private void processSearchTerm(
      LegacyQuery.SearchTerm searchTerm, SearchParameters searchParameters) {
    var term = searchTerm.getSearchTerm();
    var field = mapField(searchTerm.getField());
    var operator = searchTerm.getOperator();

    if (term == null || field == null || operator == null) {
      log.warn("Incomplete search term in legacy query: {}", searchTerm.toString());
      return;
    }

    switch (field) {
      case "saksaar" -> {
        var saksaar = splitAndClean(term);
        searchParameters.setSaksaar(saksaar);
      }
      case "sakssekvensnummer" -> {
        var sakssekvensnummer = splitAndClean(term);
        searchParameters.setSakssekvensnummer(sakssekvensnummer);
      }
      case "journalpostnummer" -> {
        var journalpostnummer = splitAndClean(term);
        searchParameters.setJournalpostnummer(journalpostnummer);
      }
      case "tittel" -> {
        var query = buildTextQuery(term, operator);
        if (query != null) {
          searchParameters.setTittel(List.of(query));
        }
      }
      case "korrespondansepartNavn" -> {
        var query = buildTextQuery(term, operator);
        if (query != null) {
          searchParameters.setKorrespondansepartNavn(List.of(query));
        }
      }
      case "skjermingshjemmel" -> {
        var query = buildTextQuery(term, operator);
        if (query != null) {
          searchParameters.setSkjermingshjemmel(List.of(query));
        }
      }
      default -> {
        // Field not handled in advanced search
      }
    }
  }

  private String buildTextQuery(String term, LegacyQuery.SearchTerm.Operator operator) {
    return switch (operator) {
      case PHRASE -> buildPhraseQuery(term);
      case OR -> buildOrQuery(term);
      case AND -> buildAndQuery(term);
      case NOT_ANY -> buildNotQuery(term);
      case SIMPLE_QUERY_STRING -> term;
    };
  }

  private String buildPhraseQuery(String term) {
    // keyword:"exact phrase"
    // Wrap in quotes
    return "\"" + term.replace("\"", "\\\"") + "\"";
  }

  private String buildOrQuery(String term) {
    // Wrap all terms in parentheses and separate by |
    return Arrays.stream(term.split(WHITESPACE_SPLIT_PATTERN))
        // Escape special characters + " | * ( ) ~ \
        .map(s -> s.replaceAll(SPECIAL_CHARS_ESCAPE_PATTERN, ESCAPE_REPLACEMENT))
        .reduce((s1, s2) -> s1 + " | " + s2)
        .map(s -> "(" + s + ")")
        .orElse(term);
  }

  private String buildAndQuery(String term) {
    // (+term1 +term2)
    return Arrays.stream(term.split(WHITESPACE_SPLIT_PATTERN))
        .map(s -> s.replaceAll(SPECIAL_CHARS_ESCAPE_PATTERN, ESCAPE_REPLACEMENT))
        .map(s -> "+" + s)
        .reduce((s1, s2) -> s1 + " " + s2)
        .map(s -> "(" + s + ")")
        .orElse(term);
  }

  private String buildNotQuery(String term) {
    // (-term1 -term2)
    return Arrays.stream(term.split(WHITESPACE_SPLIT_PATTERN))
        .map(s -> s.replaceAll(SPECIAL_CHARS_ESCAPE_PATTERN, ESCAPE_REPLACEMENT))
        .map(s -> "-" + s)
        .reduce((s1, s2) -> s1 + " " + s2)
        .map(s -> "(" + s + ")")
        .orElse(term);
  }

  private List<String> splitAndClean(String term) {
    return Arrays.stream(term.split(WHITESPACE_SPLIT_PATTERN))
        .filter(StringUtils::hasText)
        .toList();
  }

  private void processFilters(LegacyQuery query, SearchParameters searchParameters) {
    if (query.getAppliedFilters() == null) {
      return;
    }

    for (var filter : query.getAppliedFilters()) {
      processFilter(filter, searchParameters);
    }
  }

  private void processFilter(LegacyQuery.QueryFilter filter, SearchParameters searchParameters) {
    switch (filter) {
      case LegacyQuery.QueryFilter.NotQueryFilter notFilter ->
          processNotQueryFilter(notFilter, searchParameters);
      case LegacyQuery.QueryFilter.PostQueryFilter postFilter ->
          processPostQueryFilter(postFilter, searchParameters);
      case LegacyQuery.QueryFilter.TermQueryFilter termFilter ->
          processTermQueryFilter(termFilter, searchParameters);
      case LegacyQuery.QueryFilter.RangeQueryFilter rangeFilter ->
          processRangeQueryFilter(rangeFilter, searchParameters);
      default -> log.warn("Unknown filter type: {}", filter.getClass());
    }
  }

  private void processNotQueryFilter(
      LegacyQuery.QueryFilter.NotQueryFilter notQueryFilter, SearchParameters searchParameters) {
    var fieldName = notQueryFilter.getFieldName();
    // NotQueryFilter represents exclusion filters. Map to exclude* fields where available.
    if (fieldName.equals("arkivskaperTransitive")
        || fieldName.equals("arkivskaperTransitive_filter")) {
      var values =
          notQueryFilter.getFieldValue().stream()
              .map(this::resolveEnhetId)
              .filter(Objects::nonNull)
              .toList();
      searchParameters.setExcludeAdministrativEnhet(values);
    } else {
      log.warn("NotQueryFilter for field '{}' cannot be converted", fieldName);
    }
  }

  private void processPostQueryFilter(
      LegacyQuery.QueryFilter.PostQueryFilter postQueryFilter, SearchParameters searchParameters) {
    var fieldName = postQueryFilter.getFieldName();
    if (fieldName.equals("arkivskaperTransitive")
        || fieldName.equals("arkivskaperTransitive_filter")) {
      // Look up IDs - convert IRIs to internal IDs
      var values =
          postQueryFilter.getFieldValue().stream()
              .map(this::resolveEnhetId)
              .filter(Objects::nonNull)
              .toList();
      searchParameters.setAdministrativEnhet(values);
    } else if (fieldName.equals("type_filter")) {
      var values =
          postQueryFilter.getFieldValue().stream()
              .map(this::mapTypeToEntity)
              .filter(Objects::nonNull)
              .toList();
      searchParameters.setEntity(values);
    } else {
      log.warn("PostQueryFilter for field '{}' cannot be converted", fieldName);
    }
  }

  private void processTermQueryFilter(
      LegacyQuery.QueryFilter.TermQueryFilter termQueryFilter, SearchParameters searchParameters) {
    var fieldName = termQueryFilter.getFieldName();
    var values = termQueryFilter.getFieldValue().stream().toList();
    if (fieldName.equals("type") || fieldName.equals("type_filter")) {
      searchParameters.setEntity(values);
    } else if (fieldName.equals("journalposttype") || fieldName.equals("journalposttype_filter")) {
      searchParameters.setJournalposttype(values);
    }
  }

  private void processRangeQueryFilter(
      LegacyQuery.QueryFilter.RangeQueryFilter rangeQueryFilter,
      SearchParameters searchParameters) {
    var fieldName = rangeQueryFilter.getFieldName();

    switch (fieldName) {
      case "dokumentetsDato" ->
          setDateRange(
              rangeQueryFilter,
              searchParameters::setDokumentetsDatoFrom,
              searchParameters::setDokumentetsDatoTo);
      case "journaldato" ->
          setDateRange(
              rangeQueryFilter,
              searchParameters::setJournaldatoFrom,
              searchParameters::setJournaldatoTo);
      case "moetedato" ->
          setDateRange(
              rangeQueryFilter,
              searchParameters::setMoetedatoFrom,
              searchParameters::setMoetedatoTo);
      case "publisertDato" ->
          setDateRange(
              rangeQueryFilter,
              searchParameters::setPublisertDatoFrom,
              searchParameters::setPublisertDatoTo);
      default ->
          log.warn(
              "RangeQueryFilter for unsupported date field '{}' cannot be converted", fieldName);
    }
  }

  private void setDateRange(
      LegacyQuery.QueryFilter.RangeQueryFilter filter,
      Consumer<String> fromSetter,
      Consumer<String> toSetter) {
    if (filter.getFrom() != null) {
      fromSetter.accept(stripESDateMathSuffix(filter.getFrom()));
    }
    if (filter.getTo() != null) {
      toSetter.accept(stripESDateMathSuffix(filter.getTo()));
    }
  }

  private void processSortParameters(LegacyQuery query, SearchParameters searchParameters) {
    if (query.getSort() != null && StringUtils.hasText(query.getSort().getFieldName())) {
      var legacySortField = query.getSort().getFieldName();
      var mappedSortField = mapField(legacySortField);
      if (mappedSortField != null) {
        searchParameters.setSortBy(mappedSortField);
      } else {
        log.warn("Legacy sort field '{}' cannot be mapped to new sort field", legacySortField);
      }

      if (query.getSort().getOrder() != null) {
        searchParameters.setSortOrder(
            query.getSort().getOrder() == LegacyQuery.Sort.SortOrder.ASC ? "asc" : "desc");
      }
    }
  }
}
