package no.einnsyn.backend.entities.apikey;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.reflect.TypeToken;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.responses.models.ListResponseBody;
import no.einnsyn.backend.entities.apikey.models.ApiKeyDTO;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import org.junit.jupiter.api.Test;
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
    var apiKeyListType = new TypeToken<ListResponseBody<ApiKeyDTO>>() {}.getType();
    ListResponseBody<ApiKeyDTO> apiKeyResultList =
        gson.fromJson(response.getBody(), apiKeyListType);
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
}
