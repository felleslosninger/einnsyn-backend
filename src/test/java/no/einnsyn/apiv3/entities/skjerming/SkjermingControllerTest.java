package no.einnsyn.apiv3.entities.skjerming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SkjermingControllerTest extends EinnsynControllerTestBase {

  @Test
  void testSharedSkjerming() throws Exception {
    var arkivJSON = getArkivJSON();
    var response = post("/arkiv", arkivJSON);
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkivDTO.getId());

    var arkivdelJSON = getArkivdelJSON();
    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", arkivdelJSON);
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdelDTO.getId());
    assertEquals(arkivdelDTO.getParent().getId(), arkivDTO.getId());

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappeDTO.getId());
    assertEquals(saksmappeDTO.getParent().getId(), arkivdelDTO.getId());

    var skjermingJSON = getSkjermingJSON();
    var journalpost1JSON = getJournalpostJSON();
    var journalpost2JSON = getJournalpostJSON();
    journalpost1JSON.put("skjerming", skjermingJSON);
    journalpost2JSON.put("skjerming", skjermingJSON);

    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpost1JSON);
    var journalpost1DTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertNotNull(journalpost1DTO.getId());

    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpost2JSON);
    var journalpost2DTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertNotNull(journalpost2DTO.getId());

    var skjerming1DTOField = journalpost1DTO.getSkjerming();
    var skjerming2DTOField = journalpost2DTO.getSkjerming();
    assertEquals(skjerming1DTOField.getId(), skjerming2DTOField.getId());

    // Delete journalpost1, make sure skjerming still exists
    delete("/journalpost/" + journalpost1DTO.getId());
    response = get("/skjerming/" + skjerming1DTOField.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Delete journalpost2, make sure skjerming is deleted
    delete("/journalpost/" + journalpost2DTO.getId());
    response = get("/skjerming/" + skjerming1DTOField.getId());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    delete("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void testSkjermingDifferentSkjermingshjemmel() throws Exception {
    var arkivJSON = getArkivJSON();
    var response = post("/arkiv", arkivJSON);
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkivDTO.getId());

    var arkivdelJSON = getArkivdelJSON();
    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", arkivdelJSON);
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdelDTO.getId());
    assertEquals(arkivdelDTO.getParent().getId(), arkivDTO.getId());

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappeDTO.getId());
    assertEquals(saksmappeDTO.getParent().getId(), arkivdelDTO.getId());

    var skjerming1JSON = getSkjermingJSON();
    var skjerming2JSON = getSkjermingJSON();
    var journalpost1JSON = getJournalpostJSON();
    var journalpost2JSON = getJournalpostJSON();
    journalpost1JSON.put("skjerming", skjerming1JSON);
    skjerming2JSON.put("skjermingshjemmel", "ny hjemmel");
    journalpost2JSON.put("skjerming", skjerming2JSON);

    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpost1JSON);
    var journalpost1DTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertNotNull(journalpost1DTO.getId());

    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpost2JSON);
    var journalpost2DTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertNotNull(journalpost2DTO.getId());

    var skjerming1DTOField = journalpost1DTO.getSkjerming();
    var skjerming2DTOField = journalpost2DTO.getSkjerming();
    assertNotEquals(skjerming1DTOField.getId(), skjerming2DTOField.getId());

    // Delete journalpost1, make sure skjerming is deleted
    delete("/journalpost/" + journalpost1DTO.getId());
    response = get("/skjerming/" + skjerming1DTOField.getId());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    // Delete journalpost2, make sure skjerming is deleted
    delete("/journalpost/" + journalpost2DTO.getId());
    response = get("/skjerming/" + skjerming1DTOField.getId());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    delete("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void testSkjermingDifferentTilgangsrestriksjon() throws Exception {
    var arkivJSON = getArkivJSON();
    var response = post("/arkiv", arkivJSON);
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkivDTO.getId());

    var arkivdelJSON = getArkivdelJSON();
    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", arkivdelJSON);
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdelDTO.getId());
    assertEquals(arkivdelDTO.getParent().getId(), arkivDTO.getId());

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappeDTO.getId());
    assertEquals(saksmappeDTO.getParent().getId(), arkivdelDTO.getId());

    var skjerming1JSON = getSkjermingJSON();
    var skjerming2JSON = getSkjermingJSON();
    var journalpost1JSON = getJournalpostJSON();
    var journalpost2JSON = getJournalpostJSON();
    journalpost1JSON.put("skjerming", skjerming1JSON);
    skjerming2JSON.put("tilgangsrestriksjon", "ny tilgangsrestriksjon");
    journalpost2JSON.put("skjerming", skjerming2JSON);

    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpost1JSON);
    var journalpost1DTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertNotNull(journalpost1DTO.getId());

    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpost2JSON);
    var journalpost2DTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertNotNull(journalpost2DTO.getId());

    var skjerming1DTOField = journalpost1DTO.getSkjerming();
    var skjerming2DTOField = journalpost2DTO.getSkjerming();
    assertNotEquals(skjerming1DTOField.getId(), skjerming2DTOField.getId());

    // Delete journalpost1, make sure skjerming is deleted
    delete("/journalpost/" + journalpost1DTO.getId());
    response = get("/skjerming/" + skjerming1DTOField.getId());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    // Delete journalpost2, make sure skjerming is deleted
    delete("/journalpost/" + journalpost2DTO.getId());
    response = get("/skjerming/" + skjerming1DTOField.getId());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    delete("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void testSkjermingDifferentJournalenhet() throws Exception {
    var arkivJSON = getArkivJSON();
    var response = post("/arkiv", arkivJSON);
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkivDTO.getId());

    var arkivdelJSON = getArkivdelJSON();
    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", arkivdelJSON);
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdelDTO.getId());
    assertEquals(arkivdelDTO.getParent().getId(), arkivDTO.getId());

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappeDTO.getId());
    assertEquals(saksmappeDTO.getParent().getId(), arkivdelDTO.getId());

    var skjerming1JSON = getSkjermingJSON();
    var skjerming2JSON = getSkjermingJSON();
    var journalpost1JSON = getJournalpostJSON();
    var journalpost2JSON = getJournalpostJSON();
    journalpost1JSON.put("skjerming", skjerming1JSON);
    skjerming2JSON.put("journalenhet", underenhetId);
    journalpost2JSON.put("skjerming", skjerming2JSON);

    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpost1JSON);
    var journalpost1DTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertNotNull(journalpost1DTO.getId());

    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpost2JSON);
    var journalpost2DTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertNotNull(journalpost2DTO.getId());

    var skjerming1DTOField = journalpost1DTO.getSkjerming();
    var skjerming2DTOField = journalpost2DTO.getSkjerming();
    assertNotEquals(skjerming1DTOField.getId(), skjerming2DTOField.getId());

    // Delete journalpost1, make sure skjerming1 is deleted
    delete("/journalpost/" + journalpost1DTO.getId());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/skjerming/" + skjerming1DTOField.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, get("/skjerming/" + skjerming2DTOField.getId()).getStatusCode());

    // Delete journalpost2, make sure skjerming2 is deleted
    delete("/journalpost/" + journalpost2DTO.getId());
    response = get("/skjerming/" + skjerming2DTOField.getId());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    delete("/arkiv/" + arkivDTO.getId());
  }
}
