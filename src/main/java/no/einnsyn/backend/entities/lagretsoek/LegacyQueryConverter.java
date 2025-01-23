package no.einnsyn.backend.entities.lagretsoek;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.einnsyn.backend.common.search.models.SearchParameters;
import no.einnsyn.backend.entities.lagretsoek.models.LegacyQuery;
import no.einnsyn.backend.error.exceptions.EInnsynException;
import org.springframework.util.StringUtils;

public class LegacyQueryConverter {

  ObjectMapper objectMapper = new ObjectMapper();

  public SearchParameters convertLegacyQuery(String legacyQuery) throws EInnsynException {

    LegacyQuery query;
    try {
      query = objectMapper.readValue(legacyQuery, LegacyQuery.class);
    } catch (JsonProcessingException e) {
      throw new EInnsynException("Could not parse legacy query: " + e.getMessage(), e);
    }

    var searchParameters = new SearchParameters();

    // Add query
    if (StringUtils.hasText(query.getSearchTerm())) {
      searchParameters.setQuery(query.getSearchTerm());
    }

    // Filter by IDs
    if (query.getIds() != null) {
      searchParameters.setExternalIds(query.getIds());
    }

    // Add filters
    for (var filter : query.getAppliedFilters()) {
      var fieldName = filter.getFieldName();
      if (filter instanceof LegacyQuery.QueryFilter.NotQueryFilter notQueryFilter) {
        // searchParameters.addNotQueryFilter(notQueryFilter.getFieldName(),
        // notQueryFilter.getFieldValue());
      } else if (filter instanceof LegacyQuery.QueryFilter.PostQueryFilter postQueryFilter) {
        // searchParameters.addPostQueryFilter(postQueryFilter.getFieldName(),
        // postQueryFilter.getFieldValue());
      } else if (filter instanceof LegacyQuery.QueryFilter.TermQueryFilter termQueryFilter) {
        var values = termQueryFilter.getFieldValue().stream().toList();
        if (fieldName.equals("type")) {
          searchParameters.setEntity(values);
        } else if (fieldName.equals("arkivskaperTransitive")) {
          // Look up IDs
          values =
              values.stream()
                  .map(
                      iri -> {
                        var enhet = enhetService.findById(iri);
                        if (enhet == null) {
                          return null;
                        }
                        return enhet.getId();
                      })
                  .filter(id -> id != null)
                  .toList();
          searchParameters.setAdministrativEnhet(values);
        } else if (fieldName.equals("journalposttype")) {
          searchParameters.setJournalposttype(values);
        } else if (fieldName.equals("journalpostnummer")) {
          searchParameters.setJournalpostnummer(values);
        } else if (fieldName.equals("search_saksaar")) {
          searchParameters.setSaksaar(values);
        } else if (fieldName.equals("search_sakssekvensnummer")) {
          searchParameters.setSakssekvensnummer(values);
        } else if (fieldName.equals("møtesaksår")) {
          searchParameters.setMoetesaksaar(values);
        } else if (fieldName.equals("møtesakssekvensnummer")) {
          searchParameters.setMoetesakssekvensnummer(values);
        }
      } else if (filter instanceof LegacyQuery.QueryFilter.RangeQueryFilter rangeQueryFilter) {
        if (fieldName.equals("standardDato")) {
          // searchParameters.setStandardDatoAfter(rangeQueryFilter.getFrom());
          // searchParameters.setStandardDatoBefore(rangeQueryFilter.getTo());
        } else if (fieldName.equals("publisertDato")) {
          searchParameters.setPublisertDatoAfter(rangeQueryFilter.getFrom());
          searchParameters.setPublisertDatoBefore(rangeQueryFilter.getTo());
        } else if (fieldName.equals("journaldato")) {
          // searchParameters.setJournaldatoAfter(rangeQueryFilter.getFrom());
          // searchParameters.setJournaldatoBefore(rangeQueryFilter.getTo());
        } else if (fieldName.equals("dokumentetsDato")) {
          // searchParameters.setDokumentetsDatoAfter(rangeQueryFilter.getFrom());
          // searchParameters.setDokumentetsDatoBefore(rangeQueryFilter.getTo());
        } else if (fieldName.equals("moetedato")) {
          searchParameters.setMoetedatoAfter(rangeQueryFilter.getFrom());
          searchParameters.setMoetedatoBefore(rangeQueryFilter.getTo());
        }
      }
    }

    return searchParameters;
  }
}
