package no.einnsyn.backend.entities.lagretsoek;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
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

  private final ObjectMapper objectMapper;
  private final EnhetService enhetService;

  public LegacyQueryConverter(ObjectMapper objectMapper, EnhetService enhetService) {
    this.objectMapper = objectMapper;
    this.enhetService = enhetService;
  }

  public SearchParameters convertLegacyQuery(String legacyQuery) throws EInnsynException {

    LegacyQuery query;
    try {
      query = objectMapper.readValue(legacyQuery, LegacyQuery.class);
    } catch (JsonProcessingException e) {
      throw new EInnsynException(
          "Could not parse legacy query: " + e.getMessage(), e, "legacyQueryConversionError");
    }

    var searchParameters = new SearchParameters();

    // Add query
    if (StringUtils.hasText(query.getSearchTerm())) {
      searchParameters.setQuery(query.getSearchTerm());
    }

    // Search terms per field (advanced query)
    if (query.getSearchTerms() != null) {

      for (var searchTerm : query.getSearchTerms()) {
        var term = searchTerm.getSearchTerm();
        var field = mapField(searchTerm.getField());
        var operator = searchTerm.getOperator();

        if (term == null || field == null || operator == null) {
          log.warn("Incomplete search term in legacy query: {}", searchTerm);
          continue;
        }

        // Saksaar
        if (field.equals("saksaar")) {
          var saksaar =
              Arrays.stream(term.split("\\s+"))
                  .map(String::trim)
                  .filter(s -> StringUtils.hasText(s))
                  .toList();
          searchParameters.setSaksaar(saksaar);
        }

        // Sakssekvensnummer
        if (field.equals("sakssekvensnummer")) {
          var sakssekvensnummer =
              Arrays.stream(term.split("\\s+"))
                  .map(String::trim)
                  .filter(s -> StringUtils.hasText(s))
                  .toList();
          searchParameters.setSakssekvensnummer(sakssekvensnummer);
        }

        // Journalpostnummer
        if (field.equals("journalpostnummer")) {
          var journalpostnummer =
              Arrays.stream(term.split("\\s+"))
                  .map(String::trim)
                  .filter(s -> StringUtils.hasText(s))
                  .toList();
          searchParameters.setJournalpostnummer(journalpostnummer);
        }

        // Handle text search fields (tittel, korrespondansepartNavn, skjermingshjemmel)
        if (field.equals("tittel")
            || field.equals("korrespondansepartNavn")
            || field.equals("skjermingshjemmel")) {
          String processedQuery = null;

          if (LegacyQuery.SearchTerm.Operator.PHRASE.equals(operator)) {
            // keyword:"exact phrase"
            // Wrap in quotes
            processedQuery = "\"" + term.replace("\"", "\\\"") + "\"";
          } else if (LegacyQuery.SearchTerm.Operator.OR.equals(operator)) {
            // Wrap all terms in parentheses and separate by |
            processedQuery =
                Arrays.stream(term.split("\\s+"))
                    .map(String::trim)
                    .filter(s -> StringUtils.hasText(s))
                    .map(s -> s.replaceAll("([+\"|*()~\\\\])", "\\\\$1"))
                    .reduce((s1, s2) -> s1 + " | " + s2)
                    .map(s -> "(" + s + ")")
                    .orElse(term);
          } else if (LegacyQuery.SearchTerm.Operator.AND.equals(operator)) {
            // (+term1 +term2)
            processedQuery =
                Arrays.stream(term.split("\\s+"))
                    .map(String::trim)
                    .filter(s -> StringUtils.hasText(s))
                    .map(s -> s.replaceAll("([+\"|*()~\\\\])", "\\\\$1"))
                    .map(s -> "+" + s)
                    .reduce((s1, s2) -> s1 + " " + s2)
                    .map(s -> "(" + s + ")")
                    .orElse(term);
          }

          // Set the appropriate field based on which one it is
          if (processedQuery != null) {
            switch (field) {
              case "tittel" -> searchParameters.setTittel(List.of(processedQuery));
              case "korrespondansepartNavn" ->
                  searchParameters.setKorrespondansepartNavn(List.of(processedQuery));
              case "skjermingshjemmel" ->
                  searchParameters.setSkjermingshjemmel(List.of(processedQuery));
            }
          }
        }
      }
    }

    // Filter by IDs
    if (query.getIds() != null && !query.getIds().isEmpty()) {
      searchParameters.setExternalIds(query.getIds());
    }

    // Set limit
    var clampedSize = Math.min(100, Math.max(1, query.getSize()));
    searchParameters.setLimit(clampedSize);

    // Note: offset-based pagination from legacy queries cannot be directly converted
    // to the cursor-based pagination system. Offset is ignored.
    if (query.getOffset() > 0) {
      log.warn(
          "Legacy query contains offset={}, which cannot be converted to cursor-based pagination."
              + " Offset will be ignored.",
          query.getOffset());
    }

    // Add filters
    if (query.getAppliedFilters() != null) {
      for (var filter : query.getAppliedFilters()) {
        var fieldName = filter.getFieldName();

        // Exclude
        if (filter instanceof LegacyQuery.QueryFilter.NotQueryFilter notQueryFilter) {
          // NotQueryFilter represents exclusion filters. Map to exclude* fields where available.
          if (fieldName.equals("arkivskaperTransitive")
              || fieldName.equals("arkivskaperTransitive_filter")) {
            var values =
                notQueryFilter.getFieldValue().stream()
                    .map(this::resolveEnhetId)
                    .filter(id -> id != null)
                    .toList();
            searchParameters.setExcludeAdministrativEnhet(values);
          } else {
            log.warn("NotQueryFilter for field '{}' cannot be converted", fieldName);
          }
        }

        // Include (postQueryFilter, used in legacy for inclusion filters)
        else if (filter instanceof LegacyQuery.QueryFilter.PostQueryFilter postQueryFilter) {
          if (fieldName.equals("arkivskaperTransitive")
              || fieldName.equals("arkivskaperTransitive_filter")) {
            // Look up IDs - convert IRIs to internal IDs
            var values =
                postQueryFilter.getFieldValue().stream()
                    .map(this::resolveEnhetId)
                    .filter(id -> id != null)
                    .toList();
            searchParameters.setAdministrativEnhet(values);
          } else if (fieldName.equals("type_filter")) {
            var values =
                postQueryFilter.getFieldValue().stream()
                    .map(this::mapTypeToEntity)
                    .filter(type -> type != null)
                    .toList();
            searchParameters.setEntity(values);
          } else {
            log.warn("PostQueryFilter for field '{}' cannot be converted", fieldName);
          }
        }

        // TermQueryFilter
        else if (filter instanceof LegacyQuery.QueryFilter.TermQueryFilter termQueryFilter) {
          var values = termQueryFilter.getFieldValue().stream().toList();
          if (fieldName.equals("type") || fieldName.equals("type_filter")) {
            searchParameters.setEntity(values);
          } else if (fieldName.equals("journalposttype")
              || fieldName.equals("journalposttype_filter")) {
            searchParameters.setJournalposttype(values);
          }
        } else if (filter instanceof LegacyQuery.QueryFilter.RangeQueryFilter rangeQueryFilter) {
          if (fieldName.equals("dokumentetsDato")) {
            if (rangeQueryFilter.getFrom() != null) {
              searchParameters.setDokumentetsDatoFrom(rangeQueryFilter.getFrom());
            }
            if (rangeQueryFilter.getTo() != null) {
              searchParameters.setDokumentetsDatoTo(rangeQueryFilter.getTo());
            }
          } else if (fieldName.equals("journaldato")) {
            if (rangeQueryFilter.getFrom() != null) {
              searchParameters.setJournaldatoFrom(rangeQueryFilter.getFrom());
            }
            if (rangeQueryFilter.getTo() != null) {
              searchParameters.setJournaldatoTo(rangeQueryFilter.getTo());
            }
          } else if (fieldName.equals("moetedato")) {
            if (rangeQueryFilter.getFrom() != null) {
              searchParameters.setMoetedatoFrom(rangeQueryFilter.getFrom());
            }
            if (rangeQueryFilter.getTo() != null) {
              searchParameters.setMoetedatoTo(rangeQueryFilter.getTo());
            }
          } else if (fieldName.equals("publisertDato")) {
            if (rangeQueryFilter.getFrom() != null) {
              searchParameters.setPublisertDatoFrom(rangeQueryFilter.getFrom());
            }
            if (rangeQueryFilter.getTo() != null) {
              searchParameters.setPublisertDatoTo(rangeQueryFilter.getTo());
            }
          } else {
            // Legacy date fields that don't have equivalents in the new API
            log.warn(
                "RangeQueryFilter for unsupported date field '{}' cannot be converted", fieldName);
          }
        }
      }
    }

    // Convert sort parameters
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

    return searchParameters;
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

  private String mapTypeToEntity(String legacyType) {
    return switch (legacyType) {
      case "Journalpost" -> "Journalpost";
      case "Moetemappe" -> "Moetemappe";
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
   * Maps legacy sort field names to new sort field names.
   *
   * @param legacyFieldName The legacy sort field name
   * @return The new sort field name, or null if no mapping exists
   */
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
}
