package no.einnsyn.backend.entities.moetesak;

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
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeES;
import no.einnsyn.backend.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.backend.entities.moetesak.models.MoetesakES;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MoetesakLegacyESTest extends EinnsynLegacyElasticTestBase {

  ArkivDTO arkivDTO;
  MoetemappeDTO moetemappeDTO;

  @BeforeAll
  void setUp() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    captureIndexedDocuments(2);
    resetEs();
  }

  @AfterAll
  void tearDown() throws Exception {
    var response = delete("/arkiv/" + arkivDTO.getId());
    captureDeletedDocuments(2);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testMoetesakES() throws Exception {
    var moetesakJSON = getMoetesakJSON();
    var response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesakJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);

    // Should have reindexed both Moetesak and Moetemappe
    var documentMap = captureIndexedDocuments(2);
    resetEs();
    compareMoetesak(moetesakDTO, (MoetesakES) documentMap.get(moetesakDTO.getId()));
    moetemappeDTO = moetemappeService.get(moetemappeDTO.getId());
    compareMoetemappe(moetemappeDTO, (MoetemappeES) documentMap.get(moetemappeDTO.getId()));

    // Clean up
    response = delete("/moetesak/" + moetesakDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Should have deleted Moetesak from ES
    var deletedDocuments = captureDeletedDocuments(1);
    assertTrue(deletedDocuments.contains(moetesakDTO.getId()));
  }

  @Test
  void updateMoetesakES() throws Exception {
    var moetesakJSON = getMoetesakJSON();
    var response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesakJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);

    // Should have indexed two documents
    captureIndexedDocuments(2);
    resetEs();

    var updatedMoetesakJSON = getMoetesakJSON();
    updatedMoetesakJSON.put("moetesaksaar", "1999");
    response = patch("/moetesak/" + moetesakDTO.getId(), updatedMoetesakJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var updatedMoetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);

    // Should have reindexed both Moetesak and Moetemappe
    var documentMap = captureIndexedDocuments(2);
    resetEs();
    compareMoetesak(updatedMoetesakDTO, (MoetesakES) documentMap.get(updatedMoetesakDTO.getId()));
    moetemappeDTO = moetemappeService.get(moetemappeDTO.getId());
    compareMoetemappe(moetemappeDTO, (MoetemappeES) documentMap.get(moetemappeDTO.getId()));

    // This has already been checked in compare*, but let's be explicit:
    assertEquals(
        "1999", ((MoetesakES) documentMap.get(updatedMoetesakDTO.getId())).getMøtesaksår());

    // Clean up
    response = delete("/moetesak/" + updatedMoetesakDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Should have deleted Moetesak from ES
    var deletedDocuments = captureDeletedDocuments(1);
    assertTrue(deletedDocuments.contains(updatedMoetesakDTO.getId()));
  }

  @Test
  void testMoetesakWithAdmEnhet() throws Exception {
    var moetesakJSON = getMoetesakJSON();
    moetesakJSON.put("utvalg", "UNDER");
    var response = post("/moetesak", moetesakJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);

    // Should have indexed one Moetesak and one Moetemappe
    var documentMap = captureIndexedDocuments(1);
    resetEs();
    var moetesakES = (MoetesakES) documentMap.get(moetesakDTO.getId());
    compareMoetesak(moetesakDTO, moetesakES);

    var journalenhetDTO = gson.fromJson(get("/enhet/" + journalenhetId).getBody(), EnhetDTO.class);
    var underenhetDTO = gson.fromJson(get("/enhet/" + underenhetId).getBody(), EnhetDTO.class);

    assertEquals(
        List.of(underenhetDTO.getExternalId(), journalenhetDTO.getExternalId(), rootEnhetIri),
        moetesakES.getArkivskaperTransitive());
    assertEquals(
        List.of(underenhetDTO.getNavn(), journalenhetDTO.getNavn(), rootEnhetNavn),
        moetesakES.getArkivskaperNavn());

    // Clean up
    response = delete("/moetesak/" + moetesakDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNull(moetesakRepository.findById(moetesakDTO.getId()).orElse(null));

    // Should have deleted one Moetesak
    var deletedDocuments = captureDeletedDocuments(1);
    assertTrue(deletedDocuments.contains(moetesakDTO.getId()));
  }

  @Test
  void testKommerTilBehandling() throws Exception {

    // No moetemappe
    var moetesakJSON = getMoetesakJSON();
    var response = post("/moetesak", moetesakJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);

    // Should have indexed one Moetesak
    var documentMap = captureIndexedDocuments(1);
    resetEs();
    var moetesakES = (MoetesakES) documentMap.get(moetesakDTO.getId());
    compareMoetesak(moetesakDTO, moetesakES);
    assertEquals("KommerTilBehandlingMøtesaksregistrering", moetesakES.getType().getFirst());

    // Should convert "KommerTilBehandlingMøtesaksregistrering" to "Møtesaksregistrering"
    var moetesakWithMoetemappeJSON = new JSONObject();
    moetesakWithMoetemappeJSON.put("moetemappe", moetemappeDTO.getId());
    response = patch("/moetesak/" + moetesakDTO.getId(), moetesakWithMoetemappeJSON);
    moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    documentMap = captureIndexedDocuments(2);
    resetEs();
    var moetesakWithMoetemappeES = (MoetesakES) documentMap.get(moetesakDTO.getId());
    assertEquals("Møtesaksregistrering", moetesakWithMoetemappeES.getType().getFirst());

    // Clean up
    response = delete("/moetesak/" + moetesakDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNull(moetesakRepository.findById(moetesakDTO.getId()).orElse(null));

    // Should have deleted one Moetesak
    var deletedDocuments = captureDeletedDocuments(1);
    assertTrue(deletedDocuments.contains(moetesakDTO.getId()));
  }

  @Test
  void markMoetesakWithFulltextDocumentsAsSuch() throws Exception {
    // Create moetesak with dokumentbeskrivelse and dokumentobjekt
    var moetesak1JSON = getMoetesakJSON();
    moetesak1JSON.put("offentligTittel", "Moetesak with fulltext");
    var dokumentbeskrivelseJSON = getDokumentbeskrivelseJSON();
    dokumentbeskrivelseJSON.put("dokumentobjekt", new JSONArray(List.of(getDokumentobjektJSON())));
    moetesak1JSON.put("dokumentbeskrivelse", new JSONArray(List.of(dokumentbeskrivelseJSON)));

    var response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesak1JSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetesak1DTO = gson.fromJson(response.getBody(), MoetesakDTO.class);

    var documentMap = captureIndexedDocuments(2);
    resetEs();
    assertNotNull(documentMap.get(moetemappeDTO.getId()));
    assertNotNull(documentMap.get(moetesak1DTO.getId()));

    // The moetesak should be marked as fulltext
    var journalpost1ES = (MoetesakES) documentMap.get(moetesak1DTO.getId());
    assertTrue(journalpost1ES.isFulltext());

    // Create moetesak without dokumentbeskrivelse
    var moetesak2JSON = getMoetesakJSON();
    response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesak2JSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var moetesak2DTO = gson.fromJson(response.getBody(), MoetesakDTO.class);
    documentMap = captureIndexedDocuments(2);
    resetEs();
    assertNotNull(documentMap.get(moetemappeDTO.getId()));
    assertNotNull(documentMap.get(moetesak2DTO.getId()));

    // The moetesak should not be marked as fulltext
    var journalpost2ES = (MoetesakES) documentMap.get(moetesak2DTO.getId());
    assertFalse(journalpost2ES.isFulltext());

    // Cleanup
    response = delete("/moetesak/" + moetesak1DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNull(moetesakRepository.findById(moetesak1DTO.getId()).orElse(null));

    response = delete("/moetesak/" + moetesak2DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNull(moetesakRepository.findById(moetesak2DTO.getId()).orElse(null));
    captureDeletedDocuments(2);
  }
}
