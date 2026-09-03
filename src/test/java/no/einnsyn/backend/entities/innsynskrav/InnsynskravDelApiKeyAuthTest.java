package no.einnsyn.backend.entities.innsynskrav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.authentication.bruker.models.TokenResponse;
import no.einnsyn.backend.common.exceptions.models.AuthorizationException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.bruker.models.Bruker;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import no.einnsyn.backend.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
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
class InnsynskravApiKeyAuthTest extends EinnsynControllerTestBase {

  ArkivDTO arkivDTO;
  ArkivdelDTO arkivdelDTO;
  SaksmappeDTO saksmappeDTO;
  Bruker bruker1;
  String bruker1Token;
  Bruker bruker2;
  String bruker2Token;

  @BeforeAll
  void setUp() throws Exception {
    // Add Arkiv, Saksmappe with Journalposts
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put(
        "journalpost",
        new JSONArray()
            .put(getJournalpostJSON())
            .put(getJournalpostJSON())
            .put(getJournalpostJSON())
            .put(getJournalpostJSON())
            .put(getJournalpostJSON()));
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Insert bruker1
    var bruker1JSON = getBrukerJSON();
    response = post("/bruker", bruker1JSON);
    var bruker1DTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    bruker1 = brukerService.find(bruker1DTO.getId());

    // Activate bruker1
    response = patch("/bruker/" + bruker1.getId() + "/activate/" + bruker1.getSecret());
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
    bruker2 = brukerService.find(bruker2DTO.getId());

    // Activate bruker2
    response = patch("/bruker/" + bruker2.getId() + "/activate/" + bruker2.getSecret());
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

    // Make sure objects are deleted
    assertEquals(
        HttpStatus.UNAUTHORIZED, get("/bruker/" + bruker1.getId(), bruker1Token).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, getAdmin("/bruker/" + bruker1.getId()).getStatusCode());
    assertEquals(
        HttpStatus.UNAUTHORIZED, get("/bruker/" + bruker2.getId(), bruker2Token).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, getAdmin("/bruker/" + bruker2.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }

  @Test
  void testListInnsynskravByBruker() throws Exception {
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    var journalpostList = getJournalpostList(saksmappeDTO.getId()).getItems();
    innsynskravJSON.put("journalpost", journalpostList.getFirst().getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));
    var response = post("/innsynskravBestilling", innsynskravBestillingJSON, bruker1Token);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);

    // Unauthorized cannot list by bruker, and is answered as if the Bruker did not exist
    response = get("/bruker/" + bruker1.getId() + "/innsynskrav");
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    // Another user cannot list by bruker
    response = get("/bruker/" + bruker1.getId() + "/innsynskrav", bruker2Token);
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    // Authorized can list by bruker
    response = get("/bruker/" + bruker1.getId() + "/innsynskrav", bruker1Token);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Admin can list by bruker
    response = getAdmin("/bruker/" + bruker1.getId() + "/innsynskrav");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Clean up
    assertEquals(
        HttpStatus.OK,
        delete("/innsynskravBestilling/" + innsynskravBestillingDTO.getId(), bruker1Token)
            .getStatusCode());
    deleteInnsynskravFromBestilling(innsynskravBestillingDTO);
  }

  @Test
  void testListInnsynskravByInnsynskravBestilling() throws Exception {
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    var journalpostList = getJournalpostList(saksmappeDTO.getId()).getItems();
    innsynskravJSON.put("journalpost", journalpostList.getFirst().getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));
    var response = post("/innsynskravBestilling", innsynskravBestillingJSON, bruker1Token);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);

    // Unauthorized cannot list by InnsynskravBestilling
    response =
        getAnon("/innsynskravBestilling/" + innsynskravBestillingDTO.getId() + "/innsynskrav");
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Owner of the saksmappe / journalpost cannot list by InnsynskravBestilling
    response = get("/innsynskravBestilling/" + innsynskravBestillingDTO.getId() + "/innsynskrav");
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Another user cannot list by InnsynskravBestilling
    response =
        get(
            "/innsynskravBestilling/" + innsynskravBestillingDTO.getId() + "/innsynskrav",
            bruker2Token);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Authorized can list by InnsynskravBestilling
    response =
        get(
            "/innsynskravBestilling/" + innsynskravBestillingDTO.getId() + "/innsynskrav",
            bruker1Token);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Admin can list by InnsynskravBestilling
    response =
        getAdmin("/innsynskravBestilling/" + innsynskravBestillingDTO.getId() + "/innsynskrav");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Clean up
    assertEquals(
        HttpStatus.OK,
        delete("/innsynskravBestilling/" + innsynskravBestillingDTO.getId(), bruker1Token)
            .getStatusCode());
    deleteInnsynskravFromBestilling(innsynskravBestillingDTO);
  }

  @Test
  void testListInnsynskravByEnhet() throws Exception {
    // Unauthorized cannot list by enhet
    var response = getAnon("/enhet/" + journalenhetId + "/innsynskrav");
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Another user cannot list by enhet
    response = get("/enhet/" + journalenhetId + "/innsynskrav", bruker2Token);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Another enhet cannot list by enhet
    response = get("/enhet/" + journalenhetId + "/innsynskrav", journalenhet2Key);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Authorized can list by enhet
    response = get("/enhet/" + journalenhetId + "/innsynskrav");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Admin can list by enhet
    response = getAdmin("/enhet/" + journalenhetId + "/innsynskrav");
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testGetInnsynskrav() throws Exception {
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    var journalpostList = getJournalpostList(saksmappeDTO.getId()).getItems();
    innsynskravJSON.put("journalpost", journalpostList.getFirst().getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));
    var response = post("/innsynskravBestilling", innsynskravBestillingJSON, bruker1Token);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    var innsynskravId = innsynskravBestillingDTO.getInnsynskrav().getFirst().getId();

    // Unauthorized cannot get
    response = getAnon("/innsynskrav/" + innsynskravId);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Another user cannot get
    response = get("/innsynskrav/" + innsynskravId, bruker2Token);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Owner of another Enhet cannot get
    response = get("/innsynskrav/" + innsynskravId, journalenhet2Key);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Authorized can get
    response = get("/innsynskrav/" + innsynskravId, bruker1Token);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Owner of the Enhet can get
    response = get("/innsynskrav/" + innsynskravId);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Admin can get
    response = getAdmin("/innsynskrav/" + innsynskravId);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Clean up
    assertEquals(
        HttpStatus.OK,
        delete("/innsynskravBestilling/" + innsynskravBestillingDTO.getId(), bruker1Token)
            .getStatusCode());
    deleteInnsynskravFromBestilling(innsynskravBestillingDTO);
  }

  /** Helper that adds an InnsynskravBestilling with one Innsynskrav per given journalpost. */
  private InnsynskravBestillingDTO addInnsynskravBestilling(String token, String... journalpostIds)
      throws Exception {
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravArray = new JSONArray();
    for (var journalpostId : journalpostIds) {
      innsynskravArray.put(getInnsynskravJSON().put("journalpost", journalpostId));
    }
    innsynskravBestillingJSON.put("innsynskrav", innsynskravArray);
    var response =
        token == null
            ? postAnon("/innsynskravBestilling", innsynskravBestillingJSON)
            : post("/innsynskravBestilling", innsynskravBestillingJSON, token);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    return gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
  }

  /** Bestillings are locked on creation. Unlock to test the unlocked authorization branches. */
  private void unlockInnsynskravBestilling(String id) {
    var innsynskravBestilling = innsynskravBestillingRepository.findById(id).orElseThrow();
    innsynskravBestilling.setLocked(false);
    innsynskravBestillingRepository.save(innsynskravBestilling);
  }

  @Test
  void testUpdateInnsynskrav() throws Exception {
    var journalpostList = getJournalpostList(saksmappeDTO.getId()).getItems();
    var innsynskravBestillingDTO =
        addInnsynskravBestilling(bruker1Token, journalpostList.getFirst().getId());
    var innsynskravId = innsynskravBestillingDTO.getInnsynskrav().getFirst().getId();

    // A sent (locked) InnsynskravBestilling is immutable, even for the owner
    var response = patch("/innsynskrav/" + innsynskravId, new JSONObject(), bruker1Token);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    unlockInnsynskravBestilling(innsynskravBestillingDTO.getId());

    // Unauthorized cannot update
    response = patchAnon("/innsynskrav/" + innsynskravId, new JSONObject());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Another user cannot update
    response = patch("/innsynskrav/" + innsynskravId, new JSONObject(), bruker2Token);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Owner of the Enhet cannot update
    response = patch("/innsynskrav/" + innsynskravId, new JSONObject());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // The owner can update
    response = patch("/innsynskrav/" + innsynskravId, new JSONObject(), bruker1Token);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Admin can update
    response = patchAdmin("/innsynskrav/" + innsynskravId, new JSONObject());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Clean up
    assertEquals(
        HttpStatus.OK,
        delete("/innsynskravBestilling/" + innsynskravBestillingDTO.getId(), bruker1Token)
            .getStatusCode());
    deleteInnsynskravFromBestilling(innsynskravBestillingDTO);
  }

  @Test
  void testDeleteInnsynskrav() throws Exception {
    var journalpostList = getJournalpostList(saksmappeDTO.getId()).getItems();
    var innsynskravBestillingDTO =
        addInnsynskravBestilling(
            bruker1Token,
            journalpostList.get(0).getId(),
            journalpostList.get(1).getId(),
            journalpostList.get(2).getId());
    var innsynskravIds =
        innsynskravBestillingDTO.getInnsynskrav().stream()
            .map(innsynskrav -> innsynskrav.getId())
            .toList();

    // Unauthorized cannot delete
    var response = deleteAnon("/innsynskrav/" + innsynskravIds.get(0));
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Another user cannot delete
    response = delete("/innsynskrav/" + innsynskravIds.get(0), bruker2Token);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Owner of the Enhet cannot delete
    response = delete("/innsynskrav/" + innsynskravIds.get(0));
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // The owner can delete
    response = delete("/innsynskrav/" + innsynskravIds.get(0), bruker1Token);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Admin can delete
    response = deleteAdmin("/innsynskrav/" + innsynskravIds.get(1));
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Deleting the InnsynskravBestilling orphans the remaining Innsynskrav. Nobody but admin can
    // delete an orphaned Innsynskrav.
    response = delete("/innsynskravBestilling/" + innsynskravBestillingDTO.getId(), bruker1Token);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/innsynskrav/" + innsynskravIds.get(2), bruker1Token);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    response = deleteAdmin("/innsynskrav/" + innsynskravIds.get(2));
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  /**
   * No endpoint adds Innsynskrav objects directly (they are created nested in an
   * InnsynskravBestilling), so the authorization rules for add are tested on the service.
   */
  @Test
  void testAddInnsynskravServiceAuthorization() throws Exception {
    // An InnsynskravBestilling is required
    var dto = new InnsynskravDTO();
    assertThrows(AuthorizationException.class, () -> innsynskravService.add(dto));

    // The InnsynskravBestilling must exist
    dto.setInnsynskravBestilling(new ExpandableField<>("ib_nonexistent"));
    assertThrows(AuthorizationException.class, () -> innsynskravService.add(dto));

    // Anonymous cannot add to a bestilling owned by a Bruker
    var journalpostList = getJournalpostList(saksmappeDTO.getId()).getItems();
    var ownedBestillingDTO =
        addInnsynskravBestilling(bruker1Token, journalpostList.getFirst().getId());
    unlockInnsynskravBestilling(ownedBestillingDTO.getId());
    dto.setInnsynskravBestilling(new ExpandableField<>(ownedBestillingDTO.getId()));
    assertThrows(AuthorizationException.class, () -> innsynskravService.add(dto));

    // Nobody can add to a sent (locked) anonymous bestilling
    var anonBestillingDTO = addInnsynskravBestilling(null, journalpostList.getFirst().getId());
    dto.setInnsynskravBestilling(new ExpandableField<>(anonBestillingDTO.getId()));
    assertThrows(AuthorizationException.class, () -> innsynskravService.add(dto));

    // Anybody can add to an unsent anonymous bestilling
    unlockInnsynskravBestilling(anonBestillingDTO.getId());
    dto.setJournalpost(new ExpandableField<>(journalpostList.get(1).getId()));
    var addedDTO = innsynskravService.add(dto);
    assertNotNull(addedDTO.getId());
    awaitSideEffects();

    // Clean up
    assertEquals(HttpStatus.OK, deleteAdmin("/innsynskrav/" + addedDTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.OK,
        delete("/innsynskravBestilling/" + ownedBestillingDTO.getId(), bruker1Token)
            .getStatusCode());
    deleteInnsynskravFromBestilling(ownedBestillingDTO);
    assertEquals(
        HttpStatus.OK,
        deleteAdmin("/innsynskravBestilling/" + anonBestillingDTO.getId()).getStatusCode());
    deleteInnsynskravFromBestilling(anonBestillingDTO);
  }
}
