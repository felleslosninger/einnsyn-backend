package no.einnsyn.backend.entities.innsynskravbestilling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.authentication.bruker.models.TokenResponse;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.clients.ip.IPSender;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InnsynskravBestillingApiKeyAuthTest extends EinnsynControllerTestBase {

  @MockBean IPSender ipSender;

  @Test
  void testList() throws Exception {

    // Add bruker
    var brukerJSON = getBrukerJSON();
    var response = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    var bruker = brukerService.findById(brukerDTO.getId());

    // Activate bruker
    response = patch("/bruker/" + brukerDTO.getId() + "/activate/" + bruker.getSecret());
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

    // Add InnsynskravBestilling
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray(List.of(journalpostDTO.getId())));
    response = post("/innsynskravBestilling", innsynskravBestillingJSON, brukerToken);

    // Verify that anon cannot list InnsynskravBestilling
    response = getAnon("/bruker/" + brukerDTO.getId() + "/innsynskravBestilling");
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Verify that other users cannot list InnsynskravBestilling
    response = get("/bruker/" + brukerDTO.getId() + "/innsynskravBestilling");
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Verify that the user can list InnsynskravBestilling
    response = get("/bruker/" + brukerDTO.getId() + "/innsynskravBestilling", brukerToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    assertEquals(
        HttpStatus.OK, delete("/bruker/" + brukerDTO.getId(), brukerToken).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }

  @Test
  void testGet() throws Exception {

    // Add bruker
    var brukerJSON = getBrukerJSON();
    var response = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    var bruker = brukerService.findById(brukerDTO.getId());

    // Activate bruker
    response = patch("/bruker/" + brukerDTO.getId() + "/activate/" + bruker.getSecret());
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

    // Add InnsynskravBestilling
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    innsynskravBestillingJSON.put(
        "innsynskrav",
        new JSONArray(List.of(getInnsynskravJSON().put("journalpost", journalpostDTO.getId()))));
    response = post("/innsynskravBestilling", innsynskravBestillingJSON, brukerToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);

    // Verify that anon cannot get InnsynskravBestilling
    response = getAnon("/innsynskravBestilling/" + innsynskravBestillingDTO.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Verify that other users cannot get InnsynskravBestilling
    response = get("/innsynskravBestilling/" + innsynskravBestillingDTO.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Verify that the user can get InnsynskravBestilling
    response = get("/innsynskravBestilling/" + innsynskravBestillingDTO.getId(), brukerToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    assertEquals(
        HttpStatus.OK, delete("/bruker/" + brukerDTO.getId(), brukerToken).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }

  @Test
  void testInsertUpdateDeleteInnsynskravBestilling() throws Exception {

    // Add bruker
    var brukerJSON = getBrukerJSON();
    var response = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    var bruker = brukerService.findById(brukerDTO.getId());

    // Activate bruker
    response = patch("/bruker/" + brukerDTO.getId() + "/activate/" + bruker.getSecret());
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

    // Add InnsynskravBestilling as Bruker
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    innsynskravBestillingJSON.put(
        "innsynskrav",
        new JSONArray(List.of(getInnsynskravJSON().put("journalpost", journalpostDTO.getId()))));
    response = post("/innsynskravBestilling", innsynskravBestillingJSON, brukerToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);

    // Add InnsynskravBestilling as anonymous
    response = postAnon("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTOAnon =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);

    // Update InnsynskravBestilling as Bruker (fails, locked InnsynskravBestillings are immutable)
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray());
    response =
        patch(
            "/innsynskravBestilling/" + innsynskravBestillingDTO.getId(),
            innsynskravBestillingJSON,
            brukerToken);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Update InnsynskravBestilling as anonymous
    response =
        patch(
            "/innsynskravBestilling/" + innsynskravBestillingDTOAnon.getId(),
            innsynskravBestillingJSON);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Delete InnsynskravBestilling as anonymous (fails)
    response = deleteAnon("/innsynskravBestilling/" + innsynskravBestillingDTO.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    response = deleteAnon("/innsynskravBestilling/" + innsynskravBestillingDTOAnon.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Delete InnsynskravBestilling as Bruker
    response = delete("/innsynskravBestilling/" + innsynskravBestillingDTO.getId(), brukerToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response =
        delete("/innsynskravBestilling/" + innsynskravBestillingDTOAnon.getId(), brukerToken);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Delete InnsynskravBestilling as owner of Enhet
    response = delete("/innsynskravBestilling/" + innsynskravBestillingDTOAnon.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Delete InnsynskravBestilling as admin
    response = deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingDTOAnon.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Clean up
    response = delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/bruker/" + brukerDTO.getId(), brukerToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}
