package no.einnsyn.backend.entities.apikey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.UUID;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.authentication.bruker.models.TokenResponse;
import no.einnsyn.backend.entities.apikey.models.ApiKey;
import no.einnsyn.backend.entities.apikey.models.ApiKeyDTO;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import no.einnsyn.backend.utils.HashUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

/**
 * An ApiKey can be bound to a Bruker instead of an Enhet. These tests verify that the owning Bruker
 * (and an admin) can read, update and delete such a key.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiKeyBrukerAuthTest extends EinnsynControllerTestBase {

  private BrukerDTO brukerDTO;
  private String brukerToken;

  @BeforeEach
  void createBruker() throws Exception {
    var brukerJSON = getBrukerJSON();
    var response = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);

    var brukerObj = brukerService.findOrThrow(brukerDTO.getId());
    response = patch("/bruker/" + brukerDTO.getId() + "/activate/" + brukerObj.getSecret());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var loginRequest = new JSONObject();
    loginRequest.put("username", brukerDTO.getEmail());
    loginRequest.put("password", brukerJSON.getString("password"));
    response = post("/auth/token", loginRequest);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    brukerToken = gson.fromJson(response.getBody(), TokenResponse.class).getToken();
  }

  @AfterEach
  void deleteBruker() throws Exception {
    // Deleting a Bruker also deletes its ApiKeys
    var response = deleteAdmin("/bruker/" + brukerDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  /**
   * Create an ApiKey bound to the test Bruker. There is no API endpoint for this, so we insert it
   * directly, the same way EinnsynTestBase creates the Enhet-bound keys.
   */
  private ApiKey createBrukerApiKey(String secret) {
    var bruker = brukerRepository.findById(brukerDTO.getId()).orElseThrow();
    var apiKey = new ApiKey();
    apiKey.setBruker(bruker);
    apiKey.setName("BrukerApiKey");
    apiKey.setSecret(HashUtils.sha256Hex(secret));
    apiKey.setAccessibleAfter(Instant.now());
    return apiKeyRepository.saveAndFlush(apiKey);
  }

  private static String newSecret() {
    return "secret_bruker_" + UUID.randomUUID();
  }

  @Test
  void testGetBrukerScopedApiKey() throws Exception {
    var secret = newSecret();
    var apiKeyId = createBrukerApiKey(secret).getId();

    // The owning Bruker can read the key, authenticated with the key itself
    var response = get("/apiKey/" + apiKeyId, secret);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var apiKeyDTO = gson.fromJson(response.getBody(), ApiKeyDTO.class);
    assertEquals(apiKeyId, apiKeyDTO.getId());
    assertEquals("BrukerApiKey", apiKeyDTO.getName());
    assertNotNull(apiKeyDTO.getBruker());
    assertEquals(brukerDTO.getId(), apiKeyDTO.getBruker().getId());
    assertNull(apiKeyDTO.getEnhet());
    assertNull(apiKeyDTO.getSecretKey());

    // The owning Bruker can read the key, authenticated with a JWT
    response = get("/apiKey/" + apiKeyId, brukerToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // An admin can read the key
    response = getAdmin("/apiKey/" + apiKeyId);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // An unrelated Enhet can not read the key
    response = get("/apiKey/" + apiKeyId, journalenhetKey);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Anonymous users can not read the key
    response = getAnon("/apiKey/" + apiKeyId);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void testUpdateBrukerScopedApiKey() throws Exception {
    var secret = newSecret();
    var apiKeyId = createBrukerApiKey(secret).getId();

    var updateJSON = new JSONObject();
    updateJSON.put("name", "UpdatedByBruker");

    // Anonymous users can not update the key
    var response = patchAnon("/apiKey/" + apiKeyId, updateJSON);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // An unrelated Enhet can not update the key
    response = patch("/apiKey/" + apiKeyId, updateJSON, journalenhetKey);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // The owning Bruker can update the key
    response = patch("/apiKey/" + apiKeyId, updateJSON, secret);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var apiKeyDTO = gson.fromJson(response.getBody(), ApiKeyDTO.class);
    assertEquals("UpdatedByBruker", apiKeyDTO.getName());
    assertEquals(brukerDTO.getId(), apiKeyDTO.getBruker().getId());

    // The owning Bruker can update the key when authenticated with a JWT
    updateJSON.put("name", "UpdatedByBrukerJWT");
    response = patch("/apiKey/" + apiKeyId, updateJSON, brukerToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    apiKeyDTO = gson.fromJson(response.getBody(), ApiKeyDTO.class);
    assertEquals("UpdatedByBrukerJWT", apiKeyDTO.getName());

    // The owning Bruker can not move the key to an Enhet
    var moveJSON = new JSONObject();
    moveJSON.put("enhet", journalenhetId);
    response = patch("/apiKey/" + apiKeyId, moveJSON, secret);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // An admin can update the key
    updateJSON.put("name", "UpdatedByAdmin");
    response = patchAdmin("/apiKey/" + apiKeyId, updateJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    apiKeyDTO = gson.fromJson(response.getBody(), ApiKeyDTO.class);
    assertEquals("UpdatedByAdmin", apiKeyDTO.getName());
  }

  @Test
  void testDeleteBrukerScopedApiKey() throws Exception {
    var secret = newSecret();
    var apiKeyId = createBrukerApiKey(secret).getId();

    // Anonymous users can not delete the key
    var response = deleteAnon("/apiKey/" + apiKeyId);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // An unrelated Enhet can not delete the key
    response = delete("/apiKey/" + apiKeyId, journalenhetKey);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // The owning Bruker can revoke their own key
    response = delete("/apiKey/" + apiKeyId, secret);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = getAdmin("/apiKey/" + apiKeyId);
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    // An admin can delete a Bruker-bound key
    var adminDeletedId = createBrukerApiKey(newSecret()).getId();
    response = deleteAdmin("/apiKey/" + adminDeletedId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = getAdmin("/apiKey/" + adminDeletedId);
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  /**
   * Deleting a Bruker cascades to its ApiKeys, which used to fail with a NullPointerException in
   * authorizeDelete().
   */
  @Test
  void testDeleteBrukerWithApiKey() throws Exception {
    createBrukerApiKey(newSecret());
    // The @AfterEach cleanup deletes the Bruker, and with it the ApiKey. The row count check in
    // EinnsynTestBase verifies that the ApiKey is actually gone.
  }
}
