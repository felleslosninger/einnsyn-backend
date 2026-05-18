package no.einnsyn.backend.entities.matrikkelnummer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.matrikkelnummer.models.MatrikkelnummerDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
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
class MatrikkelnummerControllerTest extends EinnsynControllerTestBase {

  ArkivDTO arkivDTO;
  ArkivdelDTO arkivdelDTO;

  @BeforeAll
  void setup() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkivDTO.getId());

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdelDTO.getId());
  }

  @AfterAll
  void teardown() throws Exception {
    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }

  @Test
  void addGetUpdateDeleteMatrikkelnummer() throws Exception {
    var matrikkelnummerJSON = getMatrikkelnummerJSON();

    var response = post("/matrikkelnummer", matrikkelnummerJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var matrikkelnummerDTO = gson.fromJson(response.getBody(), MatrikkelnummerDTO.class);
    assertTrue(matrikkelnummerDTO.getId().startsWith("mat_"));
    assertEquals(
        "/matrikkelnummer/" + matrikkelnummerDTO.getId(),
        response.getHeaders().getLocation().getPath());
    assertEquals("0301", matrikkelnummerDTO.getKommunenummer());
    assertEquals(matrikkelnummerJSON.getInt("gaardsnummer"), matrikkelnummerDTO.getGaardsnummer());
    assertEquals(matrikkelnummerJSON.getInt("bruksnummer"), matrikkelnummerDTO.getBruksnummer());
    assertEquals(journalenhetId, matrikkelnummerDTO.getJournalenhet().getId());

    response = get("/matrikkelnummer/" + matrikkelnummerDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var fetchedDTO = gson.fromJson(response.getBody(), MatrikkelnummerDTO.class);
    assertEquals(matrikkelnummerDTO.getId(), fetchedDTO.getId());

    var updateJSON = new JSONObject();
    updateJSON.put("seksjonsnummer", 7);
    response = patch("/matrikkelnummer/" + matrikkelnummerDTO.getId(), updateJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var updatedDTO = gson.fromJson(response.getBody(), MatrikkelnummerDTO.class);
    assertEquals(7, updatedDTO.getSeksjonsnummer());

    response =
        patch("/matrikkelnummer/" + matrikkelnummerDTO.getId(), updateJSON, journalenhet2Key);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    response = delete("/matrikkelnummer/" + matrikkelnummerDTO.getId(), journalenhet2Key);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    response = delete("/matrikkelnummer/" + matrikkelnummerDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND,
        get("/matrikkelnummer/" + matrikkelnummerDTO.getId()).getStatusCode());
  }

  @Test
  void matrikkelnummerIsScopedToJournalenhet() throws Exception {
    String arkiv2Id = null;
    String saksmappeId = null;
    String matrikkelnummerId = null;
    String matrikkelnummer2Id = null;

    try {
      var matrikkelnummerJSON = getMatrikkelnummerJSON();

      var response = post("/matrikkelnummer", matrikkelnummerJSON);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      var matrikkelnummerDTO = gson.fromJson(response.getBody(), MatrikkelnummerDTO.class);
      matrikkelnummerId = matrikkelnummerDTO.getId();

      response = post("/matrikkelnummer", matrikkelnummerJSON);
      assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

      response = post("/arkiv", getArkivJSON(), journalenhet2Key);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      var arkiv2DTO = gson.fromJson(response.getBody(), ArkivDTO.class);
      arkiv2Id = arkiv2DTO.getId();

      response = post("/arkiv/" + arkiv2Id + "/arkivdel", getArkivdelJSON(), journalenhet2Key);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      var arkivdel2DTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

      var matrikkelnummerArray = new JSONArray();
      matrikkelnummerArray.put(matrikkelnummerJSON);

      var saksmappeJSON = getSaksmappeJSON();
      saksmappeJSON.put("matrikkelnummer", matrikkelnummerArray);

      response =
          post("/arkivdel/" + arkivdel2DTO.getId() + "/saksmappe", saksmappeJSON, journalenhet2Key);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
      saksmappeId = saksmappeDTO.getId();

      response = get("/saksmappe/" + saksmappeId + "?expand=matrikkelnummer", journalenhet2Key);
      assertEquals(HttpStatus.OK, response.getStatusCode());
      saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
      var journalenhet2Matrikkelnummer = saksmappeDTO.getMatrikkelnummer().getFirst();
      matrikkelnummer2Id = journalenhet2Matrikkelnummer.getId();
      assertNotEquals(matrikkelnummerId, matrikkelnummer2Id);
      assertEquals(
          journalenhet2Id,
          journalenhet2Matrikkelnummer.getExpandedObject().getJournalenhet().getId());
    } finally {
      if (saksmappeId != null) {
        delete("/saksmappe/" + saksmappeId, journalenhet2Key);
      }
      if (arkiv2Id != null) {
        delete("/arkiv/" + arkiv2Id, journalenhet2Key);
      }
      if (matrikkelnummer2Id != null) {
        delete("/matrikkelnummer/" + matrikkelnummer2Id, journalenhet2Key);
      }
      if (matrikkelnummerId != null) {
        delete("/matrikkelnummer/" + matrikkelnummerId);
      }
    }
  }

  @Test
  void addMatrikkelnummerOnSaksmappeAndExpand() throws Exception {
    String saksmappeId = null;
    String matrikkelnummerId = null;

    try {
      var matrikkelnummerArray = new JSONArray();
      matrikkelnummerArray.put(getMatrikkelnummerJSON());

      var saksmappeJSON = getSaksmappeJSON();
      saksmappeJSON.put("matrikkelnummer", matrikkelnummerArray);

      var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
      saksmappeId = saksmappeDTO.getId();
      assertNotNull(saksmappeId);

      response = get("/saksmappe/" + saksmappeId + "?expand=matrikkelnummer");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
      assertEquals(1, saksmappeDTO.getMatrikkelnummer().size());

      var matrikkelnummerField = saksmappeDTO.getMatrikkelnummer().getFirst();
      matrikkelnummerId = matrikkelnummerField.getId();
      assertNotNull(matrikkelnummerId);
      var expandedMatrikkelnummer = matrikkelnummerField.getExpandedObject();
      assertNotNull(expandedMatrikkelnummer);
      assertEquals("0301", expandedMatrikkelnummer.getKommunenummer());
    } finally {
      if (saksmappeId != null) {
        delete("/saksmappe/" + saksmappeId);
      }
      if (matrikkelnummerId != null) {
        deleteAdmin("/matrikkelnummer/" + matrikkelnummerId);
      }
    }
  }

  @Test
  void addMatrikkelnummerOnJournalpostAndExpand() throws Exception {
    String saksmappeId = null;
    String journalpostId = null;
    String matrikkelnummerId = null;

    try {
      var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
      saksmappeId = saksmappeDTO.getId();

      var matrikkelnummerArray = new JSONArray();
      matrikkelnummerArray.put(getMatrikkelnummerJSON());

      var journalpostJSON = getJournalpostJSON();
      journalpostJSON.put("matrikkelnummer", matrikkelnummerArray);

      response = post("/saksmappe/" + saksmappeId + "/journalpost", journalpostJSON);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
      journalpostId = journalpostDTO.getId();
      assertNotNull(journalpostId);

      response = get("/journalpost/" + journalpostId + "?expand=matrikkelnummer");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
      assertEquals(1, journalpostDTO.getMatrikkelnummer().size());

      var matrikkelnummerField = journalpostDTO.getMatrikkelnummer().getFirst();
      matrikkelnummerId = matrikkelnummerField.getId();
      assertNotNull(matrikkelnummerId);
      var expandedMatrikkelnummer = matrikkelnummerField.getExpandedObject();
      assertNotNull(expandedMatrikkelnummer);
      assertEquals("0301", expandedMatrikkelnummer.getKommunenummer());
    } finally {
      if (journalpostId != null) {
        delete("/journalpost/" + journalpostId);
      }
      if (saksmappeId != null) {
        delete("/saksmappe/" + saksmappeId);
      }
      if (matrikkelnummerId != null) {
        deleteAdmin("/matrikkelnummer/" + matrikkelnummerId);
      }
    }
  }
}
