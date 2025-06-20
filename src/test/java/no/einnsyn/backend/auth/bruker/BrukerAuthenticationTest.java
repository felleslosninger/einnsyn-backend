package no.einnsyn.backend.auth.bruker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.authentication.bruker.models.TokenResponse;
import no.einnsyn.backend.common.authinfo.models.AuthInfoResponse;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
      "application.jwt.refreshTokenExpiration=4",
      "application.jwt.accessTokenExpiration=2"
    })
@ActiveProfiles("test")
class BrukerAuthenticationTest extends EinnsynControllerTestBase {

  @Value("#{${application.jwt.accessTokenExpiration}}")
  private long expiration;

  @Value("#{${application.jwt.refreshTokenExpiration}}")
  private long refreshExpiration;

  private final CountDownLatch waiter = new CountDownLatch(1);

  @Test
  void userLifeCycleTest() throws Exception {
    // Add user
    var bruker = getBrukerJSON();
    var brukerResponse = post("/bruker", bruker);
    assertEquals(HttpStatus.CREATED, brukerResponse.getStatusCode());
    var insertedBruker = gson.fromJson(brukerResponse.getBody(), BrukerDTO.class);
    assertEquals(bruker.get("email"), insertedBruker.getEmail());
    assertFalse(insertedBruker.getActive());

    // Try to log in before activation
    var loginRequest = new JSONObject();
    loginRequest.put("username", bruker.get("email"));
    loginRequest.put("password", bruker.get("password"));
    var loginResponse = post("/auth/token", loginRequest);
    assertEquals(HttpStatus.UNAUTHORIZED, loginResponse.getStatusCode());

    // Activate user
    var insertedBrukerObj = brukerService.findById(insertedBruker.getEmail());
    var activationResponse =
        patch(
            "/bruker/" + insertedBrukerObj.getId() + "/activate/" + insertedBrukerObj.getSecret(),
            null);
    assertEquals(HttpStatus.OK, activationResponse.getStatusCode());
    var activationResponseJSON = gson.fromJson(activationResponse.getBody(), BrukerDTO.class);
    assertTrue(activationResponseJSON.getActive());

    // Log in
    loginResponse = post("/auth/token", loginRequest);
    assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
    var loginResponseJSON = gson.fromJson(loginResponse.getBody(), TokenResponse.class);
    assertEquals(expiration, loginResponseJSON.getExpiresIn());
    assertTrue(!loginResponseJSON.getToken().isEmpty());
    assertTrue(!loginResponseJSON.getRefreshToken().isEmpty());
    var accessToken = loginResponseJSON.getToken();
    var refreshToken = loginResponseJSON.getRefreshToken();

    // Verify that we can access a protected endpoint
    var protectedResponse = get("/bruker/" + insertedBrukerObj.getId(), accessToken);
    assertEquals(HttpStatus.OK, protectedResponse.getStatusCode());

    // Verify that we cannot acces a protected endpoint after token expires
    waiter.await(expiration, TimeUnit.SECONDS);
    protectedResponse = get("/bruker/" + insertedBrukerObj.getId(), accessToken);
    assertEquals(HttpStatus.UNAUTHORIZED, protectedResponse.getStatusCode());

    // Get a refreshed token
    var refreshRequest = new JSONObject();
    refreshRequest.put("refreshToken", refreshToken);
    var refreshResponse = post("/auth/token", refreshRequest);
    assertEquals(HttpStatus.OK, refreshResponse.getStatusCode());
    var refreshResponseJSON = gson.fromJson(refreshResponse.getBody(), TokenResponse.class);
    assertEquals(expiration, refreshResponseJSON.getExpiresIn());
    assertTrue(!refreshResponseJSON.getToken().isEmpty());
    assertTrue(!refreshResponseJSON.getRefreshToken().isEmpty());
    assertNotEquals(accessToken, refreshResponseJSON.getToken());
    accessToken = refreshResponseJSON.getToken();
    refreshToken = refreshResponseJSON.getRefreshToken();

    // Verify that we can access a protected endpoint
    protectedResponse = get("/bruker/" + insertedBrukerObj.getId(), accessToken);
    assertEquals(HttpStatus.OK, protectedResponse.getStatusCode());

    // Verify that we cannot acces a protected endpoint after token expires
    waiter.await(expiration, TimeUnit.SECONDS);
    protectedResponse = get("/bruker/" + insertedBrukerObj.getId(), accessToken);

    // Verify that we cannot refresh a token after it expires
    waiter.await(refreshExpiration - expiration, TimeUnit.SECONDS);
    refreshRequest = new JSONObject();
    refreshRequest.put("refreshToken", refreshToken);
    refreshResponse = post("/auth/token", refreshRequest);
    assertEquals(HttpStatus.UNAUTHORIZED, refreshResponse.getStatusCode());

    // Delete user
    var deleteResponse = deleteAdmin("/bruker/" + insertedBrukerObj.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
  }

  /** Test required login fields */
  @Test
  void loginTest() throws Exception {
    // Add user
    var bruker = getBrukerJSON();
    var brukerResponse = post("/bruker", bruker);
    assertEquals(HttpStatus.CREATED, brukerResponse.getStatusCode());
    var insertedBruker = gson.fromJson(brukerResponse.getBody(), BrukerDTO.class);
    var insertedBrukerObj = brukerService.findById(insertedBruker.getId());
    assertEquals(bruker.get("email"), insertedBruker.getEmail());
    assertFalse(insertedBruker.getActive());

    // Activate user
    var activationResponse =
        patch(
            "/bruker/" + insertedBrukerObj.getId() + "/activate/" + insertedBrukerObj.getSecret(),
            null);
    assertEquals(HttpStatus.OK, activationResponse.getStatusCode());
    var activationResponseJSON = gson.fromJson(activationResponse.getBody(), BrukerDTO.class);
    assertTrue(activationResponseJSON.getActive());

    // Try to log in without username, password or request token
    var loginRequest = new JSONObject();
    var loginResponse = post("/auth/token", loginRequest);
    assertEquals(HttpStatus.BAD_REQUEST, loginResponse.getStatusCode());

    // Try to log in without password
    loginRequest.put("username", bruker.get("email"));
    loginResponse = post("/auth/token", loginRequest);
    assertEquals(HttpStatus.BAD_REQUEST, loginResponse.getStatusCode());

    // Try to log in without username
    loginRequest.remove("username");
    loginRequest.put("password", bruker.get("password"));
    loginResponse = post("/auth/token", loginRequest);
    assertEquals(HttpStatus.BAD_REQUEST, loginResponse.getStatusCode());

    // Try to log in with username and password
    loginRequest.put("username", bruker.get("email"));
    loginResponse = post("/auth/token", loginRequest);
    assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
    var loginResponseJSON = gson.fromJson(loginResponse.getBody(), TokenResponse.class);
    assertEquals(expiration, loginResponseJSON.getExpiresIn());
    assertTrue(!loginResponseJSON.getToken().isEmpty());
    assertTrue(!loginResponseJSON.getRefreshToken().isEmpty());
    var refreshToken = loginResponseJSON.getRefreshToken();

    // Try to log in with username and refresh token
    loginRequest.remove("password");
    loginRequest.put("refreshToken", refreshToken);
    loginResponse = post("/auth/token", loginRequest);
    assertEquals(HttpStatus.OK, loginResponse.getStatusCode());

    // Try to log in with refresh token only
    loginRequest.remove("username");
    loginResponse = post("/auth/token", loginRequest);
    assertEquals(HttpStatus.OK, loginResponse.getStatusCode());

    // Delete user
    var deleteResponse = deleteAdmin("/bruker/" + insertedBruker.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
  }

  @Test
  void testAuthInfo() throws Exception {
    // Add user
    var brukerJSON = getBrukerJSON();
    var brukerResponse = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.CREATED, brukerResponse.getStatusCode());
    var brukerDTO = gson.fromJson(brukerResponse.getBody(), BrukerDTO.class);
    var bruker = brukerService.findById(brukerDTO.getId());
    assertEquals(brukerJSON.get("email"), brukerDTO.getEmail());
    assertFalse(brukerDTO.getActive());

    // Activate user
    var activationResponse =
        patch("/bruker/" + bruker.getId() + "/activate/" + bruker.getSecret(), null);
    assertEquals(HttpStatus.OK, activationResponse.getStatusCode());
    var activationResponseJSON = gson.fromJson(activationResponse.getBody(), BrukerDTO.class);
    assertTrue(activationResponseJSON.getActive());

    // Try to log in with username and password
    var loginRequest = new JSONObject();
    loginRequest.put("username", brukerJSON.get("email"));
    loginRequest.put("password", brukerJSON.get("password"));
    var loginResponse = post("/auth/token", loginRequest);
    assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
    var loginResponseJSON = gson.fromJson(loginResponse.getBody(), TokenResponse.class);
    assertEquals(expiration, loginResponseJSON.getExpiresIn());
    assertFalse(loginResponseJSON.getToken().isEmpty());
    assertTrue(!loginResponseJSON.getRefreshToken().isEmpty());
    var accessToken = loginResponseJSON.getToken();

    var response = get("/me", accessToken);
    var authInfo = gson.fromJson(response.getBody(), AuthInfoResponse.class);
    assertEquals("JWT", authInfo.getAuthType());
    assertEquals("Bruker", authInfo.getType());
    assertEquals(brukerDTO.getId(), authInfo.getId());
    assertEquals(brukerDTO.getEmail(), authInfo.getEmail());

    // Delete user
    var deleteResponse = deleteAdmin("/bruker/" + brukerDTO.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
  }
}
