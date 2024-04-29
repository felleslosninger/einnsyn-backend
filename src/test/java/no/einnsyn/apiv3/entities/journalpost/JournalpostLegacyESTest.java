package no.einnsyn.apiv3.entities.journalpost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;

import java.util.List;
import java.util.concurrent.TimeUnit;
import no.einnsyn.apiv3.EinnsynLegacyElasticTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostES;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeES;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class JournalpostLegacyESTest extends EinnsynLegacyElasticTestBase {

  ArkivDTO arkivDTO;
  SaksmappeDTO saksmappeDTO;

  @BeforeAll
  void setUp() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
  }

  @AfterAll
  void tearDown() throws Exception {
    var response = delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @BeforeEach
  void resetMocks() {
    reset(esClient);
  }

  @Test
  void testJournalpostES() throws Exception {
    var journalpostJSON = getJournalpostJSON();

    var skjermingJSON = getSkjermingJSON();
    journalpostJSON.put("skjerming", skjermingJSON);

    var korrespondansepart1JSON = getKorrespondansepartJSON();
    var korrespondansepart2JSON = getKorrespondansepartJSON();
    korrespondansepart2JSON.put("korrespondansepartNavn", "test 2 navn");
    korrespondansepart2JSON.put("korrespondansepartNavnSensitiv", "test 2 navn sensitiv");
    korrespondansepart2JSON.put("erBehandlingsansvarlig", true);
    korrespondansepart2JSON.put("korrespondanseparttype", "avsender");
    korrespondansepart2JSON.put("epostadresse", "epost@example.com");
    korrespondansepart2JSON.put("administrativEnhet", "https://testAdmEnhet2");
    journalpostJSON.put(
        "korrespondansepart",
        new JSONArray(List.of(korrespondansepart1JSON, korrespondansepart2JSON)));

    var dokumentbeskrivelse1JSON = getDokumentbeskrivelseJSON();
    var dokumentbeskrivelse2JSON = getDokumentbeskrivelseJSON();
    journalpostJSON.put(
        "dokumentbeskrivelse",
        new JSONArray(List.of(dokumentbeskrivelse1JSON, dokumentbeskrivelse2JSON)));

    var response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    System.err.println("Okai");
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    saksmappeDTO = saksmappeService.get(saksmappeDTO.getId()); // Update to get journalpost list

    // Await indexing
    waiter.await(100, TimeUnit.MILLISECONDS);
    System.err.println("Await done.");

    // Should have indexed the Journalpost, and the Saksmappe
    var documents = captureIndexedDocuments(2);
    System.err.println(documents[0]);
    System.err.println(documents[1]);
    compareJournalpost(journalpostDTO, (JournalpostES) documents[0]);
    compareSaksmappe(saksmappeDTO, (SaksmappeES) documents[1]);

    // Clean up
    response = delete("/journalpost/" + journalpostDTO.getId());
    journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertTrue(journalpostDTO.getDeleted());
  }
}
