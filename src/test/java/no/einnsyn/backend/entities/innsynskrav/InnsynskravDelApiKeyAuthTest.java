package no.einnsyn.backend.entities.innsynskrav;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.authentication.bruker.models.TokenResponse;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.bruker.models.Bruker;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InnsynskravApiKeyAuthTest extends EinnsynControllerTestBase {

  ArkivDTO arkivDTO;
  SaksmappeDTO saksmappeDTO;
  Bruker bruker1;
  String bruker1Token;
  Bruker bruker2;
  String bruker2Token;

  @BeforeAll
  void setUp() throws Exception {
    // Add Arkiv, Saksmappe with Journalposts
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put(
        "journalpost",
        new JSONArray()
            .put(getJournalpostJSON())
            .put(getJournalpostJSON())
            .put(getJournalpostJSON())
            .put(getJournalpostJSON())
            .put(getJournalpostJSON()));
    response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Insert bruker1
    var bruker1JSON = getBrukerJSON();
    response = post("/bruker", bruker1JSON);
    var bruker1DTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    bruker1 = brukerService.findById(bruker1DTO.getId());

    // Activate bruker1
    response = patch("/bruker/" + bruker1.getId() + "/activate/" + bruker1.getSecret());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Get token for bruker1
    var tokenResponse = post("/auth/token", getLoginJSON(bruker1JSON));
    assertEquals(HttpStatus.OK, tokenResponse.getStatusCode());
    var tokenResponseDTO = gson.fromJson(tokenResponse.getBody(), TokenResponse.class);
    bruker1Token = tokenResponseDTO.getToken();

    // Add bruker2
    var bruker2JSON = getBrukerJSON();
    response = post("/bruker", bruker2JSON);
    var bruker2DTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    bruker2 = brukerService.findById(bruker2DTO.getId());

    // Activate bruker2
    response = patch("/bruker/" + bruker2.getId() + "/activate/" + bruker2.getSecret());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Get token for bruker2
    tokenResponse = post("/auth/token", getLoginJSON(bruker2JSON));
    assertEquals(HttpStatus.OK, tokenResponse.getStatusCode());
    tokenResponseDTO = gson.fromJson(tokenResponse.getBody(), TokenResponse.class);
    bruker2Token = tokenResponseDTO.getToken();
  }

  @AfterAll
  void tearDown() throws Exception {
    // Clean up
    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/bruker/" + bruker1.getId(), bruker1Token).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/bruker/" + bruker2.getId(), bruker2Token).getStatusCode());

    // Make sure objects are deleted
    assertEquals(
        HttpStatus.NOT_FOUND, get("/bruker/" + bruker1.getId(), bruker1Token).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/bruker/" + bruker2.getId(), bruker2Token).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }

  @Test
  void testListInnsynskravByBruker() throws Exception {
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", saksmappeDTO.getJournalpost().getFirst().getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));
    var response = post("/innsynskravBestilling", innsynskravBestillingJSON, bruker1Token);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);

    // Unauthorized cannot list by bruker
    response = get("/bruker/" + bruker1.getId() + "/innsynskrav");
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Another user cannot list by bruker
    response = get("/bruker/" + bruker1.getId() + "/innsynskrav", bruker2Token);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Authorized can list by bruker
    response = get("/bruker/" + bruker1.getId() + "/innsynskrav", bruker1Token);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Admin can list by bruker
    response = getAdmin("/bruker/" + bruker1.getId() + "/innsynskrav");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Clean up
    assertEquals(
        HttpStatus.OK,
        delete("/innsynskravBestilling/" + innsynskravBestillingDTO.getId(), bruker1Token)
            .getStatusCode());
  }

  @Test
  void testListInnsynskravByInnsynskravBestilling() throws Exception {
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", saksmappeDTO.getJournalpost().getFirst().getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));
    var response = post("/innsynskravBestilling", innsynskravBestillingJSON, bruker1Token);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);

    // Unauthorized cannot list by InnsynskravBestilling
    response =
        getAnon("/innsynskravBestilling/" + innsynskravBestillingDTO.getId() + "/innsynskrav");
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Owner of the saksmappe / journalpost cannot list by InnsynskravBestilling
    response = get("/innsynskravBestilling/" + innsynskravBestillingDTO.getId() + "/innsynskrav");
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Another user cannot list by InnsynskravBestilling
    response =
        get(
            "/innsynskravBestilling/" + innsynskravBestillingDTO.getId() + "/innsynskrav",
            bruker2Token);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Authorized can list by InnsynskravBestilling
    response =
        get(
            "/innsynskravBestilling/" + innsynskravBestillingDTO.getId() + "/innsynskrav",
            bruker1Token);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Admin can list by InnsynskravBestilling
    response =
        getAdmin("/innsynskravBestilling/" + innsynskravBestillingDTO.getId() + "/innsynskrav");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Clean up
    assertEquals(
        HttpStatus.OK,
        delete("/innsynskravBestilling/" + innsynskravBestillingDTO.getId(), bruker1Token)
            .getStatusCode());
  }

  @Test
  void testListInnsynskravByEnhet() throws Exception {
    // Unauthorized cannot list by enhet
    var response = getAnon("/enhet/" + journalenhetId + "/innsynskrav");
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Another user cannot list by enhet
    response = get("/enhet/" + journalenhetId + "/innsynskrav", bruker2Token);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Another enhet cannot list by enhet
    response = get("/enhet/" + journalenhetId + "/innsynskrav", journalenhet2Key);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Authorized can list by enhet
    response = get("/enhet/" + journalenhetId + "/innsynskrav");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Admin can list by enhet
    response = getAdmin("/enhet/" + journalenhetId + "/innsynskrav");
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testGetInnsynskrav() throws Exception {
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", saksmappeDTO.getJournalpost().getFirst().getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));
    var response = post("/innsynskravBestilling", innsynskravBestillingJSON, bruker1Token);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    var innsynskravId = innsynskravBestillingDTO.getInnsynskrav().getFirst().getId();

    // Unauthorized cannot get
    response = getAnon("/innsynskrav/" + innsynskravId);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Another user cannot get
    response = get("/innsynskrav/" + innsynskravId, bruker2Token);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Owner of another Enhet cannot get
    response = get("/innsynskrav/" + innsynskravId, journalenhet2Key);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Authorized can get
    response = get("/innsynskrav/" + innsynskravId, bruker1Token);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Owner of the Enhet can get
    response = get("/innsynskrav/" + innsynskravId);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Admin can get
    response = getAdmin("/innsynskrav/" + innsynskravId);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Clean up
    assertEquals(
        HttpStatus.OK,
        delete("/innsynskravBestilling/" + innsynskravBestillingDTO.getId(), bruker1Token)
            .getStatusCode());
  }
}
