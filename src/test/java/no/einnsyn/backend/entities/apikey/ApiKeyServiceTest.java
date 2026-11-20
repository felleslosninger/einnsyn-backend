package no.einnsyn.backend.entities.apikey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import no.einnsyn.backend.EinnsynServiceTestBase;
import no.einnsyn.backend.authentication.EInnsynAuthentication;
import no.einnsyn.backend.authentication.EInnsynPrincipalEnhet;
import no.einnsyn.backend.common.exceptions.models.AuthorizationException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.apikey.models.ApiKeyDTO;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class ApiKeyServiceTest extends EinnsynServiceTestBase {

  @BeforeEach
  void setupAuthentication() {
    var apiKey = apiKeyService.findBySecretKey(journalenhetKey);
    var enhet = apiKey.getEnhet();
    var principal =
        new EInnsynPrincipalEnhet(
            "ApiKey", apiKey.getId(), enhet.getId(), enhet.getOrgnummer(), false);

    var securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(new EInnsynAuthentication(principal, null, null));
    SecurityContextHolder.setContext(securityContext);
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  /** An ApiKey without an Enhet should be rejected, not throw a NullPointerException. */
  @Test
  void addApiKeyWithoutEnhetIsRejected() {
    var dto = new ApiKeyDTO();
    dto.setName("ApiKeyWithoutEnhet");

    var exception = assertThrows(AuthorizationException.class, () -> apiKeyService.add(dto));
    assertEquals("EnhetId is required", exception.getMessage());
  }

  /** An ApiKey with an Enhet field that doesn't contain an ID should be rejected as well. */
  @Test
  void addApiKeyWithoutEnhetIdIsRejected() {
    var dto = new ApiKeyDTO();
    dto.setName("ApiKeyWithoutEnhetId");
    dto.setEnhet(new ExpandableField<EnhetDTO>((String) null));

    var exception = assertThrows(AuthorizationException.class, () -> apiKeyService.add(dto));
    assertEquals("EnhetId is required", exception.getMessage());
  }
}
