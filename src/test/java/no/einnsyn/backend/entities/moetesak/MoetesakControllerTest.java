package no.einnsyn.backend.entities.moetesak;

import static no.einnsyn.backend.testutils.Assertions.assertEqualInstants;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.reflect.TypeToken;
import java.time.ZonedDateTime;
import java.util.List;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.backend.utils.SlugGenerator;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MoetesakControllerTest extends EinnsynControllerTestBase {

  ArkivDTO arkivDTO;
  ArkivdelDTO arkivdelDTO;
  MoetemappeDTO moetemappeDTO;

  @BeforeEach
  public void setUp() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkivDTO.getId());

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", getMoetemappeJSON());
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertNotNull(moetemappeDTO.getId());
  }

  @AfterEach
  public void tearDown() throws Exception {
    var result = delete("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.OK, result.getStatusCode());

    result = delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.OK, result.getStatusCode());
  }

  @Test
  void testMoetesakLifecycle() throws Exception {
    var moetesakJSON = getMoetesakJSON();
    moetesakJSON.put("offentligTittel", "unique offentlig tittel for slug");
    var result = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesakJSON);
    var moetesakDTO = gson.fromJson(result.getBody(), MoetesakDTO.class);
    var moetesakId = moetesakDTO.getId();
    var legacyReferanseTilMoetesak = moetesakDTO.getLegacyReferanseTilMoetesak();
    assertNotNull(moetesakId);

    // Verify that slug was generated
    var moetesakEntity = moetesakRepository.findById(moetesakId).orElse(null);
    assertNotNull(moetesakEntity);
    assertNotNull(moetesakEntity.getSlug(), "Slug should be generated for moetesak");
    var expectedSlug =
        SlugGenerator.generate(
            moetesakDTO.getMoetesaksaar()
                + "-"
                + moetesakDTO.getMoetesakssekvensnummer()
                + "-"
                + moetesakDTO.getOffentligTittel(),
            false);
    assertEquals(expectedSlug, moetesakEntity.getSlug(), "Slug should be correctly generated");
    assertNotNull(moetesakDTO.getSlug(), "Slug should be present in DTO");
    assertEquals(expectedSlug, moetesakDTO.getSlug(), "Slug in DTO should match expected slug");

    // Verify that we can get by id
    result = get("/moetesak/" + moetesakDTO.getId());
    moetesakDTO = gson.fromJson(result.getBody(), MoetesakDTO.class);
    assertEquals(moetesakId, moetesakDTO.getId());
    assertEquals(legacyReferanseTilMoetesak, moetesakDTO.getLegacyReferanseTilMoetesak());

    // Verify that we can get by slug
    result = get("/moetesak/" + expectedSlug);
    moetesakDTO = gson.fromJson(result.getBody(), MoetesakDTO.class);

    var moetesakUpdateJSON = getMoetesakJSON();
    var accessibleAfter = ZonedDateTime.now().plusDays(2);
    moetesakUpdateJSON.put("offentligTittel", "updatedOffentligTittel");
    moetesakUpdateJSON.put("accessibleAfter", accessibleAfter.toString());
    moetesakUpdateJSON.put("legacyReferanseTilMoetesak", "http://updatedReferanseTilMoetesak");
    result = patch("/moetesak/" + moetesakDTO.getId(), moetesakUpdateJSON);
    result = getAnon("/moetesak/" + moetesakDTO.getId());
    assertEquals(HttpStatusCode.valueOf(404), result.getStatusCode());

    result = getAdmin("/moetesak/" + moetesakDTO.getId());
    moetesakDTO = gson.fromJson(result.getBody(), MoetesakDTO.class);
    assertEqualInstants(accessibleAfter.toString(), moetesakDTO.getAccessibleAfter());
    assertNotNull(moetesakDTO.getId());
    assertNotNull(moetesakDTO.getMoetemappe());
    assertEquals("http://updatedReferanseTilMoetesak", moetesakDTO.getLegacyReferanseTilMoetesak());
    assertEquals("updatedOffentligTittel", moetesakDTO.getOffentligTittel());
    assertEquals(
        moetesakJSON.get("offentligTittelSensitiv"), moetesakDTO.getOffentligTittelSensitiv());
    assertEquals(moetesakJSON.get("moetesakstype"), moetesakDTO.getMoetesakstype());
    assertEquals(moetesakJSON.get("moetesaksaar"), moetesakDTO.getMoetesaksaar());
    assertEquals(
        moetesakJSON.get("moetesakssekvensnummer"), moetesakDTO.getMoetesakssekvensnummer());
    assertEquals(moetemappeDTO.getUtvalg(), moetesakDTO.getUtvalg());
    assertEquals(moetesakJSON.get("videoLink"), moetesakDTO.getVideoLink());

    result = deleteAdmin("/moetesak/" + moetesakDTO.getId());
    moetesakDTO = gson.fromJson(result.getBody(), MoetesakDTO.class);
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(Boolean.TRUE, moetesakDTO.getDeleted());

    result = get("/moetesak/" + moetesakDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
  }

  @Test
  void testMoetesakTilBehandling() throws Exception {
    var moetesakJSON = getMoetesakJSON();
    var result = post("/moetesak", moetesakJSON);
    var moetesakDTO = gson.fromJson(result.getBody(), MoetesakDTO.class);
    var moetesakId = moetesakDTO.getId();
    assertNotNull(moetesakId);
    assertEquals(moetesakJSON.get("utvalg"), moetesakDTO.getUtvalg());
    assertEquals(journalenhetId, moetesakDTO.getUtvalgObjekt().getId());

    // Add to moetemappe
    moetesakJSON.put("moetemappe", moetemappeDTO.getId());
    result = patch("/moetesak/" + moetesakDTO.getId(), moetesakJSON);
    assertEquals(HttpStatus.OK, result.getStatusCode());
    moetesakDTO = gson.fromJson(result.getBody(), MoetesakDTO.class);
    assertEquals(moetemappeDTO.getId(), moetesakDTO.getMoetemappe().getId());
    assertEquals(moetemappeDTO.getUtvalg(), moetesakDTO.getUtvalg());
    assertEquals(moetemappeDTO.getUtvalgObjekt().getId(), moetesakDTO.getUtvalgObjekt().getId());

    // Delete
    result = delete("/moetesak/" + moetesakDTO.getId());
    assertEquals(HttpStatus.OK, result.getStatusCode());
  }

  @Test
  void testMoetesakWithChildren() throws Exception {
    var moetesakJSON = getMoetesakJSON();
    var result = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesakJSON);
    var moetesakDTO = gson.fromJson(result.getBody(), MoetesakDTO.class);
    var moetesakId = moetesakDTO.getId();
    assertNotNull(moetesakId);

    var utredningDTO = moetesakDTO.getUtredning().getExpandedObject();
    assertNotNull(utredningDTO.getId());
    assertEquals("Utredning", utredningDTO.getEntity());

    var utredningSaksbeskrivelseDTO = utredningDTO.getSaksbeskrivelse().getExpandedObject();
    assertNotNull(utredningSaksbeskrivelseDTO.getId());
    assertEquals("Moetesaksbeskrivelse", utredningSaksbeskrivelseDTO.getEntity());

    var utredningInnstilling = utredningDTO.getInnstilling().getExpandedObject();
    assertNotNull(utredningInnstilling.getId());
    assertEquals("Moetesaksbeskrivelse", utredningInnstilling.getEntity());

    var utredningsdokument = utredningDTO.getUtredningsdokument();
    assertEquals(2, utredningsdokument.size());
    for (var utredningsdokumentField : utredningsdokument) {
      var utredningsdokumentDTO = utredningsdokumentField.getExpandedObject();
      assertNotNull(utredningsdokumentDTO.getId());
      assertEquals("Dokumentbeskrivelse", utredningsdokumentDTO.getEntity());
    }

    var innstilling = moetesakDTO.getInnstilling().getExpandedObject();
    assertNotNull(innstilling.getId());
    assertEquals("Moetesaksbeskrivelse", innstilling.getEntity());

    var vedtakDTO = moetesakDTO.getVedtak().getExpandedObject();
    assertNotNull(vedtakDTO.getId());
    assertEquals("Vedtak", vedtakDTO.getEntity());

    result = get("/moetesak/" + moetesakDTO.getId());
    moetesakDTO = gson.fromJson(result.getBody(), MoetesakDTO.class);
    assertEquals(moetesakId, moetesakDTO.getId());

    result = delete("/moetesak/" + moetesakDTO.getId());
    assertEquals(HttpStatus.OK, result.getStatusCode());

    result = get("/moetesak/" + moetesakDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());

    result = get("/utredning/" + utredningDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());

    result = get("/moetesaksbeskrivelse/" + utredningSaksbeskrivelseDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());

    result = get("/moetesaksbeskrivelse/" + utredningInnstilling.getId());
    assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());

    result = get("/moetesaksbeskrivelse/" + innstilling.getId());
    assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());

    result = get("/vedtak/" + vedtakDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());

    for (var utredningsdokumentField : utredningsdokument) {
      var utredningsdokumentDTO = utredningsdokumentField.getExpandedObject();
      result = get("/dokumentbeskrivelse/" + utredningsdokumentDTO.getId());
      assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }
  }

  @Test
  void testDokumentbeskrivelse() throws Exception {
    var response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", getMoetesakJSON());
    var moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);
    var moetesakId = moetesakDTO.getId();

    response =
        post("/moetesak/" + moetesakId + "/dokumentbeskrivelse", getDokumentbeskrivelseJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var dokumentbeskrivelse1DTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    assertNotNull(dokumentbeskrivelse1DTO.getId());

    response =
        post("/moetesak/" + moetesakId + "/dokumentbeskrivelse", getDokumentbeskrivelseJSON());
    var dokumentbeskrivelse2DTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    assertNotNull(dokumentbeskrivelse2DTO.getId());

    response =
        post("/moetesak/" + moetesakId + "/dokumentbeskrivelse", getDokumentbeskrivelseJSON());
    var dokumentbeskrivelse3DTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    assertNotNull(dokumentbeskrivelse3DTO.getId());

    // Add another unrelated one, to make sure we filter by moetesakId
    response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", getMoetesakJSON());
    var unrelatedMoetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);
    response =
        post(
            "/moetesak/" + unrelatedMoetesakDTO.getId() + "/dokumentbeskrivelse",
            getDokumentbeskrivelseJSON());
    var unrelatedDokumentbeskrivelseDTO =
        gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);

    // DESC
    var type = new TypeToken<PaginatedList<DokumentbeskrivelseDTO>>() {}.getType();
    response = get("/moetesak/" + moetesakId + "/dokumentbeskrivelse");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<DokumentbeskrivelseDTO> dokumentbeskrivelseList =
        gson.fromJson(response.getBody(), type);
    assertEquals(3, dokumentbeskrivelseList.getItems().size());
    assertEquals(
        dokumentbeskrivelse1DTO.getId(), dokumentbeskrivelseList.getItems().get(2).getId());
    assertEquals(
        dokumentbeskrivelse2DTO.getId(), dokumentbeskrivelseList.getItems().get(1).getId());
    assertEquals(
        dokumentbeskrivelse3DTO.getId(), dokumentbeskrivelseList.getItems().get(0).getId());

    // DESC startingAfter
    response =
        get(
            "/moetesak/"
                + moetesakId
                + "/dokumentbeskrivelse?startingAfter="
                + dokumentbeskrivelse2DTO.getId());
    dokumentbeskrivelseList = gson.fromJson(response.getBody(), type);
    assertEquals(1, dokumentbeskrivelseList.getItems().size());
    assertEquals(
        dokumentbeskrivelse1DTO.getId(), dokumentbeskrivelseList.getItems().get(0).getId());

    // DESC endingBefore
    response =
        get(
            "/moetesak/"
                + moetesakId
                + "/dokumentbeskrivelse?endingBefore="
                + dokumentbeskrivelse2DTO.getId());
    dokumentbeskrivelseList = gson.fromJson(response.getBody(), type);
    assertEquals(1, dokumentbeskrivelseList.getItems().size());
    assertEquals(
        dokumentbeskrivelse3DTO.getId(), dokumentbeskrivelseList.getItems().get(0).getId());

    // ASC
    response = get("/moetesak/" + moetesakId + "/dokumentbeskrivelse?sortOrder=asc");
    dokumentbeskrivelseList = gson.fromJson(response.getBody(), type);
    assertEquals(3, dokumentbeskrivelseList.getItems().size());
    assertEquals(
        dokumentbeskrivelse1DTO.getId(), dokumentbeskrivelseList.getItems().get(0).getId());
    assertEquals(
        dokumentbeskrivelse2DTO.getId(), dokumentbeskrivelseList.getItems().get(1).getId());
    assertEquals(
        dokumentbeskrivelse3DTO.getId(), dokumentbeskrivelseList.getItems().get(2).getId());

    // ASC startingAfter
    response =
        get(
            "/moetesak/"
                + moetesakId
                + "/dokumentbeskrivelse?startingAfter="
                + dokumentbeskrivelse2DTO.getId()
                + "&sortOrder=asc");
    dokumentbeskrivelseList = gson.fromJson(response.getBody(), type);
    assertEquals(1, dokumentbeskrivelseList.getItems().size());
    assertEquals(
        dokumentbeskrivelse3DTO.getId(), dokumentbeskrivelseList.getItems().get(0).getId());

    // ASC endingBefore
    response =
        get(
            "/moetesak/"
                + moetesakId
                + "/dokumentbeskrivelse?endingBefore="
                + dokumentbeskrivelse2DTO.getId()
                + "&sortOrder=asc");
    dokumentbeskrivelseList = gson.fromJson(response.getBody(), type);
    assertEquals(1, dokumentbeskrivelseList.getItems().size());
    assertEquals(
        dokumentbeskrivelse1DTO.getId(), dokumentbeskrivelseList.getItems().get(0).getId());

    response = delete("/moetesak/" + moetesakId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetesak/" + moetesakId).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND,
        get("/dokumentbeskrivelse/" + dokumentbeskrivelse1DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND,
        get("/dokumentbeskrivelse/" + dokumentbeskrivelse2DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND,
        get("/dokumentbeskrivelse/" + dokumentbeskrivelse3DTO.getId()).getStatusCode());

    response = delete("/moetesak/" + unrelatedMoetesakDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/moetesak/" + unrelatedMoetesakDTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND,
        get("/dokumentbeskrivelse/" + unrelatedDokumentbeskrivelseDTO.getId()).getStatusCode());
  }

  // Test orphan deletion
  @Test
  void testDokumentbeskrivelseDeletion() throws Exception {
    var response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", getMoetesakJSON());
    var moetesak1DTO = gson.fromJson(response.getBody(), MoetesakDTO.class);

    // Add a dokumentbeskrivelse
    response =
        post(
            "/moetesak/" + moetesak1DTO.getId() + "/dokumentbeskrivelse",
            getDokumentbeskrivelseJSON());
    var dokumentbeskrivelseDTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);

    // Add another moetesak with the same dokumentbeskrivelse
    var moetesak2JSON = getMoetesakJSON();
    moetesak2JSON.put(
        "dokumentbeskrivelse", new JSONArray(List.of(dokumentbeskrivelseDTO.getId())));
    response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesak2JSON);
    var moetesak2DTO = gson.fromJson(response.getBody(), MoetesakDTO.class);

    // Make sure both have the same dokumentbeskrivelse
    response = get("/moetesak/" + moetesak1DTO.getId() + "/dokumentbeskrivelse");
    PaginatedList<DokumentbeskrivelseDTO> dokumentbeskrivelseList =
        gson.fromJson(
            response.getBody(),
            new TypeToken<PaginatedList<DokumentbeskrivelseDTO>>() {}.getType());
    assertEquals(1, dokumentbeskrivelseList.getItems().size());
    assertEquals(dokumentbeskrivelseDTO.getId(), dokumentbeskrivelseList.getItems().get(0).getId());

    response = get("/moetesak/" + moetesak2DTO.getId() + "/dokumentbeskrivelse");
    dokumentbeskrivelseList =
        gson.fromJson(
            response.getBody(),
            new TypeToken<PaginatedList<DokumentbeskrivelseDTO>>() {}.getType());
    assertEquals(1, dokumentbeskrivelseList.getItems().size());
    assertEquals(dokumentbeskrivelseDTO.getId(), dokumentbeskrivelseList.getItems().get(0).getId());

    // Make sure we can get the dokumentbeskrivelse
    response = get("/dokumentbeskrivelse/" + dokumentbeskrivelseDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Delete the first moetesak
    response = delete("/moetesak/" + moetesak1DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Make sure the moetesak is deleted
    response = get("/moetesak/" + moetesak1DTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    // Make sure the dokumentbeskrivelse is still there
    response = get("/dokumentbeskrivelse/" + dokumentbeskrivelseDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Delete the second moetesak
    response = delete("/moetesak/" + moetesak2DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Make sure the moetesak is deleted
    response = get("/moetesak/" + moetesak2DTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    // Make sure the dokumentbeskrivelse is deleted
    response = get("/dokumentbeskrivelse/" + dokumentbeskrivelseDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void addExistingDokumentbeskrivelse() throws Exception {

    // Add moetesak1
    var response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", getMoetesakJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetesak1DTO = gson.fromJson(response.getBody(), MoetesakDTO.class);

    // Add moetesak2
    response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", getMoetesakJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetesak2DTO = gson.fromJson(response.getBody(), MoetesakDTO.class);

    // Add dokumentbeskrivelse
    response =
        post(
            "/moetesak/" + moetesak1DTO.getId() + "/dokumentbeskrivelse",
            getDokumentbeskrivelseJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var dokumentbeskrivelseDTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    var dokumentbeskrivelseId = dokumentbeskrivelseDTO.getId();

    // Add the same dokumentbeskrivelse to moetesak2
    response =
        post("/moetesak/" + moetesak2DTO.getId() + "/dokumentbeskrivelse", dokumentbeskrivelseId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    dokumentbeskrivelseDTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    assertEquals(dokumentbeskrivelseId, dokumentbeskrivelseDTO.getId());

    // Make sure the dokumentbeskrivelse is in both moetesak1 and moetesak2
    response = get("/moetesak/" + moetesak1DTO.getId() + "/dokumentbeskrivelse");
    PaginatedList<DokumentbeskrivelseDTO> dokumentbeskrivelseList =
        gson.fromJson(
            response.getBody(),
            new TypeToken<PaginatedList<DokumentbeskrivelseDTO>>() {}.getType());
    assertEquals(1, dokumentbeskrivelseList.getItems().size());
    assertEquals(dokumentbeskrivelseId, dokumentbeskrivelseList.getItems().getFirst().getId());

    response = get("/moetesak/" + moetesak2DTO.getId() + "/dokumentbeskrivelse");
    dokumentbeskrivelseList =
        gson.fromJson(
            response.getBody(),
            new TypeToken<PaginatedList<DokumentbeskrivelseDTO>>() {}.getType());
    assertEquals(1, dokumentbeskrivelseList.getItems().size());
    assertEquals(dokumentbeskrivelseId, dokumentbeskrivelseList.getItems().getFirst().getId());

    // Delete moetesak1, the dokumentbeskrivelse should still be in moetesak2
    response = delete("/moetesak/" + moetesak1DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = get("/moetesak/" + moetesak1DTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    response = get("/dokumentbeskrivelse/" + dokumentbeskrivelseId);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Delete moetesak2, the dokumentbeskrivelse should be deleted
    response = delete("/moetesak/" + moetesak2DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = get("/moetesak/" + moetesak2DTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    response = get("/dokumentbeskrivelse/" + dokumentbeskrivelseId);
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void checkLegacyArkivskaperFromJournalenhet() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", getMoetesakJSON());
    var moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var journalenhet = enhetRepository.findById(journalenhetId).orElse(null);
    var moetesak = moetesakRepository.findById(moetesakDTO.getId()).orElse(null);
    assertEquals(journalenhet.getIri(), moetesak.getArkivskaper());

    delete("/moetemappe/" + moetemappeDTO.getId());
    assertNull(moetesakRepository.findById(moetesakDTO.getId()).orElse(null));
  }

  @Test
  void checkLegacyArkivskaperFromAdmEnhet() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    var moetesakJSON = getMoetesakJSON();
    moetesakJSON.put("utvalg", "UNDER");
    response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesakJSON);
    var moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var journalenhet = enhetRepository.findById(underenhetId).orElse(null);
    var moetesak = moetesakRepository.findById(moetesakDTO.getId()).orElse(null);
    assertEquals(journalenhet.getIri(), moetesak.getArkivskaper());

    delete("/moetemappe/" + moetemappeDTO.getId());
    assertNull(moetesakRepository.findById(moetesakDTO.getId()).orElse(null));
  }

  @Test
  void testDeleteDokumentbeskrivelseFromMoetesak() throws Exception {
    var listType = new TypeToken<PaginatedList<DokumentbeskrivelseDTO>>() {}.getType();

    // Add two Moetesak
    var response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", getMoetesakJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetesak1DTO = gson.fromJson(response.getBody(), MoetesakDTO.class);
    response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", getMoetesakJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetesak2DTO = gson.fromJson(response.getBody(), MoetesakDTO.class);

    // Add a dokumentbeskrivelse to the first moetesak
    response =
        post(
            "/moetesak/" + moetesak1DTO.getId() + "/dokumentbeskrivelse",
            getDokumentbeskrivelseJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var dokumentbeskrivelse1DTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    assertNotNull(dokumentbeskrivelse1DTO.getId());

    // Add the same dokumentbeskrivelse to the second moetesak
    response =
        post(
            "/moetesak/" + moetesak2DTO.getId() + "/dokumentbeskrivelse",
            dokumentbeskrivelse1DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var dokumentbeskrivelse2DTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    assertEquals(dokumentbeskrivelse1DTO.getId(), dokumentbeskrivelse2DTO.getId());

    // Delete the dokumentbeskrivelse from the first moetesak
    response =
        delete(
            "/moetesak/"
                + moetesak1DTO.getId()
                + "/dokumentbeskrivelse/"
                + dokumentbeskrivelse1DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Make sure the dokumentbeskrivelse still exists
    response = get("/dokumentbeskrivelse/" + dokumentbeskrivelse1DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Make sure it is removed from the first moetesak
    response = get("/moetesak/" + moetesak1DTO.getId() + "/dokumentbeskrivelse");
    PaginatedList<DokumentbeskrivelseDTO> dokumentbeskrivelseList =
        gson.fromJson(response.getBody(), listType);
    assertEquals(0, dokumentbeskrivelseList.getItems().size());

    // Make sure it is still in the second moetesak
    response = get("/moetesak/" + moetesak2DTO.getId() + "/dokumentbeskrivelse");
    dokumentbeskrivelseList = gson.fromJson(response.getBody(), listType);
    assertEquals(1, dokumentbeskrivelseList.getItems().size());
    assertEquals(
        dokumentbeskrivelse1DTO.getId(), dokumentbeskrivelseList.getItems().get(0).getId());

    // Delete the second moetesak
    response = delete("/moetesak/" + moetesak2DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetesak/" + moetesak2DTO.getId()).getStatusCode());

    // Make sure the dokumentbeskrivelse is deleted
    response = get("/dokumentbeskrivelse/" + dokumentbeskrivelse1DTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    // Clean up
    response = delete("/moetesak/" + moetesak1DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testMoetesakstype() throws Exception {
    // Test all moetesakstype mappings
    for (var testData : provideMoetesakstypeTestData()) {
      var expectedType = (MoetesakDTO.MoetesakstypeEnum) testData.get(0);
      var legacyType = (String) testData.get(1);

      testMoetesakstypeMapping(expectedType, legacyType);
    }
  }

  private void testMoetesakstypeMapping(
      MoetesakDTO.MoetesakstypeEnum expectedType, String legacyType) throws Exception {
    var moetesakJSON = getMoetesakJSON();
    moetesakJSON.remove("moetesakstype");
    moetesakJSON.remove("utredning");
    moetesakJSON.remove("vedtak");
    moetesakJSON.remove("innstilling");
    moetesakJSON.put("moetesakstype", "annet");
    moetesakJSON.put("legacyMoetesakstype", legacyType);
    var response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesakJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);
    assertEquals(
        expectedType.toString(),
        moetesakDTO.getMoetesakstype(),
        "Expected moetesakstype " + expectedType.toString() + " for legacy type: " + legacyType);
    delete("/moetesak/" + moetesakDTO.getId());
  }

  private static List<List<Object>> provideMoetesakstypeTestData() {
    return List.of(
        // MOETE
        List.of(
            MoetesakDTO.MoetesakstypeEnum.MOETE,
            "http://www.arkivverket.no/standarder/noark5/arkivstruktur/møtesakstype_annet"),
        List.of(
            MoetesakDTO.MoetesakstypeEnum.MOETE,
            "http://møtesakstype.976819853.no/7/Notater+og+orienteringer+fra+byr%c3%a5det%2c+henvendelser+til+utvalget"),
        List.of(MoetesakDTO.MoetesakstypeEnum.MOETE, "moetesakstypeannet"),
        List.of(MoetesakDTO.MoetesakstypeEnum.MOETE, "Notater og orienteringer"),

        // POLITISK
        List.of(
            MoetesakDTO.MoetesakstypeEnum.POLITISK,
            "http://www.arkivverket.no/standarder/noark5/arkivstruktur/politiskSak"),
        List.of(
            MoetesakDTO.MoetesakstypeEnum.POLITISK,
            "http://møtesakstype.976819837.no/5/Byr%c3%a5dssak"),
        List.of(
            MoetesakDTO.MoetesakstypeEnum.POLITISK, "http://møtesakstype.976819837.no/5/Byrådssak"),
        List.of(
            MoetesakDTO.MoetesakstypeEnum.POLITISK,
            "http://møtesakstype.976819853.no/3/Politisk+sak"),
        List.of(
            MoetesakDTO.MoetesakstypeEnum.POLITISK,
            "http://møtesakstype.976819853.no/5/Faste+saker"),
        List.of(
            MoetesakDTO.MoetesakstypeEnum.POLITISK,
            "http://møtesakstype.958935420.no/4/Klagenemnda"),
        List.of(MoetesakDTO.MoetesakstypeEnum.POLITISK, "politisk sak"),
        List.of(MoetesakDTO.MoetesakstypeEnum.POLITISK, "politisksak"),
        List.of(MoetesakDTO.MoetesakstypeEnum.POLITISK, "Byrådssaker og faste saker"),

        // DELEGERT
        List.of(
            MoetesakDTO.MoetesakstypeEnum.DELEGERT,
            "http://www.arkivverket.no/standarder/noark5/arkivstruktur/delegertSak"),
        List.of(
            MoetesakDTO.MoetesakstypeEnum.DELEGERT,
            "http://møtesakstype.974778807.no/2/Administrativ+sak"),
        List.of(
            MoetesakDTO.MoetesakstypeEnum.DELEGERT,
            "http://møtesakstype.976819853.no/7/Delegert+sak+med+vedtak"),
        List.of(MoetesakDTO.MoetesakstypeEnum.DELEGERT, "delegert sak"),
        List.of(MoetesakDTO.MoetesakstypeEnum.DELEGERT, "delegert"),

        // INTERPELLASJON
        List.of(
            MoetesakDTO.MoetesakstypeEnum.INTERPELLASJON,
            "http://www.arkivverket.no/standarder/noark5/arkivstruktur/interpellasjon"),
        List.of(MoetesakDTO.MoetesakstypeEnum.INTERPELLASJON, "interpellasjon"),

        // GODKJENNING
        List.of(
            MoetesakDTO.MoetesakstypeEnum.GODKJENNING,
            "http://www.arkivverket.no/standarder/noark5/arkivstruktur/godkjenning"),
        List.of(MoetesakDTO.MoetesakstypeEnum.GODKJENNING, "godkjenning"),

        // REFERAT
        List.of(
            MoetesakDTO.MoetesakstypeEnum.REFERAT,
            "http://www.arkivverket.no/standarder/noark5/arkivstruktur/referat"),
        List.of(
            MoetesakDTO.MoetesakstypeEnum.REFERAT,
            "http://www.arkivverket.no/standarder/noark5/arkivstruktur/referatsak"),
        List.of(
            MoetesakDTO.MoetesakstypeEnum.REFERAT,
            "http://møtesakstype.958935420.no/4/Referatsaker"),
        List.of(MoetesakDTO.MoetesakstypeEnum.REFERAT, "referat"),

        // ORIENTERING
        List.of(
            MoetesakDTO.MoetesakstypeEnum.ORIENTERING,
            "http://www.arkivverket.no/standarder/noark5/arkivstruktur/orienteringssak"),
        List.of(MoetesakDTO.MoetesakstypeEnum.ORIENTERING, "orientering"),

        // ANNET (default fallback)
        List.of(MoetesakDTO.MoetesakstypeEnum.ANNET, "unknown_type"),
        List.of(MoetesakDTO.MoetesakstypeEnum.ANNET, "ukjent"));
  }
}
