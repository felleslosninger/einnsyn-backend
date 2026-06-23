package no.einnsyn.backend.entities.matrikkelnummer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.matrikkelnummer.models.MatrikkelnummerDTO;
import no.einnsyn.backend.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MatrikkelnummerTest extends EinnsynControllerTestBase {

  @Autowired private MatrikkelnummerRepository matrikkelnummerRepository;

  private ArkivDTO arkivDTO;
  private ArkivdelDTO arkivdelDTO;

  @BeforeAll
  void setup() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
  }

  @AfterAll
  void teardown() throws Exception {
    delete("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void createMatrikkelnummerForSaksmappe() throws Exception {
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put(
        "matrikkelnummer",
        new JSONArray()
            .put(getMatrikkelnummerJSON("0301", 1, 42))
            .put(getMatrikkelnummerJSON("0301", 1, 43)));

    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var matrikkelnummerDTO = assertMatrikkelnummerReference(saksmappeDTO.getMatrikkelnummer(), 0);
    var matrikkelnummer2DTO = assertMatrikkelnummerReference(saksmappeDTO.getMatrikkelnummer(), 1);

    var matrikkelnummer =
        matrikkelnummerRepository.findByIdWithParents(matrikkelnummerDTO.getId()).orElseThrow();
    assertEquals(saksmappeDTO.getId(), matrikkelnummer.getSaksmappe().getId());
    assertNull(matrikkelnummer.getMoetemappe());
    assertNull(matrikkelnummer.getJournalpost());
    assertNull(matrikkelnummer.getMoetesak());
    assertNull(matrikkelnummer.getMoetedokument());

    var matrikkelnummer2 =
        matrikkelnummerRepository.findByIdWithParents(matrikkelnummer2DTO.getId()).orElseThrow();
    assertEquals(saksmappeDTO.getId(), matrikkelnummer2.getSaksmappe().getId());

    response = get("/saksmappe/" + saksmappeDTO.getId() + "?expand=matrikkelnummer");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    matrikkelnummerDTO = saksmappeDTO.getMatrikkelnummer().getFirst().getExpandedObject();
    assertMatrikkelnummerValues(matrikkelnummerDTO, "0301", 1, 42);
    matrikkelnummer2DTO = saksmappeDTO.getMatrikkelnummer().get(1).getExpandedObject();
    assertMatrikkelnummerValues(matrikkelnummer2DTO, "0301", 1, 43);

    assertEquals(HttpStatus.OK, delete("/saksmappe/" + saksmappeDTO.getId()).getStatusCode());
    assertTrue(matrikkelnummerRepository.findById(matrikkelnummerDTO.getId()).isEmpty());
    assertTrue(matrikkelnummerRepository.findById(matrikkelnummer2DTO.getId()).isEmpty());
  }

  @Test
  void createMatrikkelnummerForJournalpost() throws Exception {
    var saksmappeResponse =
        post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappeDTO = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);

    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put(
        "matrikkelnummer", new JSONArray().put(getMatrikkelnummerJSON("0301", 2, 7)));

    var response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    var matrikkelnummerDTO = assertMatrikkelnummerReference(journalpostDTO.getMatrikkelnummer());

    var matrikkelnummer =
        matrikkelnummerRepository.findByIdWithParents(matrikkelnummerDTO.getId()).orElseThrow();
    assertNull(matrikkelnummer.getSaksmappe());
    assertNull(matrikkelnummer.getMoetemappe());
    assertEquals(journalpostDTO.getId(), matrikkelnummer.getJournalpost().getId());
    assertNull(matrikkelnummer.getMoetesak());
    assertNull(matrikkelnummer.getMoetedokument());

    response = get("/journalpost/" + journalpostDTO.getId() + "?expand=matrikkelnummer");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    matrikkelnummerDTO = journalpostDTO.getMatrikkelnummer().getFirst().getExpandedObject();
    assertMatrikkelnummerValues(matrikkelnummerDTO, "0301", 2, 7);

    assertEquals(HttpStatus.OK, delete("/saksmappe/" + saksmappeDTO.getId()).getStatusCode());
    assertTrue(matrikkelnummerRepository.findById(matrikkelnummerDTO.getId()).isEmpty());
  }

  @Test
  void createMatrikkelnummerForMoetemappeAndMoetesak() throws Exception {
    var moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.remove("moetedokument");
    moetemappeJSON.remove("moetesak");
    moetemappeJSON.put(
        "matrikkelnummer", new JSONArray().put(getMatrikkelnummerJSON("0301", 3, 9)));

    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    var moetemappeMatrikkelDTO = assertMatrikkelnummerReference(moetemappeDTO.getMatrikkelnummer());
    var moetemappeMatrikkel =
        matrikkelnummerRepository.findByIdWithParents(moetemappeMatrikkelDTO.getId()).orElseThrow();
    assertNull(moetemappeMatrikkel.getSaksmappe());
    assertEquals(moetemappeDTO.getId(), moetemappeMatrikkel.getMoetemappe().getId());

    var moetesakJSON = getMoetesakJSON();
    moetesakJSON.put("matrikkelnummer", new JSONArray().put(getMatrikkelnummerJSON("0301", 4, 11)));

    response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesakJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);
    var moetesakMatrikkelDTO = assertMatrikkelnummerReference(moetesakDTO.getMatrikkelnummer());
    var moetesakMatrikkel =
        matrikkelnummerRepository.findByIdWithParents(moetesakMatrikkelDTO.getId()).orElseThrow();
    assertNull(moetesakMatrikkel.getSaksmappe());
    assertNull(moetesakMatrikkel.getMoetemappe());
    assertEquals(moetesakDTO.getId(), moetesakMatrikkel.getMoetesak().getId());

    assertEquals(HttpStatus.OK, delete("/moetesak/" + moetesakDTO.getId()).getStatusCode());
    assertTrue(matrikkelnummerRepository.findById(moetesakMatrikkelDTO.getId()).isEmpty());
    assertEquals(HttpStatus.OK, delete("/moetemappe/" + moetemappeDTO.getId()).getStatusCode());
    assertTrue(matrikkelnummerRepository.findById(moetemappeMatrikkelDTO.getId()).isEmpty());
  }

  @Test
  void createMatrikkelnummerForMoetedokument() throws Exception {
    var moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.remove("moetedokument");
    moetemappeJSON.remove("moetesak");

    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    var moetedokumentJSON = getMoetedokumentJSON();
    moetedokumentJSON.put(
        "matrikkelnummer", new JSONArray().put(getMatrikkelnummerJSON("0301", 5, 13)));

    response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetedokument", moetedokumentJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var moetedokumentDTO = gson.fromJson(response.getBody(), MoetedokumentDTO.class);
    var matrikkelnummerDTO = assertMatrikkelnummerReference(moetedokumentDTO.getMatrikkelnummer());

    var matrikkelnummer =
        matrikkelnummerRepository.findByIdWithParents(matrikkelnummerDTO.getId()).orElseThrow();
    assertNull(matrikkelnummer.getSaksmappe());
    assertNull(matrikkelnummer.getMoetemappe());
    assertNull(matrikkelnummer.getJournalpost());
    assertNull(matrikkelnummer.getMoetesak());
    assertEquals(moetedokumentDTO.getId(), matrikkelnummer.getMoetedokument().getId());

    response = get("/moetedokument/" + moetedokumentDTO.getId() + "?expand=matrikkelnummer");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    moetedokumentDTO = gson.fromJson(response.getBody(), MoetedokumentDTO.class);
    matrikkelnummerDTO = moetedokumentDTO.getMatrikkelnummer().getFirst().getExpandedObject();
    assertMatrikkelnummerValues(matrikkelnummerDTO, "0301", 5, 13);

    assertEquals(
        HttpStatus.OK, delete("/moetedokument/" + moetedokumentDTO.getId()).getStatusCode());
    assertTrue(matrikkelnummerRepository.findById(matrikkelnummerDTO.getId()).isEmpty());
    assertEquals(HttpStatus.OK, delete("/moetemappe/" + moetemappeDTO.getId()).getStatusCode());
  }

  @Test
  void deleteMatrikkelnummerDirectly() throws Exception {
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put(
        "matrikkelnummer", new JSONArray().put(getMatrikkelnummerJSON("0301", 7, 19)));

    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var matrikkelnummerDTO = assertMatrikkelnummerReference(saksmappeDTO.getMatrikkelnummer());

    assertEquals(
        HttpStatus.OK, delete("/matrikkelnummer/" + matrikkelnummerDTO.getId()).getStatusCode());
    assertTrue(matrikkelnummerRepository.findById(matrikkelnummerDTO.getId()).isEmpty());

    assertEquals(HttpStatus.OK, delete("/saksmappe/" + saksmappeDTO.getId()).getStatusCode());
  }

  @Test
  void noDuplicateWhenPatchedWithSameMatrikkelnummer() throws Exception {
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put(
        "matrikkelnummer", new JSONArray().put(getMatrikkelnummerJSON("0301", 8, 21)));

    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var originalId = assertMatrikkelnummerReference(saksmappeDTO.getMatrikkelnummer()).getId();

    // PATCH with the identical matrikkelnummer — should reuse the existing row
    var patchJSON = new JSONObject();
    patchJSON.put("matrikkelnummer", new JSONArray().put(getMatrikkelnummerJSON("0301", 8, 21)));
    response = patch("/saksmappe/" + saksmappeDTO.getId(), patchJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var matrikkelnummerForSaksmappe =
        matrikkelnummerRepository
            .findBySaksmappeAndKommunenummerAndGaardsnummerAndBruksnummerAndFestenummerAndSeksjonsnummer(
                saksmappeService.find(saksmappeDTO.getId()), "0301", 8, 21, 0, 0);
    assertTrue(matrikkelnummerForSaksmappe.isPresent());
    assertEquals(originalId, matrikkelnummerForSaksmappe.get().getId());

    assertEquals(HttpStatus.OK, delete("/saksmappe/" + saksmappeDTO.getId()).getStatusCode());
  }

  @Test
  void rejectMatrikkelnummerIdReferenceOnPatch() throws Exception {
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put(
        "matrikkelnummer", new JSONArray().put(getMatrikkelnummerJSON("0301", 6, 17)));

    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var matrikkelnummerDTO = assertMatrikkelnummerReference(saksmappeDTO.getMatrikkelnummer());

    var patchJSON = new JSONObject();
    patchJSON.put("matrikkelnummer", new JSONArray().put(matrikkelnummerDTO.getId()));

    response = patch("/saksmappe/" + saksmappeDTO.getId(), patchJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    assertEquals(HttpStatus.OK, delete("/saksmappe/" + saksmappeDTO.getId()).getStatusCode());
  }

  @Test
  void addAndListMatrikkelnummerViaSaksmappe() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Add via sub-collection endpoint
    response =
        post(
            "/saksmappe/" + saksmappeDTO.getId() + "/matrikkelnummer",
            getMatrikkelnummerJSON("0301", 20, 50));
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var mnDTO = gson.fromJson(response.getBody(), MatrikkelnummerDTO.class);
    assertMatrikkelnummerValues(mnDTO, "0301", 20, 50);
    assertTrue(matrikkelnummerRepository.findById(mnDTO.getId()).isPresent());

    // Same POST again — idempotent, returns existing
    response =
        post(
            "/saksmappe/" + saksmappeDTO.getId() + "/matrikkelnummer",
            getMatrikkelnummerJSON("0301", 20, 50));
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(
        mnDTO.getId(), gson.fromJson(response.getBody(), MatrikkelnummerDTO.class).getId());

    // List — should contain exactly one entry
    response = get("/saksmappe/" + saksmappeDTO.getId() + "/matrikkelnummer");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var resultType =
        new com.google.gson.reflect.TypeToken<
            no.einnsyn.backend.common.responses.models.PaginatedList<
                MatrikkelnummerDTO>>() {}.getType();
    no.einnsyn.backend.common.responses.models.PaginatedList<MatrikkelnummerDTO> list =
        gson.fromJson(response.getBody(), resultType);
    assertEquals(1, list.getItems().size());
    assertMatrikkelnummerValues(list.getItems().getFirst(), "0301", 20, 50);

    assertEquals(HttpStatus.OK, delete("/saksmappe/" + saksmappeDTO.getId()).getStatusCode());
    assertTrue(matrikkelnummerRepository.findById(mnDTO.getId()).isEmpty());
  }

  @Test
  void addAndListMatrikkelnummerViaJournalpost() throws Exception {
    var saksmappeResponse =
        post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    var saksmappeDTO = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    var journalpostResponse =
        post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", getJournalpostJSON());
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var journalpostDTO = gson.fromJson(journalpostResponse.getBody(), JournalpostDTO.class);

    // Add via sub-collection endpoint
    var response =
        post(
            "/journalpost/" + journalpostDTO.getId() + "/matrikkelnummer",
            getMatrikkelnummerJSON("0301", 21, 51));
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var mnDTO = gson.fromJson(response.getBody(), MatrikkelnummerDTO.class);
    assertMatrikkelnummerValues(mnDTO, "0301", 21, 51);

    // List
    response = get("/journalpost/" + journalpostDTO.getId() + "/matrikkelnummer");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var resultType =
        new com.google.gson.reflect.TypeToken<
            no.einnsyn.backend.common.responses.models.PaginatedList<
                MatrikkelnummerDTO>>() {}.getType();
    no.einnsyn.backend.common.responses.models.PaginatedList<MatrikkelnummerDTO> list =
        gson.fromJson(response.getBody(), resultType);
    assertEquals(1, list.getItems().size());
    assertMatrikkelnummerValues(list.getItems().getFirst(), "0301", 21, 51);

    assertEquals(HttpStatus.OK, delete("/saksmappe/" + saksmappeDTO.getId()).getStatusCode());
    assertTrue(matrikkelnummerRepository.findById(mnDTO.getId()).isEmpty());
  }

  private MatrikkelnummerDTO assertMatrikkelnummerReference(
      List<ExpandableField<MatrikkelnummerDTO>> matrikkelnummer) {
    return assertMatrikkelnummerReference(matrikkelnummer, 0);
  }

  private MatrikkelnummerDTO assertMatrikkelnummerReference(
      List<ExpandableField<MatrikkelnummerDTO>> matrikkelnummer, int index) {
    assertNotNull(matrikkelnummer);
    assertTrue(matrikkelnummer.size() > index);
    var matrikkelnummerDTO = matrikkelnummer.get(index).getExpandedObject();
    if (matrikkelnummerDTO == null) {
      matrikkelnummerDTO = new MatrikkelnummerDTO();
      matrikkelnummerDTO.setId(matrikkelnummer.get(index).getId());
    }
    assertNotNull(matrikkelnummerDTO.getId());
    return matrikkelnummerDTO;
  }

  @Test
  void getAndUpdateMatrikkelnummer() throws Exception {
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put(
        "matrikkelnummer", new JSONArray().put(getMatrikkelnummerJSON("0301", 22, 52)));

    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var matrikkelnummerDTO = assertMatrikkelnummerReference(saksmappeDTO.getMatrikkelnummer());

    // GET directly by ID
    response = get("/matrikkelnummer/" + matrikkelnummerDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var fetchedDTO = gson.fromJson(response.getBody(), MatrikkelnummerDTO.class);
    assertMatrikkelnummerValues(fetchedDTO, "0301", 22, 52);

    // PATCH — update seksjonsnummer
    var patchJSON = new JSONObject();
    patchJSON.put("seksjonsnummer", 3);
    response = patch("/matrikkelnummer/" + matrikkelnummerDTO.getId(), patchJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var updatedDTO = gson.fromJson(response.getBody(), MatrikkelnummerDTO.class);
    assertEquals(3, updatedDTO.getSeksjonsnummer());
    assertEquals("0301", updatedDTO.getKommunenummer());
    assertEquals(22, updatedDTO.getGaardsnummer());

    // GET global list — endpoint must be reachable
    response = get("/matrikkelnummer");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    assertEquals(HttpStatus.OK, delete("/saksmappe/" + saksmappeDTO.getId()).getStatusCode());
    assertTrue(matrikkelnummerRepository.findById(matrikkelnummerDTO.getId()).isEmpty());
  }

  @Test
  void addAndListMatrikkelnummerViaMoetemappe() throws Exception {
    var moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.remove("moetedokument");
    moetemappeJSON.remove("moetesak");

    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    response =
        post(
            "/moetemappe/" + moetemappeDTO.getId() + "/matrikkelnummer",
            getMatrikkelnummerJSON("0301", 23, 53));
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var mnDTO = gson.fromJson(response.getBody(), MatrikkelnummerDTO.class);
    assertMatrikkelnummerValues(mnDTO, "0301", 23, 53);

    // Same POST again — idempotent
    response =
        post(
            "/moetemappe/" + moetemappeDTO.getId() + "/matrikkelnummer",
            getMatrikkelnummerJSON("0301", 23, 53));
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(
        mnDTO.getId(), gson.fromJson(response.getBody(), MatrikkelnummerDTO.class).getId());

    // List
    response = get("/moetemappe/" + moetemappeDTO.getId() + "/matrikkelnummer");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var resultType =
        new com.google.gson.reflect.TypeToken<
            no.einnsyn.backend.common.responses.models.PaginatedList<
                MatrikkelnummerDTO>>() {}.getType();
    no.einnsyn.backend.common.responses.models.PaginatedList<MatrikkelnummerDTO> list =
        gson.fromJson(response.getBody(), resultType);
    assertEquals(1, list.getItems().size());
    assertMatrikkelnummerValues(list.getItems().getFirst(), "0301", 23, 53);

    assertEquals(HttpStatus.OK, delete("/moetemappe/" + moetemappeDTO.getId()).getStatusCode());
    assertTrue(matrikkelnummerRepository.findById(mnDTO.getId()).isEmpty());
  }

  @Test
  void addAndListMatrikkelnummerViaMoetesakAndMoetedokument() throws Exception {
    var moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.remove("moetedokument");
    moetemappeJSON.remove("moetesak");

    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    // Moetesak
    var moetesakResponse =
        post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", getMoetesakJSON());
    assertEquals(HttpStatus.CREATED, moetesakResponse.getStatusCode());
    var moetesakDTO = gson.fromJson(moetesakResponse.getBody(), MoetesakDTO.class);

    response =
        post(
            "/moetesak/" + moetesakDTO.getId() + "/matrikkelnummer",
            getMatrikkelnummerJSON("0301", 24, 54));
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var mnMoetesakDTO = gson.fromJson(response.getBody(), MatrikkelnummerDTO.class);
    assertMatrikkelnummerValues(mnMoetesakDTO, "0301", 24, 54);

    // Same POST again — idempotent
    response =
        post(
            "/moetesak/" + moetesakDTO.getId() + "/matrikkelnummer",
            getMatrikkelnummerJSON("0301", 24, 54));
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(
        mnMoetesakDTO.getId(), gson.fromJson(response.getBody(), MatrikkelnummerDTO.class).getId());

    response = get("/moetesak/" + moetesakDTO.getId() + "/matrikkelnummer");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var resultType =
        new com.google.gson.reflect.TypeToken<
            no.einnsyn.backend.common.responses.models.PaginatedList<
                MatrikkelnummerDTO>>() {}.getType();
    no.einnsyn.backend.common.responses.models.PaginatedList<MatrikkelnummerDTO> list =
        gson.fromJson(response.getBody(), resultType);
    assertEquals(1, list.getItems().size());
    assertMatrikkelnummerValues(list.getItems().getFirst(), "0301", 24, 54);

    // Moetedokument
    var moetedokumentResponse =
        post("/moetemappe/" + moetemappeDTO.getId() + "/moetedokument", getMoetedokumentJSON());
    assertEquals(HttpStatus.CREATED, moetedokumentResponse.getStatusCode());
    var moetedokumentDTO = gson.fromJson(moetedokumentResponse.getBody(), MoetedokumentDTO.class);

    response =
        post(
            "/moetedokument/" + moetedokumentDTO.getId() + "/matrikkelnummer",
            getMatrikkelnummerJSON("0301", 25, 55));
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var mnMoetedokumentDTO = gson.fromJson(response.getBody(), MatrikkelnummerDTO.class);
    assertMatrikkelnummerValues(mnMoetedokumentDTO, "0301", 25, 55);

    response = get("/moetedokument/" + moetedokumentDTO.getId() + "/matrikkelnummer");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    list = gson.fromJson(response.getBody(), resultType);
    assertEquals(1, list.getItems().size());
    assertMatrikkelnummerValues(list.getItems().getFirst(), "0301", 25, 55);

    assertEquals(HttpStatus.OK, delete("/moetemappe/" + moetemappeDTO.getId()).getStatusCode());
    assertTrue(matrikkelnummerRepository.findById(mnMoetesakDTO.getId()).isEmpty());
    assertTrue(matrikkelnummerRepository.findById(mnMoetedokumentDTO.getId()).isEmpty());
  }

  @Test
  void omittedFestenummerAndSeksjonsnummerDefaultToZero() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // POST without festenummer/seksjonsnummer
    var json = new JSONObject();
    json.put("kommunenummer", "0301");
    json.put("gaardsnummer", 100);
    json.put("bruksnummer", 200);
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/matrikkelnummer", json);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var mnDTO = gson.fromJson(response.getBody(), MatrikkelnummerDTO.class);
    assertEquals(0, mnDTO.getFestenummer());
    assertEquals(0, mnDTO.getSeksjonsnummer());

    // POST with explicit 0 — must return the same object (idempotent)
    var jsonWithZero = new JSONObject();
    jsonWithZero.put("kommunenummer", "0301");
    jsonWithZero.put("gaardsnummer", 100);
    jsonWithZero.put("bruksnummer", 200);
    jsonWithZero.put("festenummer", 0);
    jsonWithZero.put("seksjonsnummer", 0);
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/matrikkelnummer", jsonWithZero);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var mnDTOExplicit = gson.fromJson(response.getBody(), MatrikkelnummerDTO.class);
    assertEquals(mnDTO.getId(), mnDTOExplicit.getId());

    delete("/saksmappe/" + saksmappeDTO.getId());
  }

  private void assertMatrikkelnummerValues(
      MatrikkelnummerDTO matrikkelnummerDTO,
      String kommunenummer,
      int gaardsnummer,
      int bruksnummer) {
    assertNotNull(matrikkelnummerDTO);
    assertEquals(kommunenummer, matrikkelnummerDTO.getKommunenummer());
    assertEquals(gaardsnummer, matrikkelnummerDTO.getGaardsnummer());
    assertEquals(bruksnummer, matrikkelnummerDTO.getBruksnummer());
    assertEquals(0, matrikkelnummerDTO.getFestenummer());
    assertEquals(0, matrikkelnummerDTO.getSeksjonsnummer());
  }
}
