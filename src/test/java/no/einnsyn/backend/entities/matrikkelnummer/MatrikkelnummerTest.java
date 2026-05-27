package no.einnsyn.backend.entities.matrikkelnummer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.matrikkelnummer.models.Matrikkelnummer;
import no.einnsyn.backend.entities.matrikkelnummer.models.MatrikkelnummerDTO;
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
import org.springframework.dao.DataIntegrityViolationException;
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
        "matrikkelnummer", new JSONArray().put(getMatrikkelnummerJSON("0301", 1, 42)));

    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var matrikkelnummerDTO = assertMatrikkelnummerReference(saksmappeDTO.getMatrikkelnummer());

    var matrikkelnummer =
        matrikkelnummerRepository.findById(matrikkelnummerDTO.getId()).orElseThrow();
    assertEquals(saksmappeDTO.getId(), matrikkelnummer.getMappeId());
    assertNull(matrikkelnummer.getRegistreringId());

    response = get("/saksmappe/" + saksmappeDTO.getId() + "?expand=matrikkelnummer");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    matrikkelnummerDTO = saksmappeDTO.getMatrikkelnummer().getFirst().getExpandedObject();
    assertMatrikkelnummerValues(matrikkelnummerDTO, "0301", 1, 42);

    assertEquals(HttpStatus.OK, delete("/saksmappe/" + saksmappeDTO.getId()).getStatusCode());
    assertTrue(matrikkelnummerRepository.findById(matrikkelnummerDTO.getId()).isEmpty());
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
        matrikkelnummerRepository.findById(matrikkelnummerDTO.getId()).orElseThrow();
    assertNull(matrikkelnummer.getMappeId());
    assertEquals(journalpostDTO.getId(), matrikkelnummer.getRegistreringId());

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
        matrikkelnummerRepository.findById(moetemappeMatrikkelDTO.getId()).orElseThrow();
    assertEquals(moetemappeDTO.getId(), moetemappeMatrikkel.getMappeId());
    assertNull(moetemappeMatrikkel.getRegistreringId());

    var moetesakJSON = getMoetesakJSON();
    moetesakJSON.put("matrikkelnummer", new JSONArray().put(getMatrikkelnummerJSON("0301", 4, 11)));

    response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesakJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);
    var moetesakMatrikkelDTO = assertMatrikkelnummerReference(moetesakDTO.getMatrikkelnummer());
    var moetesakMatrikkel =
        matrikkelnummerRepository.findById(moetesakMatrikkelDTO.getId()).orElseThrow();
    assertNull(moetesakMatrikkel.getMappeId());
    assertEquals(moetesakDTO.getId(), moetesakMatrikkel.getRegistreringId());

    assertEquals(HttpStatus.OK, delete("/moetesak/" + moetesakDTO.getId()).getStatusCode());
    assertTrue(matrikkelnummerRepository.findById(moetesakMatrikkelDTO.getId()).isEmpty());
    assertEquals(HttpStatus.OK, delete("/moetemappe/" + moetemappeDTO.getId()).getStatusCode());
    assertTrue(matrikkelnummerRepository.findById(moetemappeMatrikkelDTO.getId()).isEmpty());
  }

  @Test
  void rejectMatrikkelnummerWithoutParent() {
    var matrikkelnummer = new Matrikkelnummer();
    matrikkelnummer.setKommunenummer("0301");
    matrikkelnummer.setGaardsnummer(1);
    matrikkelnummer.setBruksnummer(1);

    assertThrows(
        DataIntegrityViolationException.class,
        () -> matrikkelnummerRepository.saveAndFlush(matrikkelnummer));
  }

  @Test
  void rejectMatrikkelnummerIdReferenceOnPatch() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    var patchJSON = new JSONObject();
    patchJSON.put("matrikkelnummer", new JSONArray().put("mat_missing"));

    response = patch("/saksmappe/" + saksmappeDTO.getId(), patchJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    assertEquals(HttpStatus.OK, delete("/saksmappe/" + saksmappeDTO.getId()).getStatusCode());
  }

  private JSONObject getMatrikkelnummerJSON(
      String kommunenummer, int gaardsnummer, int bruksnummer) {
    var matrikkelnummerJSON = new JSONObject();
    matrikkelnummerJSON.put("kommunenummer", kommunenummer);
    matrikkelnummerJSON.put("gaardsnummer", gaardsnummer);
    matrikkelnummerJSON.put("bruksnummer", bruksnummer);
    matrikkelnummerJSON.put("festenummer", 0);
    matrikkelnummerJSON.put("seksjonsnummer", 0);
    return matrikkelnummerJSON;
  }

  private MatrikkelnummerDTO assertMatrikkelnummerReference(
      List<ExpandableField<MatrikkelnummerDTO>> matrikkelnummer) {
    assertNotNull(matrikkelnummer);
    assertEquals(1, matrikkelnummer.size());
    var matrikkelnummerDTO = matrikkelnummer.getFirst().getExpandedObject();
    if (matrikkelnummerDTO == null) {
      matrikkelnummerDTO = new MatrikkelnummerDTO();
      matrikkelnummerDTO.setId(matrikkelnummer.getFirst().getId());
    }
    assertNotNull(matrikkelnummerDTO.getId());
    return matrikkelnummerDTO;
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
