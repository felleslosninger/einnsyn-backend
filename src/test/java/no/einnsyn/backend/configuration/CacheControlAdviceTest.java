package no.einnsyn.backend.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import no.einnsyn.backend.EinnsynControllerTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CacheControlAdviceTest extends EinnsynControllerTestBase {

  @Test
  void anonymousGetIsCachedBriefly() throws Exception {
    var response = getAnon("/enhet/" + journalenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var cacheControl = response.getHeaders().getCacheControl();
    assertTrue(cacheControl.contains("public"), cacheControl);
    assertTrue(cacheControl.contains("max-age=120"), cacheControl);
    assertVaryOnCredentials(response);
  }

  @Test
  void authenticatedGetIsNotCached() throws Exception {
    var response = get("/enhet/" + journalenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getHeaders().getCacheControl().contains("no-store"));
    assertVaryOnCredentials(response);
  }

  @Test
  void anonymousNotFoundIsNotCached() throws Exception {
    var response = getAnon("/enhet/enhet_doesnotexist");
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertTrue(response.getHeaders().getCacheControl().contains("no-store"));
  }

  private void assertVaryOnCredentials(ResponseEntity<String> response) {
    var vary = response.getHeaders().getVary().stream().map(String::toLowerCase).toList();
    assertTrue(vary.containsAll(List.of("authorization", "api-key", "acting-as")), vary.toString());
  }
}
