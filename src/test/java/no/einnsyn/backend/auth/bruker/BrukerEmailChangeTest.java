package no.einnsyn.backend.auth.bruker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.authentication.bruker.models.TokenResponse;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

/**
 * Tests that tokens stay bound to the Bruker they were issued for when the e-mail address changes.
 * The refresh token is long-lived, so it must resolve the account by id, not by e-mail address.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BrukerEmailChangeTest extends EinnsynControllerTestBase {

  @Test
  void testRefreshTokenFollowsBrukerAfterEmailChange() throws Exception {
    // Add and activate user
    var brukerJSON = getBrukerJSON();
    var oldEmail = brukerJSON.getString("email");
    var response = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var brukerId = gson.fromJson(response.getBody(), BrukerDTO.class).getId();
    var bruker = brukerService.find(brukerId);
    response = patch("/bruker/" + brukerId + "/activate/" + bruker.getSecret(), null);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Log in
    response = post("/auth/token", getLoginJSON(brukerJSON));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var tokenResponse = gson.fromJson(response.getBody(), TokenResponse.class);
    var refreshToken = tokenResponse.getRefreshToken();

    // Change e-mail address
    var newEmail = "changed-" + oldEmail;
    var updateJSON = new JSONObject();
    updateJSON.put("email", newEmail);
    response = patch("/bruker/" + brukerId, updateJSON, tokenResponse.getToken());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(newEmail, gson.fromJson(response.getBody(), BrukerDTO.class).getEmail());

    // The refresh token issued before the change must still identify the user
    var refreshRequest = new JSONObject();
    refreshRequest.put("refreshToken", refreshToken);
    response = post("/auth/token", refreshRequest);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var refreshedToken = gson.fromJson(response.getBody(), TokenResponse.class).getToken();
    response = get("/bruker/" + brukerId, refreshedToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(newEmail, gson.fromJson(response.getBody(), BrukerDTO.class).getEmail());

    // Clean up
    assertEquals(HttpStatus.OK, deleteAdmin("/bruker/" + brukerId).getStatusCode());
  }
}
