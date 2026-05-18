package no.einnsyn.backend.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.gson.reflect.TypeToken;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import no.einnsyn.backend.EinnsynLegacyElasticTestBase;
import no.einnsyn.backend.authentication.bruker.models.TokenResponse;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.lagretsak.models.LagretSakDTO;
import no.einnsyn.backend.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.tasks.handlers.reindex.ElasticsearchReindexScheduler;
import org.awaitility.Awaitility;
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
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LagretSakSubscriptionTest extends EinnsynLegacyElasticTestBase {

  @Autowired
  TaskTestService taskTestService;
  @Autowired
  ElasticsearchReindexScheduler elasticsearchReindexScheduler;

  ArkivDTO arkivDTO;
  ArkivdelDTO arkivdelDTO;
  BrukerDTO brukerDTO;
  String accessToken;

  @BeforeAll
  public void setup() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    // Create user
    var brukerJSON = getBrukerJSON();
    response = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    var brukerObj = brukerService.find(brukerDTO.getId());

    // Activate user
    response = patch("/bruker/" + brukerDTO.getId() + "/activate/" + brukerObj.getSecret());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Get token
    var loginRequest = new JSONObject();
    loginRequest.put("username", brukerDTO.getEmail());
    loginRequest.put("password", brukerJSON.getString("password"));
    response = post("/auth/token", loginRequest);
    var tokenResponse = gson.fromJson(response.getBody(), TokenResponse.class);
    accessToken = tokenResponse.getToken();

    // Creating user has triggered mail sending
    resetMail();
  }

  @AfterAll
  public void tearDown() throws Exception {
    delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + arkivDTO.getId()).getStatusCode());

    deleteAdmin("/bruker/" + brukerDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, getAdmin("/bruker/" + brukerDTO.getId()).getStatusCode());
  }

  @Test
  void testLagretSaksmappeSubscription() throws Exception {
    // Create a Saksmappe
    var saksmappeJSON = getSaksmappeJSON();
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Create a lagretSak
    var lagretSakJSON = getLagretSakJSON();
    lagretSakJSON.put("saksmappe", saksmappeDTO.getId());
    response = post("/bruker/" + brukerDTO.getId() + "/lagretSak", lagretSakJSON, accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var lagretSakDTO = gson.fromJson(response.getBody(), LagretSakDTO.class);

    // Update the Saksmappe
    saksmappeJSON.put("offentligTittel", "Updated tittel");
    response = patch("/saksmappe/" + saksmappeDTO.getId(), saksmappeJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Await until indexed twice
    captureIndexedDocuments(2);
    resetEs();

    // Should have lagretSak hits
    assertEquals(1, taskTestService.getLagretSakHitCount(lagretSakDTO.getId()));

    taskTestService.notifyLagretSak();
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(1)).send(any(MimeMessage.class)));

    // Should have reset the lagretSak hits
    assertEquals(0, taskTestService.getLagretSakHitCount(lagretSakDTO.getId()));

    // Add a Journalpost to the Saksmappe
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", getJournalpostJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // Should have 1 lagretSak hit
    assertEquals(1, taskTestService.getLagretSakHitCount(lagretSakDTO.getId()));

    // Await until indexed
    captureIndexedDocuments(2);
    resetEs();

    taskTestService.notifyLagretSak();
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(2)).send(any(MimeMessage.class)));

    // Should have reset lagretSak hits
    assertEquals(0, taskTestService.getLagretSakHitCount(lagretSakDTO.getId()));

    // Delete the Saksmappe
    response = delete("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    captureDeletedDocuments(2);
  }

  @Test
  void testLagretSaksmappeSubscriptionIgnoresBackgroundReindex() throws Exception {
    // Create a Saksmappe
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("journalpost", new JSONArray().put(getJournalpostJSON()));
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    response = get("/saksmappe/" + saksmappeDTO.getId() + "/journalpost");
    var resultListType = new TypeToken<PaginatedList<JournalpostDTO>>() {
    }.getType();
    PaginatedList<JournalpostDTO> journalpostDTOList = gson.fromJson(response.getBody(), resultListType);
    var journalpostDTO = journalpostDTOList.getItems().getFirst();

    captureIndexedDocuments(2);
    resetEs();

    // Create a lagretSak subscription for the Saksmappe
    var lagretSakJSON = getLagretSakJSON();
    lagretSakJSON.put("saksmappe", saksmappeDTO.getId());
    response = post("/bruker/" + brukerDTO.getId() + "/lagretSak", lagretSakJSON, accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var lagretSakDTO = gson.fromJson(response.getBody(), LagretSakDTO.class);

    // Update the journalpost, should trigger LagretSak hit
    var updateJSON = new JSONObject();
    updateJSON.put("offentligTittel", "new title");
    response = patch("/journalpost/" + journalpostDTO.getId(), updateJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, taskTestService.getLagretSakHitCount(lagretSakDTO.getId()));
    taskTestService.notifyLagretSak();
    verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    resetMail();
    captureIndexedDocuments(2);
    resetEs();

    // Force a background reindex through the schema-version path without changing
    // the row.
    var originalSchemaTimestamp = (Instant) ReflectionTestUtils.getField(elasticsearchReindexScheduler,
        "saksmappeSchemaTimestamp");
    ReflectionTestUtils.setField(
        elasticsearchReindexScheduler, "saksmappeSchemaTimestamp", Instant.now().plusSeconds(3600));

    try {
      taskTestService.updateOutdatedDocuments();
      captureIndexedDocuments(1);
      resetEs();

      // Background reindexing should not create LagretSak hits or emails.
      assertEquals(0, taskTestService.getLagretSakHitCount(lagretSakDTO.getId()));

      taskTestService.notifyLagretSak();
      verify(javaMailSender, never()).send(any(MimeMessage.class));
    } finally {
      ReflectionTestUtils.setField(
          elasticsearchReindexScheduler, "saksmappeSchemaTimestamp", originalSchemaTimestamp);
    }

    response = delete("/lagretSak/" + lagretSakDTO.getId(), accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    response = delete("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    captureDeletedDocuments(2);
  }

  @Test
  void testLagretMoetemappeSubscriptionIgnoresBackgroundReindex() throws Exception {
    // Create a Moetemappe (default JSON includes one moetesak)
    var moetemappeJSON = getMoetemappeJSON();
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    var moetesakId = moetemappeDTO.getMoetesak().get(0).getId();

    captureIndexedDocuments(2); // moetemappe + moetesak
    resetEs();

    // Subscribe to the Moetemappe
    var lagretSakJSON = getLagretSakJSON();
    lagretSakJSON.put("moetemappe", moetemappeDTO.getId());
    response = post("/bruker/" + brukerDTO.getId() + "/lagretSak", lagretSakJSON, accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var lagretSakDTO = gson.fromJson(response.getBody(), LagretSakDTO.class);

    // Update the moetesak — cascade should bump moetemappe.updated and trigger a
    // hit
    var updateJSON = new JSONObject();
    updateJSON.put("offentligTittel", "new title");
    response = patch("/moetesak/" + moetesakId, updateJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, taskTestService.getLagretSakHitCount(lagretSakDTO.getId()));
    taskTestService.notifyLagretSak();
    verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    resetMail();
    captureIndexedDocuments(2); // moetesak + moetemappe
    resetEs();

    // Force a background reindex through the schema-version path without changing
    // the row.
    var originalSchemaTimestamp = (Instant) ReflectionTestUtils.getField(
        elasticsearchReindexScheduler, "moetemappeSchemaTimestamp");
    ReflectionTestUtils.setField(
        elasticsearchReindexScheduler,
        "moetemappeSchemaTimestamp",
        Instant.now().plusSeconds(3600));

    try {
      taskTestService.updateOutdatedDocuments();
      captureIndexedDocuments(1); // only moetemappe is touched by the schema bump
      resetEs();

      // Background reindexing should not create LagretSak hits or emails.
      assertEquals(0, taskTestService.getLagretSakHitCount(lagretSakDTO.getId()));

      taskTestService.notifyLagretSak();
      verify(javaMailSender, never()).send(any(MimeMessage.class));
    } finally {
      ReflectionTestUtils.setField(
          elasticsearchReindexScheduler, "moetemappeSchemaTimestamp", originalSchemaTimestamp);
    }

    response = delete("/lagretSak/" + lagretSakDTO.getId(), accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    response = delete("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    captureDeletedDocuments(2);
  }

  @Test
  void testKorrespondansepartChangeFiresMoetemappeSubscription() throws Exception {
    // Create a Moetemappe (default JSON includes moetedokuments with
    // korrespondanseparts)
    var moetemappeJSON = getMoetemappeJSON();
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    // Resolve a korrespondansepart attached to the first moetedokument
    var moetedokumentId = moetemappeDTO.getMoetedokument().get(0).getId();
    response = get("/moetedokument/" + moetedokumentId);
    var moetedokumentDTO = gson.fromJson(response.getBody(), MoetedokumentDTO.class);
    var korrespondansepartId = moetedokumentDTO.getKorrespondansepart().get(0).getId();

    captureIndexedDocuments(2); // moetemappe + moetesak
    resetEs();

    // Subscribe to the Moetemappe
    var lagretSakJSON = getLagretSakJSON();
    lagretSakJSON.put("moetemappe", moetemappeDTO.getId());
    response = post("/bruker/" + brukerDTO.getId() + "/lagretSak", lagretSakJSON, accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var lagretSakDTO = gson.fromJson(response.getBody(), LagretSakDTO.class);

    // Update the korrespondansepart. Korrespondansepart and Moetedokument are not
    // Indexable themselves, so the change must cascade through Moetedokument and
    // touchUpdated() the Moetemappe to fire its subscription.
    var updateJSON = new JSONObject();
    updateJSON.put("korrespondansepartNavn", "updated navn");
    response = patch("/korrespondansepart/" + korrespondansepartId, updateJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    assertEquals(1, taskTestService.getLagretSakHitCount(lagretSakDTO.getId()));
    taskTestService.notifyLagretSak();
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(1)).send(any(MimeMessage.class)));
    captureIndexedDocuments(1); // only moetemappe is reindexed
    resetEs();

    response = delete("/lagretSak/" + lagretSakDTO.getId(), accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    response = delete("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    captureDeletedDocuments(2);
  }

  @Test
  void testLagretMoetemappeSubscription() throws Exception {
    // Create a Moetemappe
    var moetemappeJSON = getMoetemappeJSON();
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    // Await until indexed
    captureIndexedDocuments(2); // Moetemappe contains one moetesak
    resetEs();

    // Create a lagretSak
    var lagretSakJSON = getLagretSakJSON();
    lagretSakJSON.put("moetemappe", moetemappeDTO.getId());
    response = post("/bruker/" + brukerDTO.getId() + "/lagretSak", lagretSakJSON, accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // Update the Moetemappe
    moetemappeJSON.put("offentligTittel", "Updated tittel");
    moetemappeJSON.remove("moetesak");
    response = patch("/moetemappe/" + moetemappeDTO.getId(), moetemappeJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    captureIndexedDocuments(2);
    resetEs();

    taskTestService.notifyLagretSak();

    Awaitility.await()
        .untilAsserted(
            () -> {
              verify(javaMailSender, times(1)).send(any(MimeMessage.class));
            });

    // Add a Moetesak to the Moetemappe
    response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", getMoetesakJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // Await until indexed
    captureIndexedDocuments(2);
    resetEs();

    taskTestService.notifyLagretSak();

    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(2)).send(any(MimeMessage.class)));

    // Delete the Moetemappe
    response = delete("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    captureDeletedDocuments(3);
  }

  @Test
  void testLagretMoeteAndSaksmappeSubscription() throws Exception {
    // Create a saksmappe
    var saksmappeJSON = getSaksmappeJSON();
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    captureIndexedDocuments(1);
    resetEs();

    // Create a moetemappe
    var moetemappeJSON = getMoetemappeJSON();
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    captureIndexedDocuments(2);
    resetEs();

    // Create a LagretSak (Saksmappe)
    var lagretSakJSON = getLagretSakJSON();
    lagretSakJSON.put("saksmappe", saksmappeDTO.getId());
    response = post("/bruker/" + brukerDTO.getId() + "/lagretSak", lagretSakJSON, accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // Create a LagretSak (Moetemappe)
    var lagretMoeteJSON = getLagretSakJSON();
    lagretMoeteJSON.put("moetemappe", moetemappeDTO.getId());
    response = post("/bruker/" + brukerDTO.getId() + "/lagretSak", lagretMoeteJSON, accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // Update the saksmappe
    saksmappeJSON.put("offentligTittel", "Updated tittel");
    response = patch("/saksmappe/" + saksmappeDTO.getId(), saksmappeJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    captureIndexedDocuments(1);
    resetEs();

    // Update the moetemappe
    moetemappeJSON.put("offentligTittel", "Updated tittel");
    moetemappeJSON.remove("moetesak");
    response = patch("/moetemappe/" + moetemappeDTO.getId(), moetemappeJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    captureIndexedDocuments(2);
    resetEs();

    taskTestService.notifyLagretSak();

    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(2)).send(any(MimeMessage.class)));

    // Delete the saksmappe
    response = delete("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    captureDeletedDocuments(1);
    resetEs();

    // Delete the moetemappe
    response = delete("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    captureDeletedDocuments(2);
    resetEs();
  }
}
