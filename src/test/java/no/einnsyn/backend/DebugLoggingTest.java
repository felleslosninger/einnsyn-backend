package no.einnsyn.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.base.BaseService;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.utils.mail.MailSenderService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs a normal CRUD and order flow with TRACE logging enabled for the classes that guard log
 * statements behind isDebugEnabled / isTraceEnabled.
 *
 * <p>The log levels are raised at runtime instead of through @SpringBootTest properties, so this
 * class can reuse the cached application context instead of booting its own.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DebugLoggingTest extends EinnsynControllerTestBase {

  private static final List<String> TRACED_LOGGERS =
      List.of(BaseService.class.getName(), MailSenderService.class.getName());

  private final Map<String, Level> originalLevels = new HashMap<>();

  @BeforeAll
  void enableTraceLogging() {
    for (var loggerName : TRACED_LOGGERS) {
      var logger = (Logger) LoggerFactory.getLogger(loggerName);
      originalLevels.put(loggerName, logger.getLevel());
      logger.setLevel(Level.TRACE);
    }
  }

  @AfterAll
  void restoreLogging() {
    for (var loggerName : TRACED_LOGGERS) {
      ((Logger) LoggerFactory.getLogger(loggerName)).setLevel(originalLevels.get(loggerName));
    }
  }

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
