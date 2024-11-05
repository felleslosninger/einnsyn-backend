package no.einnsyn.apiv3.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.apiv3.entities.skjerming.models.SkjermingDTO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ExpandableObjectValidationControllerTest extends EinnsynControllerTestBase {

  private ArkivDTO arkivDTO;

  @BeforeAll
  void setUp() throws Exception {
    var arkivJSON = getArkivJSON();
    var response = post("/arkiv", arkivJSON);
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
  }

  @AfterAll
  void tearDown() throws Exception {
    delete("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void testNonExistingPath() throws Exception {
    var response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Update non-existing
    response = patch("/saksmappe/sm_nonexistant", getSaksmappeJSON());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    // Update existing
    response = patch("/saksmappe/" + saksmappeDTO.getId(), getSaksmappeJSON());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    delete("/saksmappe/" + saksmappeDTO.getId());
  }

  @Test
  void testNonExistingReference() throws Exception {
    var response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Add journalpost
    var journalpostJSON = getJournalpostJSON();
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Can not add the same object
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostDTO.getId());
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    // Can not add a journalpost with an ID
    journalpostJSON.put("id", "jp_foo");
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    // Create skjerming
    response = post("/skjerming", getSkjermingJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var skjermingDTO = gson.fromJson(response.getBody(), SkjermingDTO.class);

    // Update journalpost with skjerming
    journalpostJSON.put("skjerming", skjermingDTO.getId());
    journalpostJSON.remove("id");
    response = patch("/journalpost/" + journalpostDTO.getId(), journalpostJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Update with non-existing skjerming
    journalpostJSON.put("skjerming", "skj_foo");
    response = patch("/journalpost/" + journalpostDTO.getId(), journalpostJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    // Update with skjerming that has an invalid ID
    var skjermingJSON = getSkjermingJSON();
    skjermingJSON.put("id", "skj_foobar");
    journalpostJSON.put("skjerming", skjermingJSON);
    response = patch("/journalpost/" + journalpostDTO.getId(), journalpostJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    // Update with skjerming that has a valid ID
    skjermingJSON.put("id", skjermingDTO.getId());
    response = patch("/journalpost/" + journalpostDTO.getId(), journalpostJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    delete("/saksmappe/" + saksmappeDTO.getId());
    delete("/skjerming/" + skjermingDTO.getId());
  }
}
