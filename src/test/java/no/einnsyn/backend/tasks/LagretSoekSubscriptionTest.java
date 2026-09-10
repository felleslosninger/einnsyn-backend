package no.einnsyn.backend.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.time.ZonedDateTime;
import java.util.function.Function;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.authentication.bruker.models.TokenResponse;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.entities.lagretsoek.models.LagretSoekDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.awaitility.Awaitility;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LagretSoekSubscriptionTest extends EinnsynControllerTestBase {

  @Autowired TaskTestService taskTestService;

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

    // Reset mail mocks (Creating user has sent mail)
    resetMail();
  }

  @AfterAll
  public void tearDown() throws Exception {
    delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + arkivDTO.getId()).getStatusCode());

    deleteAdmin("/bruker/" + brukerDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, getAdmin("/bruker/" + brukerDTO.getId()).getStatusCode());
  }

  @SuppressWarnings("unchecked")
  @Test
  void testMatchingLagretSoek() throws Exception {
    // Create a LagretSoek
    var response =
        post("/bruker/" + brukerDTO.getId() + "/lagretSoek", getLagretSoekJSON(), accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var lagretSoekDTO = gson.fromJson(response.getBody(), LagretSoekDTO.class);

    // Await until indexed
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEs();

    // Refresh percolator index
    esClient.indices().refresh(r -> r.index(percolatorIndex));

    // No emails should have been sent
    verify(javaMailSender, never()).send(any(MimeMessage.class));

    // Add a saksmappe with a title that matches the LagretSoek ("foo")
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("offentligTittel", "foo");
    saksmappeJSON.put("offentligTittelSensitiv", "foo");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Await until indexed
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEs();

    // Should have saved one hit
    assertEquals(1, taskTestService.getLagretSoekHitCount(lagretSoekDTO.getId()));
    var lagretSoekHitIds = taskTestService.getLagretSoekHitIds(lagretSoekDTO.getId());
    assertEquals(1, lagretSoekHitIds.size());

    // Should send one mail after calling notifyLagretSoek()
    taskTestService.notifyLagretSoek();
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(1)).send(any(MimeMessage.class)));

    // Should have reset saved hits
    assertEquals(0, taskTestService.getLagretSoekHitCount(lagretSoekDTO.getId()));
    var updatedLagretSoekHitIds = taskTestService.getLagretSoekHitIds(lagretSoekDTO.getId());
    assertEquals(0, updatedLagretSoekHitIds.size());

    // Delete the Saksmappe
    response = delete("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Delete the LagretSoek
    response = delete("/lagretSoek/" + lagretSoekDTO.getId(), accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @SuppressWarnings("unchecked")
  @Test
  void testMatchingLagretSoekMoetemappeAndMoetesak() throws Exception {
    // Create a LagretSoek
    var response =
        post("/bruker/" + brukerDTO.getId() + "/lagretSoek", getLagretSoekJSON(), accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var lagretSoekDTO = gson.fromJson(response.getBody(), LagretSoekDTO.class);

    // Await until indexed
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEs();

    // Refresh percolator index
    esClient.indices().refresh(r -> r.index(percolatorIndex));

    // Add a matching moetemappe (getMoetemappeJSON() creates a Moetemappe with a Moetesak)
    var moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("offentligTittel", "foo");
    moetemappeJSON.put("offentligTittelSensitiv", "foo");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    // Await until indexed
    Awaitility.await().untilAsserted(() -> verify(esClient, times(2)).index(any(Function.class)));
    resetEs();

    // Should have saved two hits
    assertEquals(2, taskTestService.getLagretSoekHitCount(lagretSoekDTO.getId()));
    var lagretSoekHitIds = taskTestService.getLagretSoekHitIds(lagretSoekDTO.getId());
    assertEquals(2, lagretSoekHitIds.size());

    // Should send one mail after calling notifyLagretSoek()
    taskTestService.notifyLagretSoek();
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(1)).send(any(MimeMessage.class)));

    // Should have reset saved hits
    assertEquals(0, taskTestService.getLagretSoekHitCount(lagretSoekDTO.getId()));
    var updatedLagretSoekHitIds = taskTestService.getLagretSoekHitIds(lagretSoekDTO.getId());
    assertEquals(0, updatedLagretSoekHitIds.size());

    // Delete the Moetemappe
    response = delete("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Delete the LagretSoek
    response = delete("/lagretSoek/" + lagretSoekDTO.getId(), accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @SuppressWarnings("unchecked")
  @Test
  void testReindexDocumentThatTurnsAccessible() throws Exception {
    // Add Arkiv, Saksmappe with Journalposts that is not accessible
    var response = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    // Create a LagretSoek (by default, the query is "foo")
    response =
        post("/bruker/" + brukerDTO.getId() + "/lagretSoek", getLagretSoekJSON(), accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var lagretSoekDTO = gson.fromJson(response.getBody(), LagretSoekDTO.class);

    // Create a Saksmappe and Journalpost that will match "foo", but is not accessible until 2
    // seconds from now
    var accessibleAfter = ZonedDateTime.now().plusSeconds(2).toString();
    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("accessibleAfter", accessibleAfter);
    journalpostJSON.put("offentligTittelSensitiv", "foo");
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("journalpost", new JSONArray().put(journalpostJSON));
    saksmappeJSON.put("accessibleAfter", accessibleAfter);
    saksmappeJSON.put("offentligTittelSensitiv", "foo");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var journalpostDTO = getJournalpostList(saksmappeDTO.getId()).getItems().getFirst();

    // Await until indexed twice (journalpost + saksmappe)
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(2)).index(any(Function.class)));
    esClient.indices().refresh(r -> r.index(percolatorIndex));

    // Notify lagret soek (shouldn't notify anything yet)
    taskTestService.notifyLagretSoek();

    // Wait until journalpost is accessible (after 2 seconds)
    Awaitility.await()
        .untilAsserted(
            () -> {
              var response2 = getAnon("/journalpost/" + journalpostDTO.getId());
              assertEquals(HttpStatus.OK, response2.getStatusCode());
            });

    // No emails should have been sent
    verify(javaMailSender, never()).send(any(MimeMessage.class));

    // Trigger reindex and notify
    taskTestService.updateOutdatedDocuments();
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(2)).index(any(Function.class)));
    esClient.indices().refresh(r -> r.index(percolatorIndex));

    // An email should have been sent
    Awaitility.await()
        .untilAsserted(
            () -> {
              taskTestService.notifyLagretSoek();
              verify(javaMailSender, times(1)).send(any(MimeMessage.class));
            });

    // Delete lagret sak
    response = delete("/lagretSoek/" + lagretSoekDTO.getId(), accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Delete arkiv (and contents)
    response = delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @SuppressWarnings("unchecked")
  @Test
  void testMaxHitCount() throws Exception {
    // Create a LagretSoek
    var response =
        post("/bruker/" + brukerDTO.getId() + "/lagretSoek", getLagretSoekJSON(), accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var lagretSoekDTO = gson.fromJson(response.getBody(), LagretSoekDTO.class);
    var lagretSoekId = lagretSoekDTO.getId();
    var lagretSoekHitIds = taskTestService.getLagretSoekHitIds(lagretSoekId);
    assertEquals(0, lagretSoekHitIds.size());

    Awaitility.await().untilAsserted(() -> verify(esClient, times(1)).index(any(Function.class)));
    esClient.indices().refresh(r -> r.index(percolatorIndex));
    resetEs();

    // Add saksmappe for matching Journalposts
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var saksmappeId = saksmappeDTO.getId();

    Awaitility.await().untilAsserted(() -> verify(esClient, times(1)).index(any(Function.class)));
    resetEs();

    // Add 101 matching saksmappe
    for (var i = 0; i < 105; i++) {
      var journalpostJSON = getJournalpostJSON();
      journalpostJSON.put("offentligTittel", "foo");
      journalpostJSON.put("offentligTittelSensitiv", "foo");
      response = post("/saksmappe/" + saksmappeId + "/journalpost", journalpostJSON);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // Await until indexed ((journalpost + saksmappe) * 105)
    Awaitility.await().untilAsserted(() -> verify(esClient, times(210)).index(any(Function.class)));

    // Should have saved 101 hits (100+)
    assertEquals(101, taskTestService.getLagretSoekHitCount(lagretSoekId));

    // Notify lagret soek
    taskTestService.notifyLagretSoek();

    // Capture one sent email
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(1)).send(any(MimeMessage.class)));

    // Delete lagret soek
    response = delete("/lagretSoek/" + lagretSoekId, accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Delete saksmappe
    response = delete("/saksmappe/" + saksmappeId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @SuppressWarnings("unchecked")
  @Test
  void testWithdrawnOnlyHitSendsNoMail() throws Exception {
    var response =
        post("/bruker/" + brukerDTO.getId() + "/lagretSoek", getLagretSoekJSON(), accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var lagretSoekDTO = gson.fromJson(response.getBody(), LagretSoekDTO.class);
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEs();
    esClient.indices().refresh(r -> r.index(percolatorIndex));

    // A public Saksmappe matching "foo" registers a hit
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("offentligTittel", "foo");
    saksmappeJSON.put("offentligTittelSensitiv", "foo");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEs();
    assertEquals(1, taskTestService.getLagretSoekHitCount(lagretSoekDTO.getId()));

    // Publication is withdrawn before the notification is sent
    var updateJSON = new JSONObject();
    updateJSON.put("accessibleAfter", ZonedDateTime.now().plusDays(1).toString());
    response = patch("/saksmappe/" + saksmappeDTO.getId(), updateJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEs();
    assertEquals(1, taskTestService.getLagretSoekHitCount(lagretSoekDTO.getId()));

    // No mail is sent, and the withdrawn hit is cleared so it does not linger
    taskTestService.notifyLagretSoek();
    verify(javaMailSender, never()).send(any(MimeMessage.class));
    assertEquals(0, taskTestService.getLagretSoekHitCount(lagretSoekDTO.getId()));
    assertEquals(0, taskTestService.getLagretSoekHitIds(lagretSoekDTO.getId()).size());

    response = delete("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/lagretSoek/" + lagretSoekDTO.getId(), accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @SuppressWarnings("unchecked")
  @Test
  void testWithdrawnHitIsLeftOutOfMail() throws Exception {
    var response =
        post("/bruker/" + brukerDTO.getId() + "/lagretSoek", getLagretSoekJSON(), accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var lagretSoekDTO = gson.fromJson(response.getBody(), LagretSoekDTO.class);
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEs();
    esClient.indices().refresh(r -> r.index(percolatorIndex));

    // Two public Saksmapper matching "foo" register two hits
    var keptJSON = getSaksmappeJSON();
    keptJSON.put("offentligTittel", "foo kept");
    keptJSON.put("offentligTittelSensitiv", "foo kept");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", keptJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var keptDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEs();

    var withdrawnJSON = getSaksmappeJSON();
    withdrawnJSON.put("offentligTittel", "foo withdrawn");
    withdrawnJSON.put("offentligTittelSensitiv", "foo withdrawn");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", withdrawnJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var withdrawnDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEs();
    assertEquals(2, taskTestService.getLagretSoekHitCount(lagretSoekDTO.getId()));

    // One of them is withdrawn before the notification is sent
    var updateJSON = new JSONObject();
    updateJSON.put("accessibleAfter", ZonedDateTime.now().plusDays(1).toString());
    response = patch("/saksmappe/" + withdrawnDTO.getId(), updateJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEs();

    // The mail lists only the accessible hit
    taskTestService.notifyLagretSoek();
    var messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(1)).send(messageCaptor.capture()));
    var txt = getTxtContent(messageCaptor.getValue());
    assertTrue(txt.contains("foo kept"), txt);
    assertFalse(txt.contains("foo withdrawn"), txt);
    assertFalse(txt.contains(lagretSoekDTO.getLegacyQuery()), txt);
    assertEquals(0, taskTestService.getLagretSoekHitCount(lagretSoekDTO.getId()));

    response = delete("/saksmappe/" + keptDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/saksmappe/" + withdrawnDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/lagretSoek/" + lagretSoekDTO.getId(), accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @SuppressWarnings("unchecked")
  @Test
  void testSearchIsAnnouncedWhenOnlyUncachedMatchesRemain() throws Exception {
    var response =
        post("/bruker/" + brukerDTO.getId() + "/lagretSoek", getLagretSoekJSON(), accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var lagretSoekDTO = gson.fromJson(response.getBody(), LagretSoekDTO.class);
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEs();
    esClient.indices().refresh(r -> r.index(percolatorIndex));

    // A Saksmappe that does not match "foo" itself, holding one more matching Journalpost than the
    // hit cache keeps
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEs();

    var matches = 11;
    for (var i = 0; i < matches; i++) {
      var journalpostJSON = getJournalpostJSON();
      journalpostJSON.put("offentligTittel", "foo " + i);
      journalpostJSON.put("offentligTittelSensitiv", "foo " + i);
      response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }
    // Each Journalpost reindexes its Saksmappe too
    Awaitility.await()
        .untilAsserted(() -> verify(esClient, atLeast(2 * matches)).index(any(Function.class)));
    resetEs();
    assertEquals(matches, taskTestService.getLagretSoekHitCount(lagretSoekDTO.getId()));
    assertEquals(10, taskTestService.getLagretSoekHitIds(lagretSoekDTO.getId()).size());

    // The parent Saksmappe is withdrawn, which withdraws every cached hit. The service cannot tell
    // whether the uncached match is still public.
    var updateJSON = new JSONObject();
    updateJSON.put("accessibleAfter", ZonedDateTime.now().plusDays(1).toString());
    response = patch("/saksmappe/" + saksmappeDTO.getId(), updateJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEs();

    // The search is still announced, with the remaining count and a link to the live search, but
    // without any of the withdrawn titles
    taskTestService.notifyLagretSoek();
    var messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(1)).send(messageCaptor.capture()));
    var txt = getTxtContent(messageCaptor.getValue());
    assertTrue(txt.contains(lagretSoekDTO.getLabel()), txt);
    // The text template escapes "=" in the link, so match the path and the count separately
    assertTrue(txt.contains("/sok?f"), txt);
    assertTrue(txt.contains("(1 "), txt);
    assertFalse(txt.contains("foo "), txt);
    assertFalse(txt.contains("/saksmappe?id="), txt);
    assertEquals(0, taskTestService.getLagretSoekHitCount(lagretSoekDTO.getId()));
    assertEquals(0, taskTestService.getLagretSoekHitIds(lagretSoekDTO.getId()).size());

    response = delete("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/lagretSoek/" + lagretSoekDTO.getId(), accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @SuppressWarnings("unchecked")
  @Test
  void testJournalpostHitIsWithdrawnWithParentSaksmappe() throws Exception {
    var response =
        post("/bruker/" + brukerDTO.getId() + "/lagretSoek", getLagretSoekJSON(), accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var lagretSoekDTO = gson.fromJson(response.getBody(), LagretSoekDTO.class);
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEs();
    esClient.indices().refresh(r -> r.index(percolatorIndex));

    // A Saksmappe that does not match "foo" itself
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEs();
    assertEquals(0, taskTestService.getLagretSoekHitCount(lagretSoekDTO.getId()));

    // A public Journalpost in it matches, and registers a hit
    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("offentligTittel", "foo");
    journalpostJSON.put("offentligTittelSensitiv", "foo");
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(2)).index(any(Function.class)));
    resetEs();
    assertEquals(1, taskTestService.getLagretSoekHitCount(lagretSoekDTO.getId()));

    // The parent Saksmappe is withdrawn before the notification is sent. The Journalpost keeps its
    // own accessibleAfter, but is no longer public.
    var updateJSON = new JSONObject();
    updateJSON.put("accessibleAfter", ZonedDateTime.now().plusDays(1).toString());
    response = patch("/saksmappe/" + saksmappeDTO.getId(), updateJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEs();

    // No mail is sent, and the hit is cleared
    taskTestService.notifyLagretSoek();
    verify(javaMailSender, never()).send(any(MimeMessage.class));
    assertEquals(0, taskTestService.getLagretSoekHitCount(lagretSoekDTO.getId()));
    assertEquals(0, taskTestService.getLagretSoekHitIds(lagretSoekDTO.getId()).size());

    response = delete("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/lagretSoek/" + lagretSoekDTO.getId(), accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @SuppressWarnings("unchecked")
  @Test
  void testHitIsDroppedWhenEnhetIsHidden() throws Exception {
    var response =
        post("/bruker/" + brukerDTO.getId() + "/lagretSoek", getLagretSoekJSON(), accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var lagretSoekDTO = gson.fromJson(response.getBody(), LagretSoekDTO.class);
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEs();
    esClient.indices().refresh(r -> r.index(percolatorIndex));

    // A child Enhet that the Saksmappe is filed under
    var enhetJSON = getEnhetJSON();
    enhetJSON.put("enhetskode", "LAGRETSOEK_SKJULT");
    response = post("/enhet/" + journalenhetId + "/underenhet", enhetJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var enhetDTO = gson.fromJson(response.getBody(), EnhetDTO.class);

    // A public Saksmappe matching "foo" registers a hit
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("offentligTittel", "foo");
    saksmappeJSON.put("offentligTittelSensitiv", "foo");
    saksmappeJSON.put("administrativEnhet", "LAGRETSOEK_SKJULT");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertEquals(enhetDTO.getId(), saksmappeDTO.getAdministrativEnhetObjekt().getId());
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEs();
    assertEquals(1, taskTestService.getLagretSoekHitCount(lagretSoekDTO.getId()));

    // The Enhet is hidden before the notification is sent
    var enhetUpdateJSON = new JSONObject();
    enhetUpdateJSON.put("skjult", true);
    response = patch("/enhet/" + enhetDTO.getId(), enhetUpdateJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // No mail is sent, and the hit is cleared
    taskTestService.notifyLagretSoek();
    verify(javaMailSender, never()).send(any(MimeMessage.class));
    assertEquals(0, taskTestService.getLagretSoekHitCount(lagretSoekDTO.getId()));
    assertEquals(0, taskTestService.getLagretSoekHitIds(lagretSoekDTO.getId()).size());

    response = delete("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/enhet/" + enhetDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/lagretSoek/" + lagretSoekDTO.getId(), accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  /** The mail is multipart/alternative with the text part first */
  private String getTxtContent(MimeMessage mimeMessage) throws Exception {
    var multipart = (MimeMultipart) mimeMessage.getContent();
    return multipart.getBodyPart(0).getContent().toString();
  }
}
