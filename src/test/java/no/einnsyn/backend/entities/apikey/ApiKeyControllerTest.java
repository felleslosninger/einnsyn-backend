package no.einnsyn.backend.entities.apikey;

import static org.junit.jupiter.api.Assertions.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.gson.reflect.TypeToken;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.apikey.models.ApiKeyDTO;
import no.einnsyn.backend.entities.base.BaseService;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiKeyControllerTest extends EinnsynControllerTestBase {

  @Test
  void testApiKeyLifecycle() throws Exception {
    var response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    var enhetDTO = gson.fromJson(response.getBody(), EnhetDTO.class);

    response = post("/enhet/" + enhetDTO.getId() + "/apiKey", getApiKeyJSON());
    var apiKeyDTO = gson.fromJson(response.getBody(), ApiKeyDTO.class);
    assertNotNull(apiKeyDTO.getId());
    assertNotNull(apiKeyDTO.getSecretKey()); // Only visible on creation
    assertEquals("ApiKeyName", apiKeyDTO.getName());
    assertEquals(enhetDTO.getId(), apiKeyDTO.getEnhet().getId());

    response = get("/apiKey/" + apiKeyDTO.getId());
    apiKeyDTO = gson.fromJson(response.getBody(), ApiKeyDTO.class);
    assertNotNull(apiKeyDTO.getId());
    assertEquals("ApiKeyName", apiKeyDTO.getName());
    assertNull(apiKeyDTO.getSecretKey());
    assertEquals(enhetDTO.getId(), apiKeyDTO.getEnhet().getId());

    response = get("/enhet/" + enhetDTO.getId() + "/apiKey");
    var apiKeyListType = new TypeToken<PaginatedList<ApiKeyDTO>>() {}.getType();
    PaginatedList<ApiKeyDTO> apiKeyResultList = gson.fromJson(response.getBody(), apiKeyListType);
    assertNotNull(apiKeyResultList);
    assertNotNull(apiKeyResultList.getItems());
    assertEquals(1, apiKeyResultList.getItems().size());
    apiKeyDTO = apiKeyResultList.getItems().getFirst();
    assertNotNull(apiKeyDTO.getId());
    assertEquals("ApiKeyName", apiKeyDTO.getName());
    assertNull(apiKeyDTO.getSecretKey());

    var updateJSON = getApiKeyJSON();
    updateJSON.put("name", "UpdatedApiKey");
    response = patch("/apiKey/" + apiKeyDTO.getId(), updateJSON);
    apiKeyDTO = gson.fromJson(response.getBody(), ApiKeyDTO.class);
    assertNotNull(apiKeyDTO.getId());
    assertEquals("UpdatedApiKey", apiKeyDTO.getName());
    assertNull(apiKeyDTO.getSecretKey());

    response = delete("/apiKey/" + apiKeyDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    response = get("/apiKey/" + apiKeyDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    response = get("/enhet/" + enhetDTO.getId() + "/apiKey");
    apiKeyResultList = gson.fromJson(response.getBody(), apiKeyListType);
    assertNotNull(apiKeyResultList);
    assertNotNull(apiKeyResultList.getItems());
    assertEquals(0, apiKeyResultList.getItems().size());

    response = delete("/enhet/" + enhetDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testSecretKeyIsNotLogged() throws Exception {
    var logger = (Logger) LoggerFactory.getLogger(BaseService.class);
    var listAppender = new ListAppender<ILoggingEvent>();
    listAppender.start();
    logger.addAppender(listAppender);

    try {
      var response = post("/enhet/" + journalenhetId + "/apiKey", getApiKeyJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      var apiKeyDTO = gson.fromJson(response.getBody(), ApiKeyDTO.class);
      var secretKey = apiKeyDTO.getSecretKey();
      assertNotNull(secretKey);

      // The returned secret must be the one the key was created with
      response = get("/apiKey/" + apiKeyDTO.getId(), secretKey);
      assertEquals(HttpStatus.OK, response.getStatusCode());

      assertFalse(
          listAppender.list.isEmpty(),
          "No BaseService log events captured; test would not verify secret redaction");
      for (var event : listAppender.list) {
        assertFalse(event.getFormattedMessage().contains(secretKey), "Secret found in log message");
        var keyValuePairs = event.getKeyValuePairs();
        if (keyValuePairs != null) {
          for (var keyValuePair : keyValuePairs) {
            assertFalse(
                String.valueOf(keyValuePair.value).contains(secretKey),
                "Secret found in log key-value " + keyValuePair.key);
          }
        }
      }

      assertEquals(HttpStatus.OK, delete("/apiKey/" + apiKeyDTO.getId()).getStatusCode());
    } finally {
      logger.detachAppender(listAppender);
    }
  }
}
