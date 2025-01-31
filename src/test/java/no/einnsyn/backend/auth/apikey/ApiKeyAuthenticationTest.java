package no.einnsyn.backend.auth.apikey;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.entities.apikey.models.ApiKeyDTO;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
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

    var secretKey = apiKeyDTO.getSecretKey();
    response = get("/testauth", secretKey);
    var userDetails = JsonParser.parseString(response.getBody()).getAsJsonObject();
    assertEquals(apiKeyDTO.getId(), userDetails.get("id").getAsString());
    assertEquals(apiKeyDTO.getId(), userDetails.get("username").getAsString());
    assertEquals(enhetDTO.getId(), userDetails.get("enhetId").getAsString());

    response = get("/testauth", apiKeyDTO2.getSecretKey());
    var userDetails2 = JsonParser.parseString(response.getBody()).getAsJsonObject();
    assertEquals(apiKeyDTO2.getId(), userDetails2.get("id").getAsString());
    assertEquals(apiKeyDTO2.getId(), userDetails2.get("username").getAsString());
    assertEquals(enhetDTO2.getId(), userDetails2.get("enhetId").getAsString());

    // Wrong key (make sure prefix is secret_, otherwise it is treated as a JWT)
    response = get("/testauth", "secret_wrongKey");
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

    deleteAdmin("/enhet/" + enhetDTO.getId());
    deleteAdmin("/enhet/" + enhetDTO2.getId());
  }

  @Test
  void testActingAs() throws Exception {
    var headers = getActingAsHeaders(journalenhetKey, underenhetId);

    // Create arkiv & arkivdel
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    // Publish as underenhet
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON(), headers);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertEquals(underenhetId, saksmappeDTO.getAdministrativEnhetObjekt().getId());

    // Publish as journalenhet
    response =
        post(
            "/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON(), journalenhetKey);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO2 = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertEquals(journalenhetId, saksmappeDTO2.getAdministrativEnhetObjekt().getId());

    // Can not delete journalenhet's enhet's saksmappe when acting as underenhet
    response = delete("/saksmappe/" + saksmappeDTO2.getId(), headers);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Can delete underenhet's saksmappe when acting as underenhet
    response = delete("/saksmappe/" + saksmappeDTO.getId(), headers);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    response = delete("/arkiv/" + arkivDTO.getId());
  }

  private HttpHeaders getActingAsHeaders(String authKey, String actingAsId) {
    var headers = getAuthHeaders(authKey);
    headers.add("ACTING-AS", actingAsId);
    return headers;
  }
}
