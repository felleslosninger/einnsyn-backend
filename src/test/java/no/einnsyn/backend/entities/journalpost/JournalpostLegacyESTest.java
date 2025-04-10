package no.einnsyn.backend.entities.journalpost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import no.einnsyn.backend.EinnsynLegacyElasticTestBase;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostES;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeES;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class JournalpostLegacyESTest extends EinnsynLegacyElasticTestBase {

  ArkivDTO arkivDTO;
  SaksmappeDTO saksmappeDTO;

  @BeforeAll
  void setUp() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    captureIndexedDocuments(1);
    resetEs();
  }

  @AfterAll
  void tearDown() throws Exception {
    var response = delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    captureDeletedDocuments(1);
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
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    saksmappeDTO = saksmappeService.get(saksmappeDTO.getId()); // Update to get journalpost list

    // Should have indexed the Journalpost, and the Saksmappe
    var documentMap = captureIndexedDocuments(2);
    resetEs();
    compareJournalpost(journalpostDTO, (JournalpostES) documentMap.get(journalpostDTO.getId()));
    compareSaksmappe(saksmappeDTO, (SaksmappeES) documentMap.get(saksmappeDTO.getId()));

    // Clean up
    response = delete("/journalpost/" + journalpostDTO.getId());
    journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertTrue(journalpostDTO.getDeleted());

    // Should have deleted the Journalpost
    captureDeletedDocuments(1);
  }

  @Test
  void testJournalpostWithAdmEnhet() throws Exception {
    var journalpostJSON = getJournalpostJSON();
    var korrpartJSON = getKorrespondansepartJSON();
    korrpartJSON.put("administrativEnhet", "UNDER");
    korrpartJSON.put("erBehandlingsansvarlig", true);
    journalpostJSON.put("korrespondansepart", new JSONArray(List.of(korrpartJSON)));
    var response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Should have indexed one Journalpost and one Saksmappe
    var documentMap = captureIndexedDocuments(2);
    var journalpostES = (JournalpostES) documentMap.get(journalpostDTO.getId());
    compareJournalpost(journalpostDTO, journalpostES);

    var journalenhetDTO = gson.fromJson(get("/enhet/" + journalenhetId).getBody(), EnhetDTO.class);
    var underenhetDTO = gson.fromJson(get("/enhet/" + underenhetId).getBody(), EnhetDTO.class);

    assertEquals(
        List.of(underenhetDTO.getExternalId(), journalenhetDTO.getExternalId(), rootEnhetIri),
        journalpostES.getArkivskaperTransitive());
    assertEquals(
        List.of(underenhetDTO.getNavn(), journalenhetDTO.getNavn(), rootEnhetNavn),
        journalpostES.getArkivskaperNavn());

    // Clean up
    response = delete("/journalpost/" + journalpostDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNull(journalpostRepository.findById(journalpostDTO.getId()).orElse(null));

    // Should have deleted one Journalpost
    var deletedDocuments = captureDeletedDocuments(1);
    assertTrue(deletedDocuments.contains(journalpostDTO.getId()));
  }

  @Test
  void reindexWhenAddingKorrespondansepart() throws Exception {
    var journalpostJSON = getJournalpostJSON();
    var response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Should have indexed one Journalpost and one Saksmappe
    var documentMap = captureIndexedDocuments(2);
    resetEs();
    var journalpostES = (JournalpostES) documentMap.get(journalpostDTO.getId());
    compareJournalpost(journalpostDTO, journalpostES);

    // Add a Korrespondansepart
    var korrespondansepartJSON = getKorrespondansepartJSON();
    response =
        post(
            "/journalpost/" + journalpostDTO.getId() + "/korrespondansepart",
            korrespondansepartJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // Should have indexed one Journalpost and one Saksmappe
    documentMap = captureIndexedDocuments(2);
    resetEs();
    assertNotNull(documentMap.get(journalpostDTO.getId()));
    assertNotNull(documentMap.get(saksmappeDTO.getId()));

    // Clean up
    response = delete("/journalpost/" + journalpostDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNull(journalpostRepository.findById(journalpostDTO.getId()).orElse(null));
    captureDeletedDocuments(1);
  }

  @Test
  void reindexWhenAddingDokumentbeskrivelse() throws Exception {
    var journalpostJSON = getJournalpostJSON();
    var response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Should have indexed one Journalpost and one Saksmappe
    var documentMap = captureIndexedDocuments(2);
    resetEs();
    var journalpostES = (JournalpostES) documentMap.get(journalpostDTO.getId());
    compareJournalpost(journalpostDTO, journalpostES);

    // Add a Dokumentbeskrivelse
    var dokumentbeskrivelseJSON = getDokumentbeskrivelseJSON();
    response =
        post(
            "/journalpost/" + journalpostDTO.getId() + "/dokumentbeskrivelse",
            dokumentbeskrivelseJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // Should have indexed one Journalpost and one Saksmappe
    documentMap = captureIndexedDocuments(2);
    resetEs();
    assertNotNull(documentMap.get(journalpostDTO.getId()));
    assertNotNull(documentMap.get(saksmappeDTO.getId()));

    // Clean up
    response = delete("/journalpost/" + journalpostDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNull(journalpostRepository.findById(journalpostDTO.getId()).orElse(null));
    captureDeletedDocuments(1);
  }

  @Test
  void markJournalpostWithFulltextDocumentsAsSuch() throws Exception {
    // Create journalpost with dokumentbeskrivelse and dokumentobjekt
    var journalpost1JSON = getJournalpostJSON();
    journalpost1JSON.put("offentligTittel", "Journalpost with fulltext");
    var dokumentbeskrivelseJSON = getDokumentbeskrivelseJSON();
    dokumentbeskrivelseJSON.put("dokumentobjekt", new JSONArray(List.of(getDokumentobjektJSON())));
    journalpost1JSON.put("dokumentbeskrivelse", new JSONArray(List.of(dokumentbeskrivelseJSON)));

    var response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpost1JSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpost1DTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    var documentMap = captureIndexedDocuments(2);
    resetEs();
    assertNotNull(documentMap.get(saksmappeDTO.getId()));
    assertNotNull(documentMap.get(journalpost1DTO.getId()));

    // The journalpost should be marked as fulltext
    var journalpost1ES = (JournalpostES) documentMap.get(journalpost1DTO.getId());
    assertTrue(journalpost1ES.isFulltext());

    // Create journalpost without dokumentbeskrivelse
    var journalpost2JSON = getJournalpostJSON();
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpost2JSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var journalpost2DTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    documentMap = captureIndexedDocuments(2);
    resetEs();
    assertNotNull(documentMap.get(saksmappeDTO.getId()));
    assertNotNull(documentMap.get(journalpost2DTO.getId()));

    // The journalpost should not be marked as fulltext
    var journalpost2ES = (JournalpostES) documentMap.get(journalpost2DTO.getId());
    assertFalse(journalpost2ES.isFulltext());

    // Cleanup
    response = delete("/journalpost/" + journalpost1DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNull(journalpostRepository.findById(journalpost1DTO.getId()).orElse(null));

    response = delete("/journalpost/" + journalpost2DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNull(journalpostRepository.findById(journalpost2DTO.getId()).orElse(null));
    captureDeletedDocuments(2);
  }
}
