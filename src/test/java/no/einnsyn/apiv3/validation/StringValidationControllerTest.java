package no.einnsyn.apiv3.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.bruker.models.BrukerDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class StringValidationControllerTest extends EinnsynControllerTestBase {

  @Test
  void validateMaxLength() throws Exception {
    var arkivJSON = getArkivJSON();

    // Verify that we can set "tittel" to 500 chars
    arkivJSON.put("tittel", "a".repeat(500));
    var response = post("/arkiv", arkivJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var responseDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    delete("/arkiv/" + responseDTO.getId());

    // Verify that 501 chars is too long
    arkivJSON.put("tittel", "a".repeat(501));
    response = post("/arkiv", arkivJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void testNotEmpty() throws Exception {
    var arkivJSON = getArkivJSON();

    // Check null
    arkivJSON.remove("tittel");
    var response = post("/arkiv", arkivJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    // Check empty string
    arkivJSON.put("tittel", "");
    response = post("/arkiv", arkivJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    // Check whitespace
    arkivJSON.put("tittel", " ");
    response = post("/arkiv", arkivJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void testNull() throws Exception {
    var arkivJSON = getArkivJSON();

    // Check null
    arkivJSON.put("id", "foo");
    var response = post("/arkiv", arkivJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void testSSN() throws Exception {
    var arkivJSON = getArkivJSON();

    // Check non-SSN
    arkivJSON.put("tittel", "050638266010");
    var response = post("/arkiv", arkivJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var responseDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    delete("/arkiv/" + responseDTO.getId());

    // Check SSN
    arkivJSON.put("tittel", "31085314494");
    response = post("/arkiv", arkivJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void testURL() throws Exception {
    var arkivDTO = gson.fromJson(post("/arkiv", getArkivJSON()).getBody(), ArkivDTO.class);

    var saksmappeDTO =
        gson.fromJson(
            post("/arkiv/" + arkivDTO.getId() + "/saksmappe", getSaksmappeJSON()).getBody(),
            SaksmappeDTO.class);

    var journslpostDTO =
        gson.fromJson(
            post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", getJournalpostJSON())
                .getBody(),
            JournalpostDTO.class);

    var dokumentbeskrivelseJSON = getDokumentbeskrivelseJSON();
    var dokumentobjektJSON = getDokumentobjektJSON();
    dokumentbeskrivelseJSON.put("dokumentobjekt", new JSONArray(List.of(dokumentobjektJSON)));

    // Valid HTTPS
    dokumentobjektJSON.put("referanseDokumentfil", "https://www.example.com");
    assertEquals(
        HttpStatus.CREATED,
        post(
                "/journalpost/" + journslpostDTO.getId() + "/dokumentbeskrivelse",
                dokumentbeskrivelseJSON)
            .getStatusCode());

    // Valid HTTP
    dokumentobjektJSON.put("referanseDokumentfil", "http://www.example.com");
    assertEquals(
        HttpStatus.CREATED,
        post(
                "/journalpost/" + journslpostDTO.getId() + "/dokumentbeskrivelse",
                dokumentbeskrivelseJSON)
            .getStatusCode());

    // Invalid
    dokumentobjektJSON.put("referanseDokumentfil", "htp://www.example.com");
    assertEquals(
        HttpStatus.BAD_REQUEST,
        post(
                "/journalpost/" + journslpostDTO.getId() + "/dokumentbeskrivelse",
                dokumentbeskrivelseJSON)
            .getStatusCode());

    delete("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void testEmail() throws Exception {
    var brukerJSON = getBrukerJSON();
    brukerJSON.put("email", "foo");
    assertEquals(HttpStatus.BAD_REQUEST, post("/bruker", brukerJSON).getStatusCode());

    brukerJSON.put("email", "@");
    assertEquals(HttpStatus.BAD_REQUEST, post("/bruker", brukerJSON).getStatusCode());

    brukerJSON.put("email", "a@example.com");
    var response = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var responseDTO = gson.fromJson(response.getBody(), BrukerDTO.class);

    response = deleteAdmin("/bruker/" + responseDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}
