package no.einnsyn.backend.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UnknownPropertiesControllerTest extends EinnsynControllerTestBase {

  @Test
  void testUnknownProperties() throws Exception {
    var arkivJSON = getArkivJSON();
    arkivJSON.put("unknownProperty", "value");
    var response = post("/arkiv", arkivJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    arkivJSON.remove("unknownProperty");
    response = post("/arkiv", arkivJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("foo", "value");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    saksmappeJSON.remove("foo");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("bar", "value");
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    journalpostJSON.remove("bar");
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var brukerJSON = getBrukerJSON();
    brukerJSON.put("baz", "value");
    response = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    brukerJSON.remove("baz");
    response = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    deleteAdmin("/bruker/" + brukerDTO.getId());

    var moeteMappeJSON = getMoetemappeJSON();
    moeteMappeJSON.put("biz", "value");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moeteMappeJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    moeteMappeJSON.remove("biz");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moeteMappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    var moetesakJSON = getMoetesakJSON();
    moetesakJSON.put("buz", "value");
    response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesakJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    moetesakJSON.remove("buz");
    response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesakJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    delete("/arkiv/" + arkivDTO.getId());
  }
}
