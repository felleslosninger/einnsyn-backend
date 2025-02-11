package no.einnsyn.backend.entities.apikey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.reflect.TypeToken;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.apikey.models.ApiKeyDTO;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiKeyApiKeyAuthTest extends EinnsynControllerTestBase {

  @Test
  void testListApiKeys() throws Exception {
    // Add API keys to two enhets
    var response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    var enhet1DTO = gson.fromJson(response.getBody(), EnhetDTO.class);
    response = post("/enhet/" + enhet1DTO.getId() + "/apiKey", getApiKeyJSON());
    var apiKey11DTO = gson.fromJson(response.getBody(), ApiKeyDTO.class);
    response = post("/enhet/" + enhet1DTO.getId() + "/apiKey", getApiKeyJSON());
    var apiKey12DTO = gson.fromJson(response.getBody(), ApiKeyDTO.class);

    response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    var enhet2DTO = gson.fromJson(response.getBody(), EnhetDTO.class);
    response = post("/enhet/" + enhet2DTO.getId() + "/apiKey", getApiKeyJSON());
    var apiKey21DTO = gson.fromJson(response.getBody(), ApiKeyDTO.class);
    response = post("/enhet/" + enhet2DTO.getId() + "/apiKey", getApiKeyJSON());
    var apiKey22DTO = gson.fromJson(response.getBody(), ApiKeyDTO.class);

    // Verify that we cannot list all keys
    response = get("/apiKey");
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Fail to list keys when not authenticated
    response = getAnon("/enhet/" + enhet1DTO.getId() + "/apiKey");
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // List keys for enhet1, authenticated as enhet1
    response = get("/enhet/" + enhet1DTO.getId() + "/apiKey", apiKey11DTO.getSecretKey());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var type = new TypeToken<PaginatedList<ApiKeyDTO>>() {}.getType();
    PaginatedList<ApiKeyDTO> apiKeyList = gson.fromJson(response.getBody(), type);
    assertEquals(2, apiKeyList.getItems().size());

    // Fail to list keys for enhet1, authenticated as enhet2
    response = get("/enhet/" + enhet1DTO.getId() + "/apiKey", apiKey21DTO.getSecretKey());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // List keys for enhet2, authenticated as enhet2
    response = get("/enhet/" + enhet2DTO.getId() + "/apiKey", apiKey21DTO.getSecretKey());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    apiKeyList = gson.fromJson(response.getBody(), type);
    assertEquals(2, apiKeyList.getItems().size());

    // Fail to list keys for enhet2, authenticated as enhet1
    response = get("/enhet/" + enhet2DTO.getId() + "/apiKey", apiKey11DTO.getSecretKey());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Clean up
    response = delete("/enhet/" + enhet1DTO.getId(), apiKey12DTO.getSecretKey());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/enhet/" + enhet2DTO.getId(), apiKey22DTO.getSecretKey());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testGetApiKey() throws Exception {
    // Add API key to two Enhets
    var response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    var enhet1DTO = gson.fromJson(response.getBody(), EnhetDTO.class);
    response = post("/enhet/" + enhet1DTO.getId() + "/apiKey", getApiKeyJSON());
    var apiKey1DTO = gson.fromJson(response.getBody(), ApiKeyDTO.class);
    response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    var enhet2DTO = gson.fromJson(response.getBody(), EnhetDTO.class);
    response = post("/enhet/" + enhet2DTO.getId() + "/apiKey", getApiKeyJSON());
    var apiKey2DTO = gson.fromJson(response.getBody(), ApiKeyDTO.class);

    // Fail to get key when not authenticated
    response = getAnon("/apiKey/" + apiKey1DTO.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Fail to get key when authorized as another Enhet
    response = get("/apiKey/" + apiKey1DTO.getId(), apiKey2DTO.getSecretKey());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Get key when authenticated
    response = get("/apiKey/" + apiKey1DTO.getId(), apiKey1DTO.getSecretKey());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var apiKeyDTO2 = gson.fromJson(response.getBody(), ApiKeyDTO.class);
    assertEquals(apiKey1DTO.getId(), apiKeyDTO2.getId());
    assertEquals(apiKey1DTO.getName(), apiKeyDTO2.getName());
    assertEquals(apiKey1DTO.getEnhet().getId(), apiKeyDTO2.getEnhet().getId());

    // Clean up
    response = delete("/enhet/" + enhet1DTO.getId(), apiKey1DTO.getSecretKey());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/enhet/" + enhet2DTO.getId(), apiKey2DTO.getSecretKey());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testInsertApiKey() throws Exception {
    // Add two Enhets with keys
    var response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    var enhet1DTO = gson.fromJson(response.getBody(), EnhetDTO.class);
    response = post("/enhet/" + enhet1DTO.getId() + "/apiKey", getApiKeyJSON());
    var apiKey1DTO = gson.fromJson(response.getBody(), ApiKeyDTO.class);
    response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    var enhet2DTO = gson.fromJson(response.getBody(), EnhetDTO.class);
    response = post("/enhet/" + enhet2DTO.getId() + "/apiKey", getApiKeyJSON());
    var apiKey2DTO = gson.fromJson(response.getBody(), ApiKeyDTO.class);

    // Fail to insert key when not authenticated
    response = postAnon("/enhet/" + enhet1DTO.getId() + "/apiKey", getApiKeyJSON());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Fail to insert on another Enhet
    response =
        post("/enhet/" + enhet1DTO.getId() + "/apiKey", getApiKeyJSON(), apiKey2DTO.getSecretKey());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Insert key when authenticated
    response = postAdmin("/enhet/" + enhet1DTO.getId() + "/apiKey", getApiKeyJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var apiKeyDTO = gson.fromJson(response.getBody(), ApiKeyDTO.class);
    assertNotNull(apiKeyDTO.getId());
    assertNotNull(apiKeyDTO.getSecretKey());
    assertEquals("ApiKeyName", apiKeyDTO.getName());
    assertEquals(enhet1DTO.getId(), apiKeyDTO.getEnhet().getId());

    // Clean up
    response = delete("/enhet/" + enhet1DTO.getId(), apiKey1DTO.getSecretKey());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/enhet/" + enhet2DTO.getId(), apiKey2DTO.getSecretKey());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testUpdateApiKey() throws Exception {
    // Add Enhets with keys
    var response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    var enhet1DTO = gson.fromJson(response.getBody(), EnhetDTO.class);
    response = post("/enhet/" + enhet1DTO.getId() + "/apiKey", getApiKeyJSON());
    var apiKey1DTO = gson.fromJson(response.getBody(), ApiKeyDTO.class);
    response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    var enhet2DTO = gson.fromJson(response.getBody(), EnhetDTO.class);
    response = post("/enhet/" + enhet2DTO.getId() + "/apiKey", getApiKeyJSON());
    var apiKey2DTO = gson.fromJson(response.getBody(), ApiKeyDTO.class);

    // Fail to update key when not authenticated
    var updateJSON = getApiKeyJSON();
    updateJSON.put("name", "UpdatedApiKey");
    response = patchAnon("/apiKey/" + apiKey1DTO.getId(), updateJSON);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Fail to update on another Enhet
    response = patch("/apiKey/" + apiKey1DTO.getId(), updateJSON, apiKey2DTO.getSecretKey());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Update key when authenticated
    response = patch("/apiKey/" + apiKey1DTO.getId(), updateJSON, apiKey1DTO.getSecretKey());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var updatedApiKeyDTO = gson.fromJson(response.getBody(), ApiKeyDTO.class);
    assertEquals("UpdatedApiKey", updatedApiKeyDTO.getName());

    // Clean up
    response = delete("/enhet/" + enhet1DTO.getId(), apiKey1DTO.getSecretKey());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/enhet/" + enhet2DTO.getId(), apiKey2DTO.getSecretKey());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testDeleteApiKey() throws Exception {
    // Add Enhets with keys
    var response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    var enhet1DTO = gson.fromJson(response.getBody(), EnhetDTO.class);
    response = post("/enhet/" + enhet1DTO.getId() + "/apiKey", getApiKeyJSON());
    var apiKey1DTO = gson.fromJson(response.getBody(), ApiKeyDTO.class);
    response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    var enhet2DTO = gson.fromJson(response.getBody(), EnhetDTO.class);
    response = post("/enhet/" + enhet2DTO.getId() + "/apiKey", getApiKeyJSON());
    var apiKey2DTO = gson.fromJson(response.getBody(), ApiKeyDTO.class);

    // Fail to delete key when not authenticated
    response = deleteAnon("/apiKey/" + apiKey1DTO.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Fail to delete on another Enhet
    response = delete("/apiKey/" + apiKey1DTO.getId(), apiKey2DTO.getSecretKey());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Delete key when authenticated
    response = delete("/apiKey/" + apiKey1DTO.getId(), apiKey1DTO.getSecretKey());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Clean up
    response = delete("/enhet/" + enhet1DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/enhet/" + enhet2DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}
