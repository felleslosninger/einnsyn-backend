package no.einnsyn.backend.entities.bruker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.gson.reflect.TypeToken;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.authentication.bruker.models.TokenResponse;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import no.einnsyn.backend.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.awaitility.Awaitility;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {"application.userSecretExpirationTime=1"})
@ActiveProfiles("test")
class BrukerControllerTest extends EinnsynControllerTestBase {

  private final CountDownLatch waiter = new CountDownLatch(1);

  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  /** Test that a user can be created, updated, and deleted */
  @Test
  void testUserLifeCycle() throws Exception {

    var bruker = getBrukerJSON();
    var password = bruker.get("password");
    var brukerResponse = post("/bruker", bruker);
    assertEquals(HttpStatus.CREATED, brukerResponse.getStatusCode());
    var insertedBruker = gson.fromJson(brukerResponse.getBody(), BrukerDTO.class);
    var insertedBrukerObj = brukerService.findById(insertedBruker.getId());
    assertEquals(bruker.get("email"), insertedBruker.getEmail());
    assertFalse(insertedBruker.getActive());

    // Verify that one email was sent
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(1)).send(any(MimeMessage.class)));

    // Check that we can update the bruker. Email/username should be converted to
    // lowercase regardless of what we send in.
    bruker.remove("password");
    bruker.put("email", "updatedEpost@example.com");
    brukerResponse = patchAdmin("/bruker/" + insertedBruker.getId(), bruker);
    assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());
    var updatedBruker = gson.fromJson(brukerResponse.getBody(), BrukerDTO.class);
    assertEquals(bruker.getString("email").toLowerCase(), updatedBruker.getEmail());

    // Check that we cannot update the bruker with an invalid email address
    bruker.put("email", "invalidEmail");
    brukerResponse = patch("/bruker/" + insertedBruker.getId(), bruker);
    assertEquals(HttpStatus.BAD_REQUEST, brukerResponse.getStatusCode());

    // Check that we can activate the bruker
    brukerResponse =
        patch(
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
    brukerResponse = get("/bruker/" + insertedBruker.getId(), accessToken);
    assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());

    // Check that we can delete the bruker
    brukerResponse = deleteAdmin("/bruker/" + insertedBruker.getId());
    assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());
    assertEquals("updatedepost@example.com", updatedBruker.getEmail());

    // Check that the bruker is deleted
    brukerResponse = get("/bruker/" + insertedBruker.getId());
    assertEquals(HttpStatus.NOT_FOUND, brukerResponse.getStatusCode());
  }

  /** Test that we cannot an user with passwords that doesn't meet requirements */
  @Test
  void testInsertWithPassword() throws Exception {
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
    brukerResponse = deleteAdmin("/bruker/" + insertedBruker.getId());
    assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());

    // Check that we can insert with another valid password
    bruker.put("password", "abcABC123");
    brukerResponse = post("/bruker", bruker);
    assertEquals(HttpStatus.CREATED, brukerResponse.getStatusCode());

    // Remove user
    insertedBruker = gson.fromJson(brukerResponse.getBody(), BrukerDTO.class);
    brukerResponse = deleteAdmin("/bruker/" + insertedBruker.getId());
    assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());
  }

  /** Test user activation expiry time */
  @Test
  void testUserActivationExpiryTime() throws Exception {
    var bruker = getBrukerJSON();

    var brukerResponse = post("/bruker", bruker);
    assertEquals(HttpStatus.CREATED, brukerResponse.getStatusCode());
    var insertedBruker = gson.fromJson(brukerResponse.getBody(), BrukerDTO.class);
    assertEquals(bruker.get("email"), insertedBruker.getEmail());
    var brukerOBJ = brukerService.findById(insertedBruker.getId());

    // Check that the secret is invalid after 1 second
    waiter.await(1100, TimeUnit.MILLISECONDS);
    brukerResponse =
        patch(
            "/bruker/" + insertedBruker.getId() + "/activate/" + brukerOBJ.getSecret(),
            new JSONObject());
    assertEquals(HttpStatus.FORBIDDEN, brukerResponse.getStatusCode());

    // Remove user
    brukerResponse = deleteAdmin("/bruker/" + insertedBruker.getId());
    assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());
  }

  /** Test password reset */
  @Test
  void testPasswordReset() throws Exception {
    var bruker = getBrukerJSON();

    var brukerResponse = post("/bruker", bruker);
    assertEquals(HttpStatus.CREATED, brukerResponse.getStatusCode());
    var insertedBruker = gson.fromJson(brukerResponse.getBody(), BrukerDTO.class);
    assertEquals(bruker.get("email"), insertedBruker.getEmail());
    // Check that one email was sent
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(1)).send(any(MimeMessage.class)));

    // Check that we can request a password reset
    brukerResponse =
        patch("/bruker/" + insertedBruker.getId() + "/requestPasswordReset", new JSONObject());
    insertedBruker = gson.fromJson(brukerResponse.getBody(), BrukerDTO.class);
    assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());

    // Check that one more email was sent
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(2)).send(any(MimeMessage.class)));

    // Check that we can reset the password with the secret
    var brukerOBJ = brukerService.findById(insertedBruker.getId());
    var passwordRequestBody = new JSONObject();
    passwordRequestBody.put("newPassword", "newPassw0rd");
    brukerResponse =
        patch(
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
        patch("/bruker/" + insertedBruker.getId() + "/updatePassword", passwordRequestBody);
    assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());

    // Check that we can login with the new password
    // bruker.put("password", "abcABC123");
    // brukerResponse = post("/bruker/login", bruker);
    // assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());

    // Remove user
    brukerResponse = deleteAdmin("/bruker/" + insertedBruker.getId());
    assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());
  }

  // Add and list InnsynskravBestilling for bruker
  @Test
  void testInnsynskravBestillingByBruker() throws Exception {
    // Create the bruker
    var brukerJSON = getBrukerJSON();
    var response = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    var brukerObj = brukerService.findById(brukerDTO.getId());
    assertNotNull(brukerObj);

    // Activate the bruker
    response = patch("/bruker/" + brukerDTO.getId() + "/activate/" + brukerObj.getSecret());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Get Bruker JWT
    var loginRequest = new JSONObject();
    loginRequest.put("username", brukerDTO.getEmail());
    loginRequest.put("password", brukerJSON.getString("password"));
    response = post("/auth/token", loginRequest);
    var tokenResponse = gson.fromJson(response.getBody(), TokenResponse.class);
    var accessToken = tokenResponse.getToken();

    var arkivJSON = getArkivJSON();
    var arkivResponse = post("/arkiv", arkivJSON);
    var arkivDTO = gson.fromJson(arkivResponse.getBody(), ArkivDTO.class);

    var arkivdelResponse = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdelDTO = gson.fromJson(arkivdelResponse.getBody(), ArkivdelDTO.class);

    // Insert saksmappe and journalposts for innsynskravBestilling
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
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

    // Create InnsynskravBestilling
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var idJSON = getInnsynskravJSON();
    idJSON.put("journalpost", jp1.getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(idJSON));
    response = post("/innsynskravBestilling", innsynskravBestillingJSON, accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var i1DTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);

    idJSON.put("journalpost", jp2.getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(idJSON));
    response = post("/innsynskravBestilling", innsynskravBestillingJSON, accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var i2DTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);

    idJSON.put("journalpost", jp3.getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(idJSON));
    response = post("/innsynskravBestilling", innsynskravBestillingJSON, accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var i3DTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);

    idJSON.put("journalpost", jp4.getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(idJSON));
    response = post("/innsynskravBestilling", innsynskravBestillingJSON, accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var i4DTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);

    // List InnsynskravBestilling for bruker (DESC)
    var resultListType = new TypeToken<PaginatedList<InnsynskravBestillingDTO>>() {}.getType();
    response = get("/bruker/" + brukerDTO.getId() + "/innsynskravBestilling", accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<InnsynskravBestillingDTO> listDTO =
        gson.fromJson(response.getBody(), resultListType);
    var items = listDTO.getItems();
    assertEquals(4, items.size());
    assertEquals(i4DTO.getId(), items.get(0).getId());
    assertEquals(i3DTO.getId(), items.get(1).getId());
    assertEquals(i2DTO.getId(), items.get(2).getId());
    assertEquals(i1DTO.getId(), items.get(3).getId());

    // ASC
    response =
        get("/bruker/" + brukerDTO.getId() + "/innsynskravBestilling?sortOrder=asc", accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    listDTO = gson.fromJson(response.getBody(), resultListType);
    items = listDTO.getItems();
    assertEquals(4, items.size());
    assertEquals(i1DTO.getId(), items.get(0).getId());
    assertEquals(i2DTO.getId(), items.get(1).getId());
    assertEquals(i3DTO.getId(), items.get(2).getId());
    assertEquals(i4DTO.getId(), items.get(3).getId());

    // StartingAfter
    response =
        get(
            "/bruker/"
                + brukerDTO.getId()
                + "/innsynskravBestilling?startingAfter="
                + i2DTO.getId(),
            accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    listDTO = gson.fromJson(response.getBody(), resultListType);
    items = listDTO.getItems();
    assertEquals(1, items.size());
    assertEquals(i1DTO.getId(), items.get(0).getId());

    // EndingBefore
    response =
        get(
            "/bruker/" + brukerDTO.getId() + "/innsynskravBestilling?endingBefore=" + i3DTO.getId(),
            accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    listDTO = gson.fromJson(response.getBody(), resultListType);
    items = listDTO.getItems();
    assertEquals(1, items.size());
    assertEquals(i4DTO.getId(), items.get(0).getId());

    // Delete bruker
    response = delete("/bruker/" + brukerDTO.getId(), accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Make sure the InnsynskravBestillings are deleted
    assertEquals(
        HttpStatus.NOT_FOUND, get("/innsynskravBestilling/" + i1DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/innsynskravBestilling/" + i2DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/innsynskravBestilling/" + i3DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/innsynskravBestilling/" + i4DTO.getId()).getStatusCode());

    // Delete the Innsynskrav
    for (var innsynskrav : i1DTO.getInnsynskrav()) {
      assertEquals(
          HttpStatus.OK, deleteAdmin("/innsynskrav/" + innsynskrav.getId()).getStatusCode());
    }
    for (var innsynskrav : i2DTO.getInnsynskrav()) {
      assertEquals(
          HttpStatus.OK, deleteAdmin("/innsynskrav/" + innsynskrav.getId()).getStatusCode());
    }
    for (var innsynskrav : i3DTO.getInnsynskrav()) {
      assertEquals(
          HttpStatus.OK, deleteAdmin("/innsynskrav/" + innsynskrav.getId()).getStatusCode());
    }
    for (var innsynskrav : i4DTO.getInnsynskrav()) {
      assertEquals(
          HttpStatus.OK, deleteAdmin("/innsynskrav/" + innsynskrav.getId()).getStatusCode());
    }

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

  // Test /bruker/{brukerId}/innsynskrav
  @Test
  void testInnsynskravByBruker() throws Exception {

    // Add Bruker1
    var brukerResponse = post("/bruker", getBrukerJSON());
    var bruker1DTO = gson.fromJson(brukerResponse.getBody(), BrukerDTO.class);
    assertNotNull(bruker1DTO);
    assertNotNull(bruker1DTO.getId());
    var bruker1Id = bruker1DTO.getId();
    var bruker1 = brukerService.findById(bruker1Id);

    // Add Bruker2
    brukerResponse = post("/bruker", getBrukerJSON());
    var bruker2DTO = gson.fromJson(brukerResponse.getBody(), BrukerDTO.class);
    assertNotNull(bruker2DTO);
    assertNotNull(bruker2DTO.getId());
    var bruker2Id = bruker2DTO.getId();
    var bruker2 = brukerService.findById(bruker2Id);

    // Activate Brukers
    brukerResponse = patch("/bruker/" + bruker1Id + "/activate/" + bruker1.getSecret());
    assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());
    brukerResponse = patch("/bruker/" + bruker2Id + "/activate/" + bruker2.getSecret());
    assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());

    // Get JWT tokens for Brukers
    var loginRequest = new JSONObject();
    loginRequest.put("username", bruker1DTO.getEmail());
    loginRequest.put("password", getBrukerJSON().getString("password"));
    var tokenResponse = post("/auth/token", loginRequest);
    var tokenResponseDTO = gson.fromJson(tokenResponse.getBody(), TokenResponse.class);
    var bruker1Token = tokenResponseDTO.getToken();
    assertNotNull(bruker1Token);
    loginRequest.put("username", bruker2DTO.getEmail());
    tokenResponse = post("/auth/token", loginRequest);
    tokenResponseDTO = gson.fromJson(tokenResponse.getBody(), TokenResponse.class);
    var bruker2Token = tokenResponseDTO.getToken();
    assertNotNull(bruker2Token);

    // Add Arkiv for Saksmappe
    var arkivResponse = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(arkivResponse.getBody(), ArkivDTO.class);
    assertNotNull(arkivDTO.getId());

    // Add Arkivdel for Saksmappe
    var arkivdelResponse = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdelDTO = gson.fromJson(arkivdelResponse.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdelDTO.getId());

    // Add Saksmappe with Journalposts
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put(
        "journalpost",
        new JSONArray(
            List.of(
                getJournalpostJSON(),
                getJournalpostJSON(),
                getJournalpostJSON(),
                getJournalpostJSON(),
                getJournalpostJSON(),
                getJournalpostJSON(),
                getJournalpostJSON(),
                getJournalpostJSON(),
                getJournalpostJSON(),
                getJournalpostJSON())));
    var saksmappeResponse = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    var saksmappeDTO = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappeDTO);
    assertNotNull(saksmappeDTO.getId());

    BiFunction<Integer, String, InnsynskravBestillingDTO> addInnsynskravBestilling =
        (Integer i, String token) -> {
          try {
            var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
            var innsynskravJSON = getInnsynskravJSON();
            innsynskravBestillingJSON.put("innsynskrav", new JSONArray(List.of(innsynskravJSON)));
            var journalpostList = getJournalpostList(saksmappeDTO.getId()).getItems();
            innsynskravJSON.put("journalpost", journalpostList.get(i).getId());
            var response = post("/innsynskravBestilling", innsynskravBestillingJSON, token);
            var innsynskravBestillingDTO =
                gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
            return innsynskravBestillingDTO;
          } catch (Exception e) {
            e.printStackTrace();
            return null;
          }
        };

    // Add 10 items as bruker1
    var innsynskravBestillingForBruker1 =
        IntStream.rangeClosed(0, 9)
            .mapToObj(i -> addInnsynskravBestilling.apply(i, bruker1Token))
            .toList();
    var innsynskravForBruker1 =
        innsynskravBestillingForBruker1.stream()
            .map(i -> i.getInnsynskrav().getFirst().getExpandedObject())
            .toList();

    // Add one to Bruker2, to make sure it's not seen in bruker1's list
    var innsynskravForBruker2 = addInnsynskravBestilling.apply(0, bruker2Token);

    var type = new TypeToken<PaginatedList<InnsynskravDTO>>() {}.getType();
    testGenericList(
        type, innsynskravForBruker1, "/bruker/" + bruker1Id + "/innsynskrav", bruker1Token);

    // Clean up
    assertEquals(HttpStatus.OK, delete("/bruker/" + bruker1Id, bruker1Token).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/bruker/" + bruker2Id, bruker2Token).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());

    for (var innsynskravBestillingDTO : innsynskravBestillingForBruker1) {
      for (var innsynskrav : innsynskravBestillingDTO.getInnsynskrav()) {
        assertEquals(
            HttpStatus.OK, deleteAdmin("/innsynskrav/" + innsynskrav.getId()).getStatusCode());
      }
    }

    for (var innsynskrav : innsynskravForBruker2.getInnsynskrav()) {
      assertEquals(
          HttpStatus.OK, deleteAdmin("/innsynskrav/" + innsynskrav.getId()).getStatusCode());
    }
  }
}
