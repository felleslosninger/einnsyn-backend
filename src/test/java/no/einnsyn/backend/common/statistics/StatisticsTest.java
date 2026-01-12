package no.einnsyn.backend.common.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.statistics.models.StatisticsResponse;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.innsynskravbestilling.InnsynskravBestillingTestService;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.tasks.TaskTestService;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class StatisticsTest extends EinnsynControllerTestBase {

  @Lazy @Autowired private InnsynskravBestillingTestService innsynskravBestillingTestService;
  @Lazy @Autowired private StatisticsTestService statisticsTestService;
  @Lazy @Autowired private TaskTestService taskTestService;

  ArkivDTO arkivDTO;
  ArkivdelDTO arkivdelDTO;
  SaksmappeDTO saksmappeDTO;
  JournalpostDTO journalpostWithFulltextDTO;
  JournalpostDTO journalpostWithoutFulltextDTO;
  JournalpostDTO journalpostOldDTO;
  InnsynskravBestillingDTO innsynskravBestillingDTO;

  @BeforeAll
  void setup() throws Exception {
    // Create arkiv structure
    System.err.println("SETUP");
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    // Create saksmappe
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("saksaar", "2024");
    saksmappeJSON.put("sakssekvensnummer", "1");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Create journalpost with fulltext
    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("offentligTittel", "Journalpost with fulltext");
    journalpostJSON.put("journalsekvensnummer", "1");
    journalpostJSON.put("journalpostnummer", 101);
    var dokumentobjektJSON = getDokumentobjektJSON();
    var dokumentbeskrivelseJSON = getDokumentbeskrivelseJSON();
    dokumentbeskrivelseJSON.put("dokumentobjekt", new JSONArray().put(dokumentobjektJSON));
    journalpostJSON.put("dokumentbeskrivelse", new JSONArray().put(dokumentbeskrivelseJSON));
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    journalpostWithFulltextDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Create journalpost without fulltext
    journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("offentligTittel", "Journalpost without fulltext");
    journalpostJSON.put("journalsekvensnummer", "2");
    journalpostJSON.put("journalpostnummer", 102);
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    journalpostWithoutFulltextDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Create an old journalpost (created in 2023)
    journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("offentligTittel", "Old journalpost");
    journalpostJSON.put("journalsekvensnummer", "3");
    journalpostJSON.put("journalpostnummer", 103);
    journalpostJSON.put("publisertDato", "2023-06-15T00:00:00Z");
    journalpostJSON.put("oppdatertDato", "2023-06-15T00:00:00Z");
    response = postAdmin("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    journalpostOldDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    statisticsTestService.modifyJournalpostCreatedDate(
        journalpostOldDTO.getId(), -1, ChronoUnit.YEARS);

    // Create innsynskrav
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", journalpostOldDTO.getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));
    response = postAdmin("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    innsynskravBestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);

    // Verify innsynskrav
    var innsynskravBestillingId = innsynskravBestillingDTO.getId();
    var verificationSecret =
        innsynskravBestillingTestService.getVerificationSecret(innsynskravBestillingDTO.getId());
    response =
        patch(
            "/innsynskravBestilling/" + innsynskravBestillingId + "/verify/" + verificationSecret,
            null);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravBestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    var innsynskravId = innsynskravBestillingDTO.getInnsynskrav().getFirst().getId();
    statisticsTestService.modifyInnsynskravCreatedDate(innsynskravId, -6, ChronoUnit.MONTHS);

    // Reindex old innsynskrav
    taskTestService.updateOutdatedDocuments();

    // Refresh indices
    esClient.indices().refresh(r -> r.index(elasticsearchIndex));
  }

  @AfterAll
  void teardown() throws Exception {
    deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingDTO.getId());
    deleteAdmin("/journalpost/" + journalpostOldDTO.getId());
    deleteAdmin("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void testAllResults() throws Exception {
    var from = LocalDate.now().minusYears(10).toString();
    var to = LocalDate.now().plusDays(1).toString();
    var response = get("/statistics?aggregateFrom=" + from + "&aggregateTo=" + to);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var statisticsResponse = gson.fromJson(response.getBody(), StatisticsResponse.class);

    assertNotNull(statisticsResponse);
    assertNotNull(statisticsResponse.getSummary());
    assertNotNull(statisticsResponse.getTimeSeries());
    assertNotNull(statisticsResponse.getMetadata());

    // Saksmappe + 3 journalposts
    assertEquals(4, statisticsResponse.getSummary().getCreatedCount());
    assertEquals(1, statisticsResponse.getSummary().getCreatedWithFulltextCount());
    assertEquals(1, statisticsResponse.getSummary().getCreatedInnsynskravCount());
  }

  @Test
  void testFilteredJournalpostInnsynskrav() throws Exception {
    var from = LocalDate.now().minusMonths(1).toString();
    var to = LocalDate.now().plusDays(1).toString();
    var response = get("/statistics?aggregateFrom=" + from + "&aggregateTo=" + to);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var statisticsResponse = gson.fromJson(response.getBody(), StatisticsResponse.class);

    assertNotNull(statisticsResponse);
    assertNotNull(statisticsResponse.getSummary());
    assertNotNull(statisticsResponse.getTimeSeries());
    assertNotNull(statisticsResponse.getMetadata());

    // Saksmappe + 2 journalposts (one is too old)
    assertEquals(3, statisticsResponse.getSummary().getCreatedCount());
    assertEquals(1, statisticsResponse.getSummary().getCreatedWithFulltextCount());
    assertEquals(0, statisticsResponse.getSummary().getCreatedInnsynskravCount());

    var metadata = statisticsResponse.getMetadata();
    assertEquals(from, metadata.getAggregateFrom());
    assertEquals(to, metadata.getAggregateTo());
    assertNotNull(metadata.getAggregateInterval());
  }

  @Test
  void testOldJournalpost() throws Exception {
    // Query for
    var from = LocalDate.now().minusMonths(13).toString();
    var to = LocalDate.now().minusMonths(11).toString();
    var response = get("/statistics?aggregateFrom=" + from + "&aggregateTo=" + to);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var statisticsResponse = gson.fromJson(response.getBody(), StatisticsResponse.class);
    assertNotNull(statisticsResponse);

    var summary = statisticsResponse.getSummary();
    // Should have created 1 journalpost
    assertEquals(1, summary.getCreatedCount());
    assertEquals(0, summary.getCreatedWithFulltextCount());
    assertEquals(0, summary.getCreatedInnsynskravCount());

    var metadata = statisticsResponse.getMetadata();
    assertEquals(from, metadata.getAggregateFrom());
    assertEquals(to, metadata.getAggregateTo());
  }

  @Test
  void testOldInnsynskrav() throws Exception {
    // Query for
    var from = LocalDate.now().minusMonths(7).toString();
    var to = LocalDate.now().minusMonths(5).toString();
    var response = get("/statistics?aggregateFrom=" + from + "&aggregateTo=" + to);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var statisticsResponse = gson.fromJson(response.getBody(), StatisticsResponse.class);
    assertNotNull(statisticsResponse);

    var summary = statisticsResponse.getSummary();
    // Should have created 1 innsynskrav
    assertEquals(0, summary.getCreatedCount());
    assertEquals(0, summary.getCreatedWithFulltextCount());
    assertEquals(1, summary.getCreatedInnsynskravCount());

    var metadata = statisticsResponse.getMetadata();
    assertEquals(from, metadata.getAggregateFrom());
    assertEquals(to, metadata.getAggregateTo());
  }

  @Test
  void testStatisticsWithHourInterval() throws Exception {
    // Query for a short period with hour interval
    var today = LocalDate.now();
    var response = get("/statistics?aggregateFrom=" + today + "&aggregateTo=" + today);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var statisticsResponse = gson.fromJson(response.getBody(), StatisticsResponse.class);
    assertNotNull(statisticsResponse);

    var metadata = statisticsResponse.getMetadata();
    assertEquals("hour", metadata.getAggregateInterval());
  }

  @Test
  void testStatisticsWithDayInterval() throws Exception {
    // Query with day interval - use a large enough range that hour would exceed 1000 buckets
    // 1000 hours = ~42 days, so use 50 days to force day interval
    var from = "2024-01-01";
    var to = "2024-02-20"; // 50 days
    var response = get("/statistics?aggregateFrom=" + from + "&aggregateTo=" + to);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var statisticsResponse = gson.fromJson(response.getBody(), StatisticsResponse.class);
    assertNotNull(statisticsResponse);

    var metadata = statisticsResponse.getMetadata();
    assertEquals("day", metadata.getAggregateInterval());
  }

  @Test
  void testStatisticsWithWeekInterval() throws Exception {
    // Query with week interval - use > 1000 days to force week interval
    // 1000 days = ~143 weeks, so use 3 years (1095 days) to force week interval
    var from = "2021-01-01";
    var to = "2024-01-01"; // ~3 years
    var response = get("/statistics?aggregateFrom=" + from + "&aggregateTo=" + to);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var statisticsResponse = gson.fromJson(response.getBody(), StatisticsResponse.class);
    assertNotNull(statisticsResponse);

    var metadata = statisticsResponse.getMetadata();
    assertEquals("week", metadata.getAggregateInterval());
  }

  @Test
  void testStatisticsWithLongRangeDefaultsToMonth() throws Exception {
    // Query for period > 1000 weeks - should default to month interval
    // 1000 weeks = ~19 years, so use 20 years to force month interval
    var from = "2000-01-01";
    var to = "2020-01-01";
    var response = get("/statistics?aggregateFrom=" + from + "&aggregateTo=" + to);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var statisticsResponse = gson.fromJson(response.getBody(), StatisticsResponse.class);
    assertNotNull(statisticsResponse);

    var metadata = statisticsResponse.getMetadata();
    assertEquals("month", metadata.getAggregateInterval());
  }

  @Test
  void testOverrideIntervalToMonth() throws Exception {
    // Query for short period but override to month interval
    var today = LocalDate.now();
    var response =
        get(
            "/statistics?aggregateFrom="
                + today
                + "&aggregateTo="
                + today
                + "&aggregateInterval=month");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var statisticsResponse = gson.fromJson(response.getBody(), StatisticsResponse.class);
    assertNotNull(statisticsResponse);

    var metadata = statisticsResponse.getMetadata();
    assertEquals("month", metadata.getAggregateInterval());
  }

  @Test
  void testStatisticsTimeSeriesStructure() throws Exception {
    var from = LocalDate.now().minusYears(10).toString();
    var to = LocalDate.now().plusDays(1).toString();
    var response = get("/statistics?aggregateFrom=" + from + "&aggregateTo=" + to);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var statisticsResponse = gson.fromJson(response.getBody(), StatisticsResponse.class);
    assertNotNull(statisticsResponse);

    var timeSeries = statisticsResponse.getTimeSeries();
    assertNotNull(timeSeries);
    assertEquals(3, timeSeries.size());

    // Verify that each data point has the required fields
    for (var dataPoint : timeSeries) {
      assertNotNull(dataPoint.getTime());
      assertNotNull(dataPoint.getCreatedCount());
    }
  }

  @Test
  void testStatisticsWithAdministrativEnhetFilter() throws Exception {
    var from = LocalDate.now().minusYears(2).toString();
    var to = LocalDate.now().plusDays(1).toString();
    var response =
        get(
            "/statistics?aggregateFrom="
                + from
                + "&aggregateTo="
                + to
                + "&administrativEnhet="
                + journalenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var statisticsResponse = gson.fromJson(response.getBody(), StatisticsResponse.class);
    assertNotNull(statisticsResponse);

    var summary = statisticsResponse.getSummary();
    assertEquals(4, summary.getCreatedCount());
    assertEquals(1, summary.getCreatedWithFulltextCount());
    assertEquals(1, summary.getCreatedInnsynskravCount());
  }

  @Test
  void testStatisticsWithEntityFilter() throws Exception {
    var from = LocalDate.now().minusYears(2).toString();
    var to = LocalDate.now().plusDays(1).toString();

    // Filter for Journalpost
    var response =
        get("/statistics?aggregateFrom=" + from + "&aggregateTo=" + to + "&entity=Journalpost");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var statisticsResponse = gson.fromJson(response.getBody(), StatisticsResponse.class);
    assertNotNull(statisticsResponse);

    var summary = statisticsResponse.getSummary();
    assertEquals(3, summary.getCreatedCount());
    assertEquals(1, summary.getCreatedWithFulltextCount());
    assertEquals(1, summary.getCreatedInnsynskravCount());

    // Filter for Saksmappe
    response =
        get("/statistics?aggregateFrom=" + from + "&aggregateTo=" + to + "&entity=Saksmappe");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    statisticsResponse = gson.fromJson(response.getBody(), StatisticsResponse.class);
    assertNotNull(statisticsResponse);

    summary = statisticsResponse.getSummary();
    assertEquals(1, summary.getCreatedCount());
    assertEquals(0, summary.getCreatedWithFulltextCount());
    assertEquals(0, summary.getCreatedInnsynskravCount());
  }

  @Test
  void testStatisticsWithFulltextFilter() throws Exception {
    var from = LocalDate.now().minusYears(2).toString();
    var to = LocalDate.now().plusDays(1).toString();
    var response =
        get("/statistics?aggregateFrom=" + from + "&aggregateTo=" + to + "&fulltext=true");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var statisticsResponse = gson.fromJson(response.getBody(), StatisticsResponse.class);
    assertNotNull(statisticsResponse);
    var summary = statisticsResponse.getSummary();
    assertEquals(1, summary.getCreatedCount());
    assertEquals(1, summary.getCreatedWithFulltextCount());
    assertEquals(0, summary.getCreatedInnsynskravCount());
  }

  @Test
  void testStatisticsWithMultipleFilters() throws Exception {
    var from = LocalDate.now().minusYears(2).toString();
    var to = LocalDate.now().plusDays(1).toString();
    var response =
        get(
            "/statistics?aggregateFrom="
                + from
                + "&aggregateTo="
                + to
                + "&entity=Journalpost&administrativEnhet="
                + journalenhetId
                + "&fulltext=true");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var statisticsResponse = gson.fromJson(response.getBody(), StatisticsResponse.class);
    assertNotNull(statisticsResponse);

    var summary = statisticsResponse.getSummary();
    assertEquals(1, summary.getCreatedCount());
    assertEquals(1, summary.getCreatedWithFulltextCount());
    assertEquals(0, summary.getCreatedInnsynskravCount());
  }

  @Test
  void testStatisticsWithDateRangeFilters() throws Exception {
    var from = LocalDate.now().minusYears(2).toString();
    var to = LocalDate.now().plusDays(1).toString();
    var response =
        get(
            "/statistics?aggregateFrom="
                + from
                + "&aggregateTo="
                + to
                + "&publisertDatoFrom=2023-06-15&publisertDatoTo=2023-06-15");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var statisticsResponse = gson.fromJson(response.getBody(), StatisticsResponse.class);
    assertNotNull(statisticsResponse);
    var summary = statisticsResponse.getSummary();
    assertEquals(1, summary.getCreatedCount());
    assertEquals(0, summary.getCreatedWithFulltextCount());
    assertEquals(1, summary.getCreatedInnsynskravCount());
  }

  @Test
  void testStatisticsWithSaksaarFilter() throws Exception {
    var from = LocalDate.now().minusYears(2).toString();
    var to = LocalDate.now().plusDays(1).toString();
    var response =
        get("/statistics?aggregateFrom=" + from + "&aggregateTo=" + to + "&saksaar=2024");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var statisticsResponse = gson.fromJson(response.getBody(), StatisticsResponse.class);
    assertNotNull(statisticsResponse);
    var summary = statisticsResponse.getSummary();
    assertEquals(4, summary.getCreatedCount());
    assertEquals(1, summary.getCreatedWithFulltextCount());
    assertEquals(1, summary.getCreatedInnsynskravCount());

    response = get("/statistics?aggregateFrom=" + from + "&aggregateTo=" + to + "&saksaar=2023");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    statisticsResponse = gson.fromJson(response.getBody(), StatisticsResponse.class);
    assertNotNull(statisticsResponse);
    summary = statisticsResponse.getSummary();
    assertEquals(0, summary.getCreatedCount());
    assertEquals(0, summary.getCreatedWithFulltextCount());
    assertEquals(0, summary.getCreatedInnsynskravCount());
  }
}
