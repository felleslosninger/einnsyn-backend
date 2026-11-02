package no.einnsyn.backend.entities.lagretsoek;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.entities.enhet.EnhetService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

class LegacyQueryConverterUnitTest {

  @ParameterizedTest
  @ValueSource(strings = {"postQueryFilter", "notQueryFilter"})
  void unresolvedEnhetFilterFailsConversion(String filterType) throws EInnsynException {
    var enhetService = mock(EnhetService.class);
    when(enhetService.find("missing-enhet-id")).thenReturn(null);
    var converter = new LegacyQueryConverter(JsonMapper.builder().build(), enhetService);
    var legacyQueryJson =
        """
        {
          "appliedFilters": [
            {
              "type": "%s",
              "fieldName": "arkivskaperTransitive",
              "fieldValue": ["missing-enhet-id"]
            }
          ]
        }
        """
            .formatted(filterType);

    var exception =
        assertThrows(EInnsynException.class, () -> converter.convertLegacyQuery(legacyQueryJson));

    assertEquals("legacyQueryConversionError", exception.getType());
    assertEquals(
        "Could not resolve enhet IRI/ID in legacy query: missing-enhet-id", exception.getMessage());
  }
}
