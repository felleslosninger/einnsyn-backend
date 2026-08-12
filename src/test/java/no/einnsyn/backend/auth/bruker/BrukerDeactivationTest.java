package no.einnsyn.backend.auth.bruker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * Tests that a deactivated Bruker cannot obtain new tokens. This uses the default (long) token
 * expiration times, so that a rejected refresh cannot be explained by an expired refresh token.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BrukerDeactivationTest extends EinnsynControllerTestBase {

  @Test
  void testDeactivatedBrukerCannotRefreshToken() throws Exception {
    // Add and activate user
    var brukerJSON = getBrukerJSON();
    var response = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    var bruker = brukerService.find(brukerDTO.getId());
    response = patch("/bruker/" + bruker.getId() + "/activate/" + bruker.getSecret(), null);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(gson.fromJson(response.getBody(), BrukerDTO.class).getActive());

    // Log in
    var loginRequest = getLoginJSON(brukerJSON);
    response = post("/auth/token", loginRequest);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var refreshToken = gson.fromJson(response.getBody(), TokenResponse.class).getRefreshToken();

    // The refresh token works while the account is active
    var refreshRequest = new JSONObject();
    refreshRequest.put("refreshToken", refreshToken);
    response = post("/auth/token", refreshRequest);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    refreshToken = gson.fromJson(response.getBody(), TokenResponse.class).getRefreshToken();

    // Deactivate the account
    var brukerObj = brukerService.find(brukerDTO.getId());
    brukerObj.setActive(false);
    brukerRepository.saveAndFlush(brukerObj);
    assertFalse(brukerService.find(brukerDTO.getId()).isActive());

    // The still-unexpired refresh token must no longer be accepted
    refreshRequest = new JSONObject();
    refreshRequest.put("refreshToken", refreshToken);
    response = post("/auth/token", refreshRequest);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

    // Neither must username / password
    response = post("/auth/token", loginRequest);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

    // Reactivating the account makes the refresh token usable again, which proves the rejection
    // above was caused by the deactivation and not by an expired token.
    brukerObj = brukerService.find(brukerDTO.getId());
    brukerObj.setActive(true);
    brukerRepository.saveAndFlush(brukerObj);
    response = post("/auth/token", refreshRequest);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Clean up
    response = deleteAdmin("/bruker/" + brukerDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}
