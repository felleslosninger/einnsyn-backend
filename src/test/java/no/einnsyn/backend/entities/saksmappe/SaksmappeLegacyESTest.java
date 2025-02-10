package no.einnsyn.backend.entities.saksmappe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import no.einnsyn.backend.EinnsynLegacyElasticTestBase;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostES;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeES;
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
class SaksmappeLegacyESTest extends EinnsynLegacyElasticTestBase {

  ArkivDTO arkivDTO;
  ArkivdelDTO arkivdelDTO;

  @BeforeAll
  void setUp() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
  }

  @AfterAll
  void tearDown() throws Exception {
    var response = delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
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
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var journalpost1DTO = saksmappeDTO.getJournalpost().get(0).getExpandedObject();
    var journalpost2DTO = saksmappeDTO.getJournalpost().get(1).getExpandedObject();

    // Should have indexed one Saksmappe and two Journalposts
    var documentMap = captureIndexedDocuments(3);
    resetEs();
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
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var journalpost1DTO = saksmappeDTO.getJournalpost().get(0).getExpandedObject();
    var journalpost2DTO = saksmappeDTO.getJournalpost().get(1).getExpandedObject();

    // Should have indexed one Saksmappe and two Journalposts
    var documentMap = captureIndexedDocuments(3);
    resetEs();
    compareSaksmappe(saksmappeDTO, (SaksmappeES) documentMap.get(saksmappeDTO.getId()));
    compareJournalpost(journalpost1DTO, (JournalpostES) documentMap.get(journalpost1DTO.getId()));
    compareJournalpost(journalpost2DTO, (JournalpostES) documentMap.get(journalpost2DTO.getId()));

    // Update Saksmappe saksaar, this should trigger a reindex of Saksmappe and Journalposts
    var updateJSON = new JSONObject();
    updateJSON.put("saksaar", "1900");
    response = patch("/saksmappe/" + saksmappeDTO.getId(), updateJSON);
    System.err.println(response.getBody());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = get("/saksmappe/" + saksmappeDTO.getId() + "?expand=journalpost.korrespondansepart");
    saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    journalpost1DTO = saksmappeDTO.getJournalpost().get(0).getExpandedObject();
    journalpost2DTO = saksmappeDTO.getJournalpost().get(1).getExpandedObject();

    // Compare saksmappe and journalposts
    documentMap = captureIndexedDocuments(3);
    resetEs();
    compareSaksmappe(saksmappeDTO, (SaksmappeES) documentMap.get(saksmappeDTO.getId()));
    compareJournalpost(journalpost1DTO, (JournalpostES) documentMap.get(journalpost1DTO.getId()));
    compareJournalpost(journalpost2DTO, (JournalpostES) documentMap.get(journalpost2DTO.getId()));

    // The following should already have been compared in the compareSaksmappe method, but let's be
    // explicit:
    var saksaar = "1900";
    var sakssekvensnummer = saksmappeDTO.getSakssekvensnummer();
    var saksaarShort = saksaar.substring(2);
    var expectedSaksnummerGenerert =
        List.of(
            saksaar + "/" + sakssekvensnummer,
            saksaarShort + "/" + sakssekvensnummer,
            sakssekvensnummer + "/" + saksaar,
            sakssekvensnummer + "/" + saksaarShort);
    var jp1no = journalpost1DTO.getJournalpostnummer();
    var expectedJp1SaksnummerGenerert =
        expectedSaksnummerGenerert.stream().map(snr -> snr + "-" + jp1no).toList();
    var jp2no = journalpost2DTO.getJournalpostnummer();
    var expectedJp2SaksnummerGenerert =
        expectedSaksnummerGenerert.stream().map(snr -> snr + "-" + jp2no).toList();
    assertEquals("1900", ((SaksmappeES) documentMap.get(saksmappeDTO.getId())).getSaksaar());
    assertEquals(
        expectedSaksnummerGenerert,
        ((SaksmappeES) documentMap.get(saksmappeDTO.getId())).getSaksnummerGenerert());
    assertEquals(
        expectedJp1SaksnummerGenerert,
        ((JournalpostES) documentMap.get(journalpost1DTO.getId())).getSaksnummerGenerert());
    assertEquals(
        expectedJp2SaksnummerGenerert,
        ((JournalpostES) documentMap.get(journalpost2DTO.getId())).getSaksnummerGenerert());

    // Clean up
    captureDeletedDocuments(0);
    response = delete("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Should have deleted one Saksmappe and two Journalposts
    var deletedDocuments = captureDeletedDocuments(3);
    assertTrue(deletedDocuments.contains(saksmappeDTO.getId()));
    assertTrue(deletedDocuments.contains(journalpost1DTO.getId()));
    assertTrue(deletedDocuments.contains(journalpost2DTO.getId()));
  }

  @Test
  void testSaksmappeWithAdmEnhet() throws Exception {
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("administrativEnhet", "UNDER");
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    var journalenhetDTO = gson.fromJson(get("/enhet/" + journalenhetId).getBody(), EnhetDTO.class);
    var underenhetDTO = gson.fromJson(get("/enhet/" + underenhetId).getBody(), EnhetDTO.class);

    // Should have indexed one Saksmappe
    var documentMap = captureIndexedDocuments(1);
    resetEs();
    var saksmappeES = (SaksmappeES) documentMap.get(saksmappeDTO.getId());
    compareSaksmappe(saksmappeDTO, saksmappeES);

    assertEquals(
        List.of(underenhetDTO.getExternalId(), journalenhetDTO.getExternalId(), rootEnhetIri),
        saksmappeES.getArkivskaperTransitive());
    assertEquals(
        List.of(underenhetDTO.getNavn(), journalenhetDTO.getNavn(), rootEnhetNavn),
        saksmappeES.getArkivskaperNavn());

    // Clean up
    response = delete("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNull(saksmappeRepository.findById(saksmappeDTO.getId()).orElse(null));

    // Should have deleted one Saksmappe
    var deletedDocuments = captureDeletedDocuments(1);
    assertTrue(deletedDocuments.contains(saksmappeDTO.getId()));
  }
}
