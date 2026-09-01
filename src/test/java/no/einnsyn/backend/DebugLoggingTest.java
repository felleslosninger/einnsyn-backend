package no.einnsyn.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs a normal CRUD and order flow with TRACE logging enabled for the application. The debug and
 * trace log statements serialize entities and raw e-mails, and this verifies that those code paths
 * work instead of blowing up the request that triggers them in production.
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = "logging.level.no.einnsyn.backend=TRACE")
@ActiveProfiles("test")
class DebugLoggingTest extends EinnsynControllerTestBase {

  @Test
  void testCrudAndOrderFlowWithTraceLogging() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    // Create a Saksmappe with a nested Journalpost
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("journalpost", new JSONArray().put(getJournalpostJSON()));
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var journalpostId = getJournalpostList(saksmappeDTO.getId()).getItems().getFirst().getId();

    // Get with expand, list and update
    response = get("/saksmappe/" + saksmappeDTO.getId() + "?expand=journalpost");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = get("/saksmappe/" + saksmappeDTO.getId() + "/journalpost");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response =
        patch(
            "/journalpost/" + journalpostId,
            new JSONObject().put("offentligTittel", "Updated tittel"));
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Order the Journalpost anonymously, which sends a confirmation e-mail. With debug logging
    // enabled the raw MIME content of the e-mail is serialized and logged.
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    innsynskravBestillingJSON.put(
        "innsynskrav", new JSONArray().put(getInnsynskravJSON().put("journalpost", journalpostId)));
    response = postAnon("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    assertNotNull(innsynskravBestillingDTO.getId());

    // Clean up
    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.OK,
        deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingDTO.getId()).getStatusCode());
    deleteInnsynskravFromBestilling(innsynskravBestillingDTO);
  }
}
