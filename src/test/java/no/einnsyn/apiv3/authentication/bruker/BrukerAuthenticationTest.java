package no.einnsyn.apiv3.authentication.bruker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import no.einnsyn.apiv3.authentication.bruker.models.TokenResponse;
import no.einnsyn.apiv3.entities.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.bruker.models.BrukerDTO;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
      "application.jwt.refreshTokenExpiration=4",
      "application.jwt.accessTokenExpiration=2"
    })
class BrukerAuthenticationTest extends EinnsynControllerTestBase {

  @MockBean JavaMailSender javaMailSender;

  @Value("#{${application.jwt.accessTokenExpiration}}")
  private long expiration;

  @Value("#{${application.jwt.refreshTokenExpiration}}")
  private long refreshExpiration;

  private final CountDownLatch waiter = new CountDownLatch(1);

  @Test
  void userLifeCycleTest() throws Exception {
    // Add user
    MimeMessage mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

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
        put(
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
    assertTrue(loginResponseJSON.getToken().length() > 0);
    assertTrue(loginResponseJSON.getRefreshToken().length() > 0);
    var accessToken = loginResponseJSON.getToken();
    var refreshToken = loginResponseJSON.getRefreshToken();

    // Verify that we can access a protected endpoint
    var protectedResponse = getWithJWT("/bruker/" + insertedBrukerObj.getId(), accessToken);
    assertEquals(HttpStatus.OK, protectedResponse.getStatusCode());

    // Verify that we cannot acces a protected endpoint after token expires
    // TODO: Enable this when authorization is implemented
    // waiter.await(expiration, TimeUnit.SECONDS);
    // protectedResponse = getWithJWT("/bruker/" + insertedBrukerObj.getId(), accessToken);
    // assertEquals(HttpStatus.FORBIDDEN, protectedResponse.getStatusCode());

    // Get a refreshed token
    var refreshRequest = new JSONObject();
    refreshRequest.put("refreshToken", refreshToken);
    var refreshResponse = post("/auth/token", refreshRequest);
    assertEquals(HttpStatus.OK, refreshResponse.getStatusCode());
    var refreshResponseJSON = gson.fromJson(refreshResponse.getBody(), TokenResponse.class);
    assertEquals(expiration, refreshResponseJSON.getExpiresIn());
    assertTrue(refreshResponseJSON.getToken().length() > 0);
    assertTrue(refreshResponseJSON.getRefreshToken().length() > 0);
    assertNotEquals(accessToken, refreshResponseJSON.getToken());
    accessToken = refreshResponseJSON.getToken();
    refreshToken = refreshResponseJSON.getRefreshToken();

    // Verify that we can access a protected endpoint
    protectedResponse = getWithJWT("/bruker/" + insertedBrukerObj.getId(), accessToken);
    assertEquals(HttpStatus.OK, protectedResponse.getStatusCode());

    // Verify that we cannot acces a protected endpoint after token expires
    waiter.await(expiration, TimeUnit.SECONDS);
    protectedResponse = getWithJWT("/bruker/" + insertedBrukerObj.getId(), accessToken);

    // Verify that we cannot refresh a token after it expires
    waiter.await(refreshExpiration - expiration, TimeUnit.SECONDS);
    refreshRequest = new JSONObject();
    refreshRequest.put("refreshToken", refreshToken);
    refreshResponse = post("/auth/token", refreshRequest);
    assertEquals(HttpStatus.UNAUTHORIZED, refreshResponse.getStatusCode());

    // Delete user
    var deleteResponse = delete("/bruker/" + insertedBrukerObj.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
  }

  /** Test required login fields */
  @Test
  void loginTest() throws Exception {
    // Add user
    MimeMessage mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    var bruker = getBrukerJSON();
    var brukerResponse = post("/bruker", bruker);
    assertEquals(HttpStatus.CREATED, brukerResponse.getStatusCode());
    var insertedBruker = gson.fromJson(brukerResponse.getBody(), BrukerDTO.class);
    var insertedBrukerObj = brukerService.findById(insertedBruker.getId());
    assertEquals(bruker.get("email"), insertedBruker.getEmail());
    assertFalse(insertedBruker.getActive());

    // Activate user
    var activationResponse =
        put(
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
    assertTrue(loginResponseJSON.getToken().length() > 0);
    assertTrue(loginResponseJSON.getRefreshToken().length() > 0);
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
    var deleteResponse = delete("/bruker/" + insertedBruker.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
  }
}
