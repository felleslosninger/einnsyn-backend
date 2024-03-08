package no.einnsyn.apiv3.auth.apikey;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;
import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.apikey.models.ApiKeyDTO;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ApiKeyAuthenticationTest extends EinnsynControllerTestBase {

  @Test
  void testApiKeyAuthentication() throws Exception {
    // Add API key to Enhet
    var response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    var enhetDTO = gson.fromJson(response.getBody(), EnhetDTO.class);
    response = post("/enhet/" + enhetDTO.getId() + "/apiKey", getApiKeyJSON());
    var apiKeyDTO = gson.fromJson(response.getBody(), ApiKeyDTO.class);

    // Add API key to another enhet
    response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    var enhetDTO2 = gson.fromJson(response.getBody(), EnhetDTO.class);
    response = post("/enhet/" + enhetDTO2.getId() + "/apiKey", getApiKeyJSON());
    var apiKeyDTO2 = gson.fromJson(response.getBody(), ApiKeyDTO.class);

    var key = apiKeyDTO.getId();
    var secret = apiKeyDTO.getSecretKey();
    response = get("/testauth", key, secret);
    var userDetails = JsonParser.parseString(response.getBody()).getAsJsonObject();
    assertEquals(apiKeyDTO.getId(), userDetails.get("id").getAsString());
    assertEquals(apiKeyDTO.getId(), userDetails.get("username").getAsString());
    assertEquals(enhetDTO.getId(), userDetails.get("enhetId").getAsString());

    response = get("/testauth", apiKeyDTO2.getId(), apiKeyDTO2.getSecretKey());
    var userDetails2 = JsonParser.parseString(response.getBody()).getAsJsonObject();
    assertEquals(apiKeyDTO2.getId(), userDetails2.get("id").getAsString());
    assertEquals(apiKeyDTO2.getId(), userDetails2.get("username").getAsString());
    assertEquals(enhetDTO2.getId(), userDetails2.get("enhetId").getAsString());

    // Wrong key
    response = get("/testauth", "wrongKey", secret);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

    // Wrong secret
    response = get("/testauth", key, "wrongSecret");
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

    delete("/enhet/" + enhetDTO.getId(), adminKey, adminSecret);
    delete("/enhet/" + enhetDTO2.getId(), adminKey, adminSecret);
  }
}
