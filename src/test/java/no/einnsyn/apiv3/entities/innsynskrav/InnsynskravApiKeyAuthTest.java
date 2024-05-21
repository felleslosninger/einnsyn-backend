package no.einnsyn.apiv3.entities.innsynskrav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.authentication.bruker.models.TokenResponse;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.bruker.models.BrukerDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.clients.ip.IPSender;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class InnsynskravApiKeyAuthTest extends EinnsynControllerTestBase {

  @MockBean IPSender ipSender;

  @MockBean JavaMailSender javaMailSender;

  @Test
  void testList() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    // Add bruker
    var brukerJSON = getBrukerJSON();
    var response = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    var bruker = brukerService.findById(brukerDTO.getId());

    // Activate bruker
    response = put("/bruker/" + brukerDTO.getId() + "/activate/" + bruker.getSecret());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Authenticate bruker
    var loginRequest = new JSONObject();
    loginRequest.put("username", brukerJSON.get("email"));
    loginRequest.put("password", brukerJSON.get("password"));
    response = post("/auth/token", loginRequest);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var tokenResponse = gson.fromJson(response.getBody(), TokenResponse.class);
    var brukerToken = tokenResponse.getToken();

    // Add Arkiv
    response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    // Add Saksmappe
    response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Add Journalpost
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", getJournalpostJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Add Innsynskrav
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("innsynskravDel", new JSONArray(List.of(journalpostDTO.getId())));
    response = post("/innsynskrav", innsynskravJSON, brukerToken);

    // Verify that anon cannot list innsynskrav
    response = getAnon("/bruker/" + brukerDTO.getId() + "/innsynskrav");
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Verify that other users cannot list innsynskrav
    response = get("/bruker/" + brukerDTO.getId() + "/innsynskrav");
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Verify that the user can list innsynskrav
    response = get("/bruker/" + brukerDTO.getId() + "/innsynskrav", brukerToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    assertEquals(
        HttpStatus.OK, delete("/bruker/" + brukerDTO.getId(), brukerToken).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }

  @Test
  void testGet() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    // Add bruker
    var brukerJSON = getBrukerJSON();
    var response = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    var bruker = brukerService.findById(brukerDTO.getId());

    // Activate bruker
    response = put("/bruker/" + brukerDTO.getId() + "/activate/" + bruker.getSecret());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Authenticate bruker
    var loginRequest = new JSONObject();
    loginRequest.put("username", brukerJSON.get("email"));
    loginRequest.put("password", brukerJSON.get("password"));
    response = post("/auth/token", loginRequest);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var tokenResponse = gson.fromJson(response.getBody(), TokenResponse.class);
    var brukerToken = tokenResponse.getToken();

    // Add Arkiv
    response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    // Add Saksmappe
    response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Add Journalpost
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", getJournalpostJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Add Innsynskrav
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put(
        "innsynskravDel",
        new JSONArray(List.of(getInnsynskravDelJSON().put("journalpost", journalpostDTO.getId()))));
    response = post("/innsynskrav", innsynskravJSON, brukerToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);

    // Verify that anon cannot get innsynskrav
    response = getAnon("/innsynskrav/" + innsynskravDTO.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Verify that other users cannot get innsynskrav
    response = get("/innsynskrav/" + innsynskravDTO.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Verify that the user can get innsynskrav
    response = get("/innsynskrav/" + innsynskravDTO.getId(), brukerToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    assertEquals(
        HttpStatus.OK, delete("/bruker/" + brukerDTO.getId(), brukerToken).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }

  @Test
  void testInsertUpdateDeleteInnsynskrav() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    // Add bruker
    var brukerJSON = getBrukerJSON();
    var response = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    var bruker = brukerService.findById(brukerDTO.getId());

    // Activate bruker
    response = put("/bruker/" + brukerDTO.getId() + "/activate/" + bruker.getSecret());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Authenticate bruker
    var loginRequest = new JSONObject();
    loginRequest.put("username", brukerJSON.get("email"));
    loginRequest.put("password", brukerJSON.get("password"));
    response = post("/auth/token", loginRequest);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var tokenResponse = gson.fromJson(response.getBody(), TokenResponse.class);
    var brukerToken = tokenResponse.getToken();

    // Add Arkiv
    response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    // Add Saksmappe
    response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Add Journalpost
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", getJournalpostJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Add Innsynskrav as Bruker
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put(
        "innsynskravDel",
        new JSONArray(List.of(getInnsynskravDelJSON().put("journalpost", journalpostDTO.getId()))));
    response = post("/innsynskrav", innsynskravJSON, brukerToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);

    // Add Innsynskrav as anonymous
    response = postAnon("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravDTOAnon = gson.fromJson(response.getBody(), InnsynskravDTO.class);

    // Update Innsynskrav as Bruker (fails, locked Innsynskravs are immutable)
    innsynskravJSON.put("innsynskravDel", new JSONArray());
    response = put("/innsynskrav/" + innsynskravDTO.getId(), innsynskravJSON, brukerToken);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Update Innsynskrav as anonymous
    response = put("/innsynskrav/" + innsynskravDTOAnon.getId(), innsynskravJSON);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Delete Innsynskrav as anonymous (fails)
    response = deleteAnon("/innsynskrav/" + innsynskravDTO.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    response = deleteAnon("/innsynskrav/" + innsynskravDTOAnon.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Delete Innsynskrav as Bruker
    response = delete("/innsynskrav/" + innsynskravDTO.getId(), brukerToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/innsynskrav/" + innsynskravDTOAnon.getId(), brukerToken);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Delete Innsynskrav as owner of Enhet
    response = delete("/innsynskrav/" + innsynskravDTOAnon.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Delete Innsynskrav as admin
    response = deleteAdmin("/innsynskrav/" + innsynskravDTOAnon.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Clean up
    response = delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/bruker/" + brukerDTO.getId(), brukerToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}
