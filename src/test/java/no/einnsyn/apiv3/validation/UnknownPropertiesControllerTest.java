package no.einnsyn.apiv3.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.bruker.models.BrukerDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class UnknownPropertiesControllerTest extends EinnsynControllerTestBase {

  @Test
  void testUnknownProperties() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    var arkivJSON = getArkivJSON();
    arkivJSON.put("unknownProperty", "value");
    var response = post("/arkiv", arkivJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    arkivJSON.remove("unknownProperty");
    response = post("/arkiv", arkivJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("foo", "value");
    response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    saksmappeJSON.remove("foo");
    response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
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
    response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", moeteMappeJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    moeteMappeJSON.remove("biz");
    response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", moeteMappeJSON);
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
