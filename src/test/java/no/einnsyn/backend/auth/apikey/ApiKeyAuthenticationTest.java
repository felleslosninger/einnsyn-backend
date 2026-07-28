package no.einnsyn.backend.auth.apikey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.auth.AuthenticationController;
import no.einnsyn.backend.common.authinfo.models.AuthInfo;
import no.einnsyn.backend.common.exceptions.models.AuthenticationException;
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
    var userDetails =
        gson.fromJson(response.getBody(), AuthenticationController.TestAuthResponse.class);
    assertEquals(apiKeyDTO.getId(), userDetails.getId());
    assertEquals(enhetDTO.getOrgnummer(), userDetails.getUsername());
    assertEquals(enhetDTO.getId(), userDetails.getEnhetId());

    response = get("/testauth", apiKeyDTO2.getSecretKey());
    var userDetails2 =
        gson.fromJson(response.getBody(), AuthenticationController.TestAuthResponse.class);
    assertEquals(apiKeyDTO2.getId(), userDetails2.getId());
    assertEquals(enhetDTO2.getOrgnummer(), userDetails2.getUsername());
    assertEquals(enhetDTO2.getId(), userDetails2.getEnhetId());

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

  @Test
  void testHandteresAv() throws Exception {
    // Create Enhet
    var enhetJSON = getEnhetJSON();
    var response = post("/enhet/" + journalenhetId + "/underenhet", enhetJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var enhetDTO = gson.fromJson(response.getBody(), EnhetDTO.class);

    // Try to act on behalf of this enhet with the API key of journalenhet2.
    var headers = getActingAsHeaders(journalenhet2Key, enhetDTO.getId());
    response = post("/arkiv", getArkivJSON(), headers);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

    // Update the enhet, set handteresAv to journalenhet2
    enhetJSON.put("handteresAv", journalenhet2Id);
    response = patch("/enhet/" + enhetDTO.getId(), enhetJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Try to act on behalf of this enhet with the API key of journalenhet2.
    response = post("/arkiv", getArkivJSON(), headers);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // Verify the journalenhet
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertEquals(enhetDTO.getId(), arkivDTO.getJournalenhet().getId());

    // Clean up
    delete("/arkiv/" + arkivDTO.getId());
    delete("/enhet/" + enhetDTO.getId());
  }

  /**
   * A key belonging to a root Enhet is an admin key. When such a key acts as a subunit, the
   * resulting principal is scoped to that subunit and must no longer be an admin, so that ACTING-AS
   * works as a privilege reduction mechanism.
   */
  @Test
  void testActingAsDropsAdminPrivileges() throws Exception {
    // Sanity check: the plain admin key (root Enhet) is an admin
    var response = get("/testauth", adminKey);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var adminAuth =
        gson.fromJson(response.getBody(), AuthenticationController.TestAuthResponse.class);
    assertEquals(rootEnhetId, adminAuth.getEnhetId());
    assertTrue(adminAuth.isAdmin());

    // ... and can reach an admin-only endpoint
    response = getAdmin("/tilbakemelding");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // The same key acting as a subunit must be scoped to that subunit, and not be an admin
    var headers = getActingAsHeaders(adminKey, journalenhetId);
    response = get("/testauth", headers);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var actingAsAuth =
        gson.fromJson(response.getBody(), AuthenticationController.TestAuthResponse.class);
    assertEquals(journalenhetId, actingAsAuth.getEnhetId());
    assertEquals(journalenhetOrgnummer, actingAsAuth.getUsername());
    assertFalse(actingAsAuth.isAdmin());

    // ... and must therefore be rejected by the admin-only endpoint
    response = get("/tilbakemelding", headers);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  /** ACTING-AS with the same Enhet as the key itself should keep the key's own admin status. */
  @Test
  void testActingAsSelfKeepsAdminPrivileges() throws Exception {
    var headers = getActingAsHeaders(adminKey, rootEnhetId);
    var response = get("/testauth", headers);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var authResponse =
        gson.fromJson(response.getBody(), AuthenticationController.TestAuthResponse.class);
    assertEquals(rootEnhetId, authResponse.getEnhetId());
    assertTrue(authResponse.isAdmin());
  }

  /**
   * ACTING-AS may only reduce privileges, never grant them. An Enhet without a parent is an admin
   * Enhet, but a non-admin key that is allowed to act as such an Enhet (through handteresAv) must
   * not thereby become an admin.
   */
  @Test
  void testActingAsParentlessEnhetDoesNotGrantAdmin() throws Exception {
    // An admin delegates handling of the root Enhet to the non-admin journalenhet
    var rootEnhet = enhetRepository.findById(rootEnhetId).orElseThrow();
    rootEnhet.setHandteresAv(enhetRepository.findById(journalenhetId).orElseThrow());
    enhetRepository.saveAndFlush(rootEnhet);

    try {
      // The journalenhet key is now allowed to act as the root Enhet, but must stay non-admin
      var headers = getActingAsHeaders(journalenhetKey, rootEnhetId);
      var response = get("/testauth", headers);
      assertEquals(HttpStatus.OK, response.getStatusCode());
      var authResponse =
          gson.fromJson(response.getBody(), AuthenticationController.TestAuthResponse.class);
      assertEquals(rootEnhetId, authResponse.getEnhetId());
      assertFalse(authResponse.isAdmin());

      // ... and must therefore still be rejected by an admin-only endpoint
      response = get("/tilbakemelding", headers);
      assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    } finally {
      rootEnhet = enhetRepository.findById(rootEnhetId).orElseThrow();
      rootEnhet.setHandteresAv(null);
      enhetRepository.saveAndFlush(rootEnhet);
    }
  }

  /** A non-admin key acting as one of its subunits stays non-admin. */
  @Test
  void testActingAsFromNonAdminKeyIsNotAdmin() throws Exception {
    var headers = getActingAsHeaders(journalenhetKey, underenhetId);
    var response = get("/testauth", headers);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var authResponse =
        gson.fromJson(response.getBody(), AuthenticationController.TestAuthResponse.class);
    assertEquals(underenhetId, authResponse.getEnhetId());
    assertFalse(authResponse.isAdmin());
  }

  @Test
  void testInvalidActingAsReturnsUnauthorized() throws Exception {
    var headers = getAuthHeaders(journalenhetKey);
    headers.add("ACTING-AS", "999999999");

    var response = get("/testauth", headers);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void testAuthInfo() throws Exception {
    var response = get("/me", journalenhetKey);
    var authInfo = gson.fromJson(response.getBody(), AuthInfo.class);
    assertEquals("ApiKey", authInfo.getAuthType());
    assertEquals("Enhet", authInfo.getType());
    assertEquals(journalenhetId, authInfo.getId());
    assertEquals(journalenhetOrgnummer, authInfo.getOrgnummer());

    response = get("/me", journalenhet2Key);
    authInfo = gson.fromJson(response.getBody(), AuthInfo.class);
    assertEquals("ApiKey", authInfo.getAuthType());
    assertEquals("Enhet", authInfo.getType());
    assertEquals(journalenhet2Id, authInfo.getId());
    assertEquals(journalenhet2Orgnummer, authInfo.getOrgnummer());

    response = getAnon("/me");
    authInfo = gson.fromJson(response.getBody(), AuthInfo.class);
    var error = gson.fromJson(response.getBody(), AuthenticationException.ClientResponse.class);
    assertEquals("authenticationError", error.getType());
  }

  private HttpHeaders getActingAsHeaders(String authKey, String actingAsId) {
    var headers = getAuthHeaders(authKey);
    headers.add("ACTING-AS", actingAsId);
    return headers;
  }
}
