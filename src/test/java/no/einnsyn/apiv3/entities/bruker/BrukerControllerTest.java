package no.einnsyn.apiv3.entities.bruker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.reflect.TypeToken;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import no.einnsyn.apiv3.authentication.bruker.models.TokenResponse;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.bruker.models.BrukerDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {"application.userSecretExpirationTime=1"})
class BrukerControllerTest extends EinnsynControllerTestBase {

  @MockBean JavaMailSender javaMailSender;

  private final CountDownLatch waiter = new CountDownLatch(1);

  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  /** Test that a user can be created, updated, and deleted */
  @Test
  void testUserLifeCycle() throws Exception {
    MimeMessage mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    var bruker = getBrukerJSON();
    var password = bruker.get("password");
    var brukerResponse = post("/bruker", bruker);
    assertEquals(HttpStatus.CREATED, brukerResponse.getStatusCode());
    var insertedBruker = gson.fromJson(brukerResponse.getBody(), BrukerDTO.class);
    var insertedBrukerObj = brukerService.findById(insertedBruker.getId());
    assertEquals(bruker.get("email"), insertedBruker.getEmail());
    assertFalse(insertedBruker.getActive());

    // Verify that one email was sent
    waiter.await(50, TimeUnit.MILLISECONDS);
    verify(javaMailSender, times(1)).createMimeMessage();
    verify(javaMailSender, times(1)).send(mimeMessage);

    // Check that we can update the bruker
    bruker.put("email", "updatedEpost@example.com");
    brukerResponse = put("/bruker/" + insertedBruker.getId(), bruker);
    assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());
    var updatedBruker = gson.fromJson(brukerResponse.getBody(), BrukerDTO.class);
    assertEquals(bruker.get("email"), updatedBruker.getEmail());

    // Check that we cannot update the bruker with an invalid email address
    bruker.put("email", "invalidEmail");
    brukerResponse = put("/bruker/" + insertedBruker.getId(), bruker);
    assertEquals(HttpStatus.BAD_REQUEST, brukerResponse.getStatusCode());

    // Check that we can activate the bruker
    brukerResponse =
        put(
            "/bruker/" + insertedBruker.getId() + "/activate/" + insertedBrukerObj.getSecret(),
            new JSONObject());
    assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());
    updatedBruker = gson.fromJson(brukerResponse.getBody(), BrukerDTO.class);
    assertEquals(true, updatedBruker.getActive());

    // Authenticate user, to be able to get /bruker/id
    var loginRequest = new JSONObject();
    loginRequest.put("username", "updatedEpost@example.com");
    loginRequest.put("password", password);
    var loginResponse = post("/auth/token", loginRequest);
    var tokenResponse = gson.fromJson(loginResponse.getBody(), TokenResponse.class);
    var accessToken = tokenResponse.getToken();

    // Check that we can get the new bruker from the API
    brukerResponse = getWithJWT("/bruker/" + insertedBruker.getId(), accessToken);
    assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());

    // Check that we can delete the bruker
    brukerResponse = delete("/bruker/" + insertedBruker.getId());
    assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());
    assertEquals("updatedEpost@example.com", updatedBruker.getEmail());

    // Check that the bruker is deleted
    brukerResponse = get("/bruker/" + insertedBruker.getId());
    assertEquals(HttpStatus.NOT_FOUND, brukerResponse.getStatusCode());
  }

  /** Test that we cannot an user with passwords that doesn't meet requirements */
  @Test
  void testInsertWithPassword() throws Exception {
    MimeMessage mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    var bruker = getBrukerJSON();

    // Check that we cannot insert an invalid password (too short)
    bruker.put("password", "abcABC1");
    var brukerResponse = post("/bruker", bruker);
    assertEquals(HttpStatus.BAD_REQUEST, brukerResponse.getStatusCode());

    // Check that we cannot insert a password with no uppercase letters
    bruker.put("password", "abcabc123");
    brukerResponse = post("/bruker", bruker);
    assertEquals(HttpStatus.BAD_REQUEST, brukerResponse.getStatusCode());

    // Check that we cannot insert a password with no lowercase letters
    bruker.put("password", "ABCABC123");
    brukerResponse = post("/bruker", bruker);
    assertEquals(HttpStatus.BAD_REQUEST, brukerResponse.getStatusCode());

    // Check that we cannot insert a password with no numbers / special characters
    bruker.put("password", "abcABCabc");
    brukerResponse = post("/bruker", bruker);
    assertEquals(HttpStatus.BAD_REQUEST, brukerResponse.getStatusCode());

    // Check that we cannot insert without a password
    bruker.remove("password");
    brukerResponse = post("/bruker", bruker);
    assertEquals(HttpStatus.BAD_REQUEST, brukerResponse.getStatusCode());

    // Check that we can insert with a valid password
    bruker.put("password", "abcABC,,,");
    brukerResponse = post("/bruker", bruker);
    assertEquals(HttpStatus.CREATED, brukerResponse.getStatusCode());

    // Remove user
    var insertedBruker = gson.fromJson(brukerResponse.getBody(), BrukerDTO.class);
    brukerResponse = delete("/bruker/" + insertedBruker.getId());
    assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());

    // Check that we can insert with another valid password
    bruker.put("password", "abcABC123");
    brukerResponse = post("/bruker", bruker);
    assertEquals(HttpStatus.CREATED, brukerResponse.getStatusCode());

    // Remove user
    insertedBruker = gson.fromJson(brukerResponse.getBody(), BrukerDTO.class);
    brukerResponse = delete("/bruker/" + insertedBruker.getId());
    assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());
  }

  /** Test user activation expiry time */
  @Test
  void testUserActivationExpiryTime() throws Exception {
    MimeMessage mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    var bruker = getBrukerJSON();

    var brukerResponse = post("/bruker", bruker);
    assertEquals(HttpStatus.CREATED, brukerResponse.getStatusCode());
    var insertedBruker = gson.fromJson(brukerResponse.getBody(), BrukerDTO.class);
    assertEquals(bruker.get("email"), insertedBruker.getEmail());
    var brukerOBJ = brukerService.findById(insertedBruker.getId());

    // Check that the secret is invalid after 1 second
    waiter.await(1100, TimeUnit.MILLISECONDS);
    brukerResponse =
        put(
            "/bruker/" + insertedBruker.getId() + "/activate/" + brukerOBJ.getSecret(),
            new JSONObject());
    assertEquals(HttpStatus.UNAUTHORIZED, brukerResponse.getStatusCode());

    // Remove user
    brukerResponse = delete("/bruker/" + insertedBruker.getId());
    assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());
  }

  /** Test password reset */
  @Test
  void testPasswordReset() throws Exception {
    MimeMessage mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    var bruker = getBrukerJSON();

    var brukerResponse = post("/bruker", bruker);
    assertEquals(HttpStatus.CREATED, brukerResponse.getStatusCode());
    var insertedBruker = gson.fromJson(brukerResponse.getBody(), BrukerDTO.class);
    assertEquals(bruker.get("email"), insertedBruker.getEmail());
    // Check that one email was sent
    waiter.await(50, TimeUnit.MILLISECONDS);
    verify(javaMailSender, times(1)).createMimeMessage();
    verify(javaMailSender, times(1)).send(mimeMessage);

    // Check that we can request a password reset
    brukerResponse =
        put("/bruker/" + insertedBruker.getId() + "/requestPasswordReset", new JSONObject());
    insertedBruker = gson.fromJson(brukerResponse.getBody(), BrukerDTO.class);
    assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());

    // Check that one more email was sent
    waiter.await(50, TimeUnit.MILLISECONDS);
    verify(javaMailSender, times(2)).createMimeMessage();
    verify(javaMailSender, times(2)).send(mimeMessage);

    // Check that we can reset the password with the secret
    var brukerOBJ = brukerService.findById(insertedBruker.getId());
    var passwordRequestBody = new JSONObject();
    passwordRequestBody.put("newPassword", "newPassw0rd");
    brukerResponse =
        put(
            "/bruker/" + insertedBruker.getId() + "/updatePassword/" + brukerOBJ.getSecret(),
            passwordRequestBody);
    assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());
    var insertedBrukerObj = brukerService.findById(insertedBruker.getId());
    assertEquals(true, passwordEncoder.matches("newPassw0rd", insertedBrukerObj.getPassword()));

    // Check that we can login with the new password
    // bruker.put("password", "abcABC123");
    // brukerResponse = post("/bruker/login", bruker);
    // assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());

    // Check that we can update the password by giving the old password
    passwordRequestBody = new JSONObject();
    passwordRequestBody.put("oldPassword", "newPassw0rd");
    passwordRequestBody.put("newPassword", "newPassw0rd2");
    brukerResponse =
        put("/bruker/" + insertedBruker.getId() + "/updatePassword", passwordRequestBody);
    assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());

    // Check that we can login with the new password
    // bruker.put("password", "abcABC123");
    // brukerResponse = post("/bruker/login", bruker);
    // assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());

    // Remove user
    brukerResponse = delete("/bruker/" + insertedBruker.getId());
    assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());
  }

  // Add and list innsynskrav for bruker
  @Test
  void testInnsynskravByBruker() throws Exception {
    // Create the bruker
    var mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    var response = post("/bruker", getBrukerJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    var brukerObj = brukerService.findById(brukerDTO.getId());
    assertNotNull(brukerObj);

    // Activate the bruker
    response = put("/bruker/" + brukerDTO.getId() + "/activate/" + brukerObj.getSecret());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var arkivJSON = getArkivJSON();
    var arkivResponse = post("/arkiv", arkivJSON);
    var arkivDTO = gson.fromJson(arkivResponse.getBody(), ArkivDTO.class);

    // Insert saksmappe and journalposts for innsynskrav
    response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var smDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    response = post("/saksmappe/" + smDTO.getId() + "/journalpost", getJournalpostJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var jp1 = gson.fromJson(response.getBody(), JournalpostDTO.class);
    response = post("/saksmappe/" + smDTO.getId() + "/journalpost", getJournalpostJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var jp2 = gson.fromJson(response.getBody(), JournalpostDTO.class);
    response = post("/saksmappe/" + smDTO.getId() + "/journalpost", getJournalpostJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var jp3 = gson.fromJson(response.getBody(), JournalpostDTO.class);
    response = post("/saksmappe/" + smDTO.getId() + "/journalpost", getJournalpostJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var jp4 = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Create innsynskrav
    var innsynskravJSON = getInnsynskravJSON();
    var idJSON = getInnsynskravDelJSON();
    idJSON.put("journalpost", jp1.getId());
    innsynskravJSON.put("innsynskravDel", new JSONArray().put(idJSON));
    response = post("/bruker/" + brukerDTO.getId() + "/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var i1DTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);

    idJSON.put("journalpost", jp2.getId());
    innsynskravJSON.put("innsynskravDel", new JSONArray().put(idJSON));
    response = post("/bruker/" + brukerDTO.getId() + "/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var i2DTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);

    idJSON.put("journalpost", jp3.getId());
    innsynskravJSON.put("innsynskravDel", new JSONArray().put(idJSON));
    response = post("/bruker/" + brukerDTO.getId() + "/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var i3DTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);

    idJSON.put("journalpost", jp4.getId());
    innsynskravJSON.put("innsynskravDel", new JSONArray().put(idJSON));
    response = post("/bruker/" + brukerDTO.getId() + "/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var i4DTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);

    // List innsynskrav for bruker (DESC)
    var resultListType = new TypeToken<ResultList<InnsynskravDTO>>() {}.getType();
    response = get("/bruker/" + brukerDTO.getId() + "/innsynskrav");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    ResultList<InnsynskravDTO> listDTO = gson.fromJson(response.getBody(), resultListType);
    var items = listDTO.getItems();
    assertEquals(4, items.size());
    assertEquals(i4DTO.getId(), items.get(0).getId());
    assertEquals(i3DTO.getId(), items.get(1).getId());
    assertEquals(i2DTO.getId(), items.get(2).getId());
    assertEquals(i1DTO.getId(), items.get(3).getId());

    // ASC
    response = get("/bruker/" + brukerDTO.getId() + "/innsynskrav?sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    listDTO = gson.fromJson(response.getBody(), resultListType);
    items = listDTO.getItems();
    assertEquals(4, items.size());
    assertEquals(i1DTO.getId(), items.get(0).getId());
    assertEquals(i2DTO.getId(), items.get(1).getId());
    assertEquals(i3DTO.getId(), items.get(2).getId());
    assertEquals(i4DTO.getId(), items.get(3).getId());

    // StartingAfter
    response = get("/bruker/" + brukerDTO.getId() + "/innsynskrav?startingAfter=" + i2DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    listDTO = gson.fromJson(response.getBody(), resultListType);
    items = listDTO.getItems();
    assertEquals(1, items.size());
    assertEquals(i1DTO.getId(), items.get(0).getId());

    // EndingBefore
    response = get("/bruker/" + brukerDTO.getId() + "/innsynskrav?endingBefore=" + i3DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    listDTO = gson.fromJson(response.getBody(), resultListType);
    items = listDTO.getItems();
    assertEquals(1, items.size());
    assertEquals(i4DTO.getId(), items.get(0).getId());

    // Delete bruker
    response = delete("/bruker/" + brukerDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Make sure the innsynskravs are deleted
    assertEquals(HttpStatus.NOT_FOUND, get("/innsynskrav/" + i1DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/innsynskrav/" + i2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/innsynskrav/" + i3DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/innsynskrav/" + i4DTO.getId()).getStatusCode());

    // Make sure the journalposts still exist
    assertEquals(HttpStatus.OK, get("/journalpost/" + jp1.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, get("/journalpost/" + jp2.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, get("/journalpost/" + jp3.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, get("/journalpost/" + jp4.getId()).getStatusCode());

    // Delete the saksmappe
    response = delete("/saksmappe/" + smDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Make sure the journalposts and saksmappe are deleted
    assertEquals(HttpStatus.NOT_FOUND, get("/journalpost/" + jp1.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/journalpost/" + jp2.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/journalpost/" + jp3.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/journalpost/" + jp4.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/saksmappe/" + smDTO.getId()).getStatusCode());

    // Delete Arkiv
    delete("/arkiv/" + arkivDTO.getId());
  }
}
