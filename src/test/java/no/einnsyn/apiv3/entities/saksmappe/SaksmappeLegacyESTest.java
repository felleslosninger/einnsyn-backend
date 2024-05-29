package no.einnsyn.apiv3.entities.saksmappe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;

import java.util.List;
import no.einnsyn.apiv3.EinnsynLegacyElasticTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostES;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeES;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SaksmappeLegacyESTest extends EinnsynLegacyElasticTestBase {

  ArkivDTO arkivDTO;

  @BeforeAll
  void setUp() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
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
  void testSaksmappeES() throws Exception {
    var journalpost1JSON = getJournalpostJSON();
    var skjermingJSON = getSkjermingJSON();
    journalpost1JSON.put("skjerming", skjermingJSON);
    journalpost1JSON.put("korrespondansepart", new JSONArray(List.of(getKorrespondansepartJSON())));
    journalpost1JSON.put(
        "dokumentbeskrivelse", new JSONArray(List.of(getDokumentbeskrivelseJSON())));

    var journalpost2JSON = getJournalpostJSON();

    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("journalpost", new JSONArray(List.of(journalpost1JSON, journalpost2JSON)));
    var response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var journalpost1DTO = saksmappeDTO.getJournalpost().get(0).getExpandedObject();
    var journalpost2DTO = saksmappeDTO.getJournalpost().get(1).getExpandedObject();

    // Should have indexed one Saksmappe and two Journalposts
    var documentMap = captureIndexedDocuments(3);
    compareSaksmappe(saksmappeDTO, (SaksmappeES) documentMap.get(saksmappeDTO.getId()));
    compareJournalpost(journalpost1DTO, (JournalpostES) documentMap.get(journalpost1DTO.getId()));
    compareJournalpost(journalpost2DTO, (JournalpostES) documentMap.get(journalpost2DTO.getId()));

    // Clean up
    response = delete("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNull(saksmappeRepository.findById(saksmappeDTO.getId()).orElse(null));

    // Should have deleted one Saksmappe and two Journalposts
    var deletedDocuments = captureDeletedDocuments(3);
    assertTrue(deletedDocuments.contains(saksmappeDTO.getId()));
    assertTrue(deletedDocuments.contains(journalpost1DTO.getId()));
    assertTrue(deletedDocuments.contains(journalpost2DTO.getId()));
  }

  @Test
  void updateSaksmappeES() throws Exception {
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put(
        "journalpost", new JSONArray(List.of(getJournalpostJSON(), getJournalpostJSON())));
    var response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var journalpost1DTO = saksmappeDTO.getJournalpost().get(0).getExpandedObject();
    var journalpost2DTO = saksmappeDTO.getJournalpost().get(1).getExpandedObject();

    // Should have indexed one Saksmappe and two Journalposts
    var documentMap = captureIndexedDocuments(3);
    compareSaksmappe(saksmappeDTO, (SaksmappeES) documentMap.get(saksmappeDTO.getId()));
    compareJournalpost(journalpost1DTO, (JournalpostES) documentMap.get(journalpost1DTO.getId()));
    compareJournalpost(journalpost2DTO, (JournalpostES) documentMap.get(journalpost2DTO.getId()));

    // Update Saksmappe saksaar, this should trigger a reindex of Saksmappe and Journalposts
    reset(esClient);
    var updateJSON = new JSONObject();
    updateJSON.put("saksaar", "1111");
    response = put("/saksmappe/" + saksmappeDTO.getId(), updateJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    journalpost1DTO = journalpostService.get(saksmappeDTO.getJournalpost().get(0).getId());
    journalpost2DTO = journalpostService.get(saksmappeDTO.getJournalpost().get(1).getId());

    // Compare saksmappe and journalposts
    documentMap = captureIndexedDocuments(3);
    compareSaksmappe(saksmappeDTO, (SaksmappeES) documentMap.get(saksmappeDTO.getId()));
    compareJournalpost(journalpost1DTO, (JournalpostES) documentMap.get(journalpost1DTO.getId()));
    compareJournalpost(journalpost2DTO, (JournalpostES) documentMap.get(journalpost2DTO.getId()));

    // The following should already have been compared in the compareSaksmappe method, but let's be
    // explicit:
    var saksaar = "1111";
    var sakssekvensnummer = saksmappeDTO.getSakssekvensnummer();
    var saksaarShort = saksaar.substring(2);
    var expectedSaksnummerGenerert =
        List.of(
            saksaar + "/" + sakssekvensnummer,
            saksaarShort + "/" + sakssekvensnummer,
            sakssekvensnummer + "/" + saksaar,
            sakssekvensnummer + "/" + saksaarShort);
    assertEquals("1111", ((SaksmappeES) documentMap.get(saksmappeDTO.getId())).getSaksaar());
    assertEquals(
        expectedSaksnummerGenerert,
        ((SaksmappeES) documentMap.get(saksmappeDTO.getId())).getSaksnummerGenerert());
    assertEquals(
        expectedSaksnummerGenerert,
        ((JournalpostES) documentMap.get(journalpost1DTO.getId())).getSaksnummerGenerert());
    assertEquals(
        expectedSaksnummerGenerert,
        ((JournalpostES) documentMap.get(journalpost2DTO.getId())).getSaksnummerGenerert());

    // Clean up
    response = delete("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Should have deleted one Saksmappe and two Journalposts
    var deletedDocuments = captureDeletedDocuments(3);
    assertTrue(deletedDocuments.contains(saksmappeDTO.getId()));
    assertTrue(deletedDocuments.contains(journalpost1DTO.getId()));
    assertTrue(deletedDocuments.contains(journalpost2DTO.getId()));
  }
}
