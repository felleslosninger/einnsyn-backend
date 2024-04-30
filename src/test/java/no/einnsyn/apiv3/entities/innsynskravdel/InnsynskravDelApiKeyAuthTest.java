package no.einnsyn.apiv3.entities.innsynskravdel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.authentication.bruker.models.TokenResponse;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.bruker.models.Bruker;
import no.einnsyn.apiv3.entities.bruker.models.BrukerDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.clients.ip.IPSender;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InnsynskravDelApiKeyAuthTest extends EinnsynControllerTestBase {

  @MockBean private JavaMailSender javaMailSender;

  @MockBean private IPSender ipSender;

  ArkivDTO arkivDTO;
  SaksmappeDTO saksmappeDTO;
  Bruker bruker1;
  String bruker1Token;
  Bruker bruker2;
  String bruker2Token;

  @BeforeAll
  void setUp() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    // Add Arkiv, Saksmappe with Journalposts
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put(
        "journalpost",
        new JSONArray()
            .put(getJournalpostJSON())
            .put(getJournalpostJSON())
            .put(getJournalpostJSON())
            .put(getJournalpostJSON())
            .put(getJournalpostJSON()));
    response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Insert bruker1
    var bruker1JSON = getBrukerJSON();
    response = post("/bruker", bruker1JSON);
    var bruker1DTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    bruker1 = brukerService.findById(bruker1DTO.getId());

    // Activate bruker1
    response = put("/bruker/" + bruker1.getId() + "/activate/" + bruker1.getSecret());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Get token for bruker1
    var tokenResponse = post("/auth/token", getLoginJSON(bruker1JSON));
    assertEquals(HttpStatus.OK, tokenResponse.getStatusCode());
    var tokenResponseDTO = gson.fromJson(tokenResponse.getBody(), TokenResponse.class);
    bruker1Token = tokenResponseDTO.getToken();

    // Add bruker2
    var bruker2JSON = getBrukerJSON();
    response = post("/bruker", bruker2JSON);
    var bruker2DTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    bruker2 = brukerService.findById(bruker2DTO.getId());

    // Activate bruker2
    response = put("/bruker/" + bruker2.getId() + "/activate/" + bruker2.getSecret());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Get token for bruker2
    tokenResponse = post("/auth/token", getLoginJSON(bruker2JSON));
    assertEquals(HttpStatus.OK, tokenResponse.getStatusCode());
    tokenResponseDTO = gson.fromJson(tokenResponse.getBody(), TokenResponse.class);
    bruker2Token = tokenResponseDTO.getToken();
  }

  @AfterAll
  void tearDown() throws Exception {
    // Clean up
    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/bruker/" + bruker1.getId(), bruker1Token).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/bruker/" + bruker2.getId(), bruker2Token).getStatusCode());
  }

  @Test
  void testListInnsynskravDelByBruker() throws Exception {
    var innsynskravJSON = getInnsynskravJSON();
    var innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", saksmappeDTO.getJournalpost().getFirst().getId());
    innsynskravJSON.put("innsynskravDel", new JSONArray().put(innsynskravDelJSON));
    var response = post("/innsynskrav", innsynskravJSON, bruker1Token);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);

    // Unauthorized cannot list by bruker
    response = get("/bruker/" + bruker1.getId() + "/innsynskravDel");
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Another user cannot list by bruker
    response = get("/bruker/" + bruker1.getId() + "/innsynskravDel", bruker2Token);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Authorized can list by bruker
    response = get("/bruker/" + bruker1.getId() + "/innsynskravDel", bruker1Token);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Admin can list by bruker
    response = getAdmin("/bruker/" + bruker1.getId() + "/innsynskravDel");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Clean up
    assertEquals(
        HttpStatus.OK,
        delete("/innsynskrav/" + innsynskravDTO.getId(), bruker1Token).getStatusCode());
  }

  @Test
  void testListInnsynskravDelByInnsynskrav() throws Exception {
    var innsynskravJSON = getInnsynskravJSON();
    var innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", saksmappeDTO.getJournalpost().getFirst().getId());
    innsynskravJSON.put("innsynskravDel", new JSONArray().put(innsynskravDelJSON));
    var response = post("/innsynskrav", innsynskravJSON, bruker1Token);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);

    // Unauthorized cannot list by innsynskrav
    response = getAnon("/innsynskrav/" + innsynskravDTO.getId() + "/innsynskravDel");
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Owner of the saksmappe / journalpost cannot list by innsynskrav
    response = get("/innsynskrav/" + innsynskravDTO.getId() + "/innsynskravDel");
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Another user cannot list by innsynskrav
    response = get("/innsynskrav/" + innsynskravDTO.getId() + "/innsynskravDel", bruker2Token);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Authorized can list by innsynskrav
    response = get("/innsynskrav/" + innsynskravDTO.getId() + "/innsynskravDel", bruker1Token);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Admin can list by innsynskrav
    response = getAdmin("/innsynskrav/" + innsynskravDTO.getId() + "/innsynskravDel");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Clean up
    assertEquals(
        HttpStatus.OK,
        delete("/innsynskrav/" + innsynskravDTO.getId(), bruker1Token).getStatusCode());
  }

  @Test
  void testListInnsynskravDelByEnhet() throws Exception {
    // Unauthorized cannot list by enhet
    var response = getAnon("/enhet/" + journalenhetId + "/innsynskravDel");
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Another user cannot list by enhet
    response = get("/enhet/" + journalenhetId + "/innsynskravDel", bruker2Token);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Another enhet cannot list by enhet
    response = get("/enhet/" + journalenhetId + "/innsynskravDel", journalenhet2Key);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Authorized can list by enhet
    response = get("/enhet/" + journalenhetId + "/innsynskravDel");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Admin can list by enhet
    response = getAdmin("/enhet/" + journalenhetId + "/innsynskravDel");
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testGetInnsynskravDel() throws Exception {
    var innsynskravJSON = getInnsynskravJSON();
    var innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", saksmappeDTO.getJournalpost().getFirst().getId());
    innsynskravJSON.put("innsynskravDel", new JSONArray().put(innsynskravDelJSON));
    var response = post("/innsynskrav", innsynskravJSON, bruker1Token);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);
    var innsynskravDelId = innsynskravDTO.getInnsynskravDel().getFirst().getId();

    // Unauthorized cannot get
    response = getAnon("/innsynskravDel/" + innsynskravDelId);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Another user cannot get
    response = get("/innsynskravDel/" + innsynskravDelId, bruker2Token);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Owner of another Enhet cannot get
    response = get("/innsynskravDel/" + innsynskravDelId, journalenhet2Key);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Authorized can get
    response = get("/innsynskravDel/" + innsynskravDelId, bruker1Token);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Owner of the Enhet can get
    response = get("/innsynskravDel/" + innsynskravDelId);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Admin can get
    response = getAdmin("/innsynskravDel/" + innsynskravDelId);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Clean up
    assertEquals(
        HttpStatus.OK,
        delete("/innsynskrav/" + innsynskravDTO.getId(), bruker1Token).getStatusCode());
  }
}
