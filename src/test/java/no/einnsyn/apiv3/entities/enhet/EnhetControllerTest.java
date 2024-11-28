package no.einnsyn.apiv3.entities.enhet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.reflect.TypeToken;
import java.util.List;
import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.apikey.models.ApiKeyDTO;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EnhetControllerTest extends EinnsynControllerTestBase {

  @Test
  void insertEnhet() throws Exception {
    var enhetJSON = getEnhetJSON();
    var enhetResponse = post("/enhet/" + journalenhetId + "/underenhet", enhetJSON);
    assertEquals(HttpStatus.CREATED, enhetResponse.getStatusCode());
    var insertedEnhetDTO = gson.fromJson(enhetResponse.getBody(), EnhetDTO.class);
    assertEquals(enhetJSON.get("navn"), insertedEnhetDTO.getNavn());
    assertEquals(enhetJSON.get("navnNynorsk"), insertedEnhetDTO.getNavnNynorsk());
    assertEquals(enhetJSON.get("navnEngelsk"), insertedEnhetDTO.getNavnEngelsk());
    assertEquals(enhetJSON.get("navnSami"), insertedEnhetDTO.getNavnSami());
    assertEquals(enhetJSON.get("avsluttetDato").toString(), insertedEnhetDTO.getAvsluttetDato());
    assertEquals(enhetJSON.get("innsynskravEpost"), insertedEnhetDTO.getInnsynskravEpost());
    assertEquals(enhetJSON.get("kontaktpunktAdresse"), insertedEnhetDTO.getKontaktpunktAdresse());
    assertEquals(enhetJSON.get("kontaktpunktEpost"), insertedEnhetDTO.getKontaktpunktEpost());
    assertEquals(enhetJSON.get("kontaktpunktTelefon"), insertedEnhetDTO.getKontaktpunktTelefon());
    assertEquals(enhetJSON.get("orgnummer"), insertedEnhetDTO.getOrgnummer());
    assertEquals(enhetJSON.get("enhetskode"), insertedEnhetDTO.getEnhetskode());
    assertEquals(
        enhetJSON.get("enhetstype").toString(), insertedEnhetDTO.getEnhetstype().toString());
    assertEquals(enhetJSON.get("skjult"), insertedEnhetDTO.getSkjult());
    assertEquals(enhetJSON.get("avsluttetDato").toString(), insertedEnhetDTO.getAvsluttetDato());
    String enhetId = insertedEnhetDTO.getId();

    // Check that we can get the new enhet from the API
    enhetResponse = get("/enhet/" + enhetId);
    assertEquals(HttpStatus.OK, enhetResponse.getStatusCode());

    // Check that we can update the enhet
    enhetJSON.put("navn", "updatedNavn");
    enhetResponse = patch("/enhet/" + enhetId, enhetJSON);
    assertEquals(HttpStatus.OK, enhetResponse.getStatusCode());
    insertedEnhetDTO = gson.fromJson(enhetResponse.getBody(), EnhetDTO.class);
    assertEquals(enhetJSON.get("navn"), insertedEnhetDTO.getNavn());

    // Check that we can delete the enhet
    enhetResponse = delete("/enhet/" + enhetId);
    assertEquals(HttpStatus.OK, enhetResponse.getStatusCode());

    // Check that the enhet is deleted
    enhetResponse = get("/enhet/" + enhetId);
    assertEquals(HttpStatus.NOT_FOUND, enhetResponse.getStatusCode());
  }

  /**
   * Add new enhet with "parent" field
   *
   * @throws Exception
   */
  @Test
  void addUnderenhetWithParent() throws Exception {
    var parentEnhetDTO = getEnhetJSON();
    var parentEnhetResponse = post("/enhet/" + journalenhetId + "/underenhet", parentEnhetDTO);
    assertEquals(HttpStatus.CREATED, parentEnhetResponse.getStatusCode());
    var insertedParentEnhetDTO = gson.fromJson(parentEnhetResponse.getBody(), EnhetDTO.class);
    var parentEnhetId = insertedParentEnhetDTO.getId();

    var childEnhetDTO = getEnhetJSON();
    var childEnhetResponse = post("/enhet/" + parentEnhetId + "/underenhet", childEnhetDTO);
    assertEquals(HttpStatus.CREATED, childEnhetResponse.getStatusCode());
    var insertedChildEnhetDTO = gson.fromJson(childEnhetResponse.getBody(), EnhetDTO.class);
    var childEnhetId = insertedChildEnhetDTO.getId();

    // Check that the childEnhet has the parentEnhet as parent
    childEnhetResponse = get("/enhet/" + childEnhetId);
    assertEquals(HttpStatus.OK, childEnhetResponse.getStatusCode());
    insertedChildEnhetDTO = gson.fromJson(childEnhetResponse.getBody(), EnhetDTO.class);
    assertEquals(parentEnhetId, insertedChildEnhetDTO.getParent().getId());

    // Check that the parent has one underenhet
    parentEnhetResponse = get("/enhet/" + parentEnhetId);
    assertEquals(HttpStatus.OK, parentEnhetResponse.getStatusCode());
    insertedParentEnhetDTO = gson.fromJson(parentEnhetResponse.getBody(), EnhetDTO.class);
    assertEquals(1, insertedParentEnhetDTO.getUnderenhet().size());

    // Delete the parent
    parentEnhetResponse = delete("/enhet/" + parentEnhetId);
    assertEquals(HttpStatus.OK, parentEnhetResponse.getStatusCode());

    // Check that the parent is deleted
    parentEnhetResponse = get("/enhet/" + parentEnhetId);
    assertEquals(HttpStatus.NOT_FOUND, parentEnhetResponse.getStatusCode());

    // Check that the childEnhet is deleted
    childEnhetResponse = get("/enhet/" + childEnhetId);
    assertEquals(HttpStatus.NOT_FOUND, childEnhetResponse.getStatusCode());
  }

  // Add and list underenheter using /underenhet endpoint
  @Test
  void addUnderenheter() throws Exception {
    var resultListType = new TypeToken<ResultList<EnhetDTO>>() {}.getType();
    var parentEnhetResponse = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    var parentEnhetDTO = gson.fromJson(parentEnhetResponse.getBody(), EnhetDTO.class);
    var parentId = parentEnhetDTO.getId();

    var child1EnhetResponse = post("/enhet/" + parentId + "/underenhet", getEnhetJSON());
    assertEquals(HttpStatus.CREATED, child1EnhetResponse.getStatusCode());
    var child1EnhetDTO = gson.fromJson(child1EnhetResponse.getBody(), EnhetDTO.class);
    var child2EnhetResponse = post("/enhet/" + parentId + "/underenhet", getEnhetJSON());
    assertEquals(HttpStatus.CREATED, child2EnhetResponse.getStatusCode());
    var child2EnhetDTO = gson.fromJson(child2EnhetResponse.getBody(), EnhetDTO.class);
    var child3EnhetResponse = post("/enhet/" + parentId + "/underenhet", getEnhetJSON());
    assertEquals(HttpStatus.CREATED, child3EnhetResponse.getStatusCode());
    var child3EnhetDTO = gson.fromJson(child3EnhetResponse.getBody(), EnhetDTO.class);
    var child4EnhetResponse = post("/enhet/" + parentId + "/underenhet", getEnhetJSON());
    assertEquals(HttpStatus.CREATED, child4EnhetResponse.getStatusCode());
    var child4EnhetDTO = gson.fromJson(child4EnhetResponse.getBody(), EnhetDTO.class);

    // List underenheter
    var underenheterResponse = get("/enhet/" + parentId + "/underenhet");
    assertEquals(HttpStatus.OK, underenheterResponse.getStatusCode());
    ResultList<EnhetDTO> underenheterDTO =
        gson.fromJson(underenheterResponse.getBody(), resultListType);
    var items = underenheterDTO.getItems();
    assertEquals(4, items.size());
    assertEquals(child4EnhetDTO.getId(), items.get(0).getId());
    assertEquals(child3EnhetDTO.getId(), items.get(1).getId());
    assertEquals(child2EnhetDTO.getId(), items.get(2).getId());
    assertEquals(child1EnhetDTO.getId(), items.get(3).getId());

    // Get ascending list
    underenheterResponse = get("/enhet/" + parentId + "/underenhet?sortOrder=asc");
    assertEquals(HttpStatus.OK, underenheterResponse.getStatusCode());
    underenheterDTO = gson.fromJson(underenheterResponse.getBody(), resultListType);
    items = underenheterDTO.getItems();
    assertEquals(4, underenheterDTO.getItems().size());
    assertEquals(child1EnhetDTO.getId(), items.get(0).getId());
    assertEquals(child2EnhetDTO.getId(), items.get(1).getId());
    assertEquals(child3EnhetDTO.getId(), items.get(2).getId());
    assertEquals(child4EnhetDTO.getId(), items.get(3).getId());

    // Get ascending list, startingAfter
    underenheterResponse =
        get(
            "/enhet/"
                + parentId
                + "/underenhet?sortOrder=asc&startingAfter="
                + child2EnhetDTO.getId());
    assertEquals(HttpStatus.OK, underenheterResponse.getStatusCode());
    underenheterDTO = gson.fromJson(underenheterResponse.getBody(), resultListType);
    items = underenheterDTO.getItems();
    assertEquals(2, underenheterDTO.getItems().size());
    assertEquals(child3EnhetDTO.getId(), items.get(0).getId());
    assertEquals(child4EnhetDTO.getId(), items.get(1).getId());

    // Get ascending list, endingBefore
    underenheterResponse =
        get(
            "/enhet/"
                + parentId
                + "/underenhet?sortOrder=asc&endingBefore="
                + child2EnhetDTO.getId());
    assertEquals(HttpStatus.OK, underenheterResponse.getStatusCode());
    underenheterDTO = gson.fromJson(underenheterResponse.getBody(), resultListType);
    items = underenheterDTO.getItems();
    assertEquals(1, underenheterDTO.getItems().size());
    assertEquals(child1EnhetDTO.getId(), items.get(0).getId());

    // Get descending list, startingAfter
    underenheterResponse =
        get(
            "/enhet/"
                + parentId
                + "/underenhet?sortOrder=desc&startingAfter="
                + child2EnhetDTO.getId());
    assertEquals(HttpStatus.OK, underenheterResponse.getStatusCode());
    underenheterDTO = gson.fromJson(underenheterResponse.getBody(), resultListType);
    items = underenheterDTO.getItems();
    assertEquals(1, underenheterDTO.getItems().size());
    assertEquals(child1EnhetDTO.getId(), items.get(0).getId());

    // Get descending list, endingBefore
    underenheterResponse =
        get(
            "/enhet/"
                + parentId
                + "/underenhet?sortOrder=desc&endingBefore="
                + child2EnhetDTO.getId());
    assertEquals(HttpStatus.OK, underenheterResponse.getStatusCode());
    underenheterDTO = gson.fromJson(underenheterResponse.getBody(), resultListType);
    items = underenheterDTO.getItems();
    assertEquals(2, underenheterDTO.getItems().size());
    assertEquals(child4EnhetDTO.getId(), items.get(0).getId());
    assertEquals(child3EnhetDTO.getId(), items.get(1).getId());

    // Get descending list, endingBefore
    underenheterResponse =
        get(
            "/enhet/"
                + parentId
                + "/underenhet?sortOrder=desc&endingBefore="
                + child1EnhetDTO.getId());
    assertEquals(HttpStatus.OK, underenheterResponse.getStatusCode());
    underenheterDTO = gson.fromJson(underenheterResponse.getBody(), resultListType);
    items = underenheterDTO.getItems();
    assertEquals(3, underenheterDTO.getItems().size());
    assertEquals(child4EnhetDTO.getId(), items.get(0).getId());
    assertEquals(child3EnhetDTO.getId(), items.get(1).getId());
    assertEquals(child2EnhetDTO.getId(), items.get(2).getId());

    // Delete
    assertEquals(HttpStatus.OK, delete("/enhet/" + parentEnhetDTO.getId()).getStatusCode());
  }

  // Test /enhet/{enhetId}/apiKey
  @Test
  @SuppressWarnings("java:S5961") // Allow 27 asserts
  void testEnhetApiKey() throws Exception {
    var enhetJSON = getEnhetJSON();
    var enhetResponse = post("/enhet/" + journalenhetId + "/underenhet", enhetJSON);
    assertEquals(HttpStatus.CREATED, enhetResponse.getStatusCode());
    var insertedEnhetDTO = gson.fromJson(enhetResponse.getBody(), EnhetDTO.class);
    var enhetId = insertedEnhetDTO.getId();

    // Add three API keys
    var response = post("/enhet/" + enhetId + "/apiKey", getApiKeyJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var apiKey1 = gson.fromJson(response.getBody(), ApiKeyDTO.class);
    response = post("/enhet/" + enhetId + "/apiKey", getApiKeyJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var apiKey2 = gson.fromJson(response.getBody(), ApiKeyDTO.class);
    response = post("/enhet/" + enhetId + "/apiKey", getApiKeyJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var apiKey3 = gson.fromJson(response.getBody(), ApiKeyDTO.class);

    // List API keys (DESC)
    var type = new TypeToken<ResultList<ApiKeyDTO>>() {}.getType();
    response = get("/enhet/" + enhetId + "/apiKey");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    ResultList<ApiKeyDTO> apiKeyList = gson.fromJson(response.getBody(), type);
    assertEquals(3, apiKeyList.getItems().size());
    assertEquals(apiKey1.getId(), apiKeyList.getItems().get(2).getId());
    assertEquals(apiKey2.getId(), apiKeyList.getItems().get(1).getId());
    assertEquals(apiKey3.getId(), apiKeyList.getItems().get(0).getId());

    // List API keys (DESC) startingAfter
    response = get("/enhet/" + enhetId + "/apiKey?startingAfter=" + apiKey2.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    apiKeyList = gson.fromJson(response.getBody(), type);
    assertEquals(1, apiKeyList.getItems().size());
    assertEquals(apiKey1.getId(), apiKeyList.getItems().get(0).getId());

    // List API keys (DESC) endingBefore
    response = get("/enhet/" + enhetId + "/apiKey?endingBefore=" + apiKey2.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    apiKeyList = gson.fromJson(response.getBody(), type);
    assertEquals(1, apiKeyList.getItems().size());
    assertEquals(apiKey3.getId(), apiKeyList.getItems().get(0).getId());

    // List API keys (ASC)
    response = get("/enhet/" + enhetId + "/apiKey?sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    apiKeyList = gson.fromJson(response.getBody(), type);
    assertEquals(3, apiKeyList.getItems().size());
    assertEquals(apiKey1.getId(), apiKeyList.getItems().get(0).getId());
    assertEquals(apiKey2.getId(), apiKeyList.getItems().get(1).getId());
    assertEquals(apiKey3.getId(), apiKeyList.getItems().get(2).getId());

    // List API keys (ASC) startingAfter
    response = get("/enhet/" + enhetId + "/apiKey?sortOrder=asc&startingAfter=" + apiKey2.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    apiKeyList = gson.fromJson(response.getBody(), type);
    assertEquals(1, apiKeyList.getItems().size());
    assertEquals(apiKey3.getId(), apiKeyList.getItems().get(0).getId());

    // List API keys (ASC) endingBefore
    response = get("/enhet/" + enhetId + "/apiKey?sortOrder=asc&endingBefore=" + apiKey2.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    apiKeyList = gson.fromJson(response.getBody(), type);
    assertEquals(1, apiKeyList.getItems().size());
    assertEquals(apiKey1.getId(), apiKeyList.getItems().get(0).getId());

    // Delete the enhet
    enhetResponse = delete("/enhet/" + enhetId);
    assertEquals(HttpStatus.OK, enhetResponse.getStatusCode());
  }

  // Test /enhet/{enhetId}/innsynskravDel
  @Test
  void testEnhetInnsynskravDel() throws Exception {
    // Add saksmappe with journalposts
    var response = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put(
        "journalpost",
        new JSONArray(List.of(getJournalpostJSON(), getJournalpostJSON(), getJournalpostJSON())));
    response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    var saksmappe = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    var journalpost1 = saksmappe.getJournalpost().get(0);
    var journalpost2 = saksmappe.getJournalpost().get(1);
    var journalpost3 = saksmappe.getJournalpost().get(2);

    // Add journalpost for another enhet
    response =
        post(
            "/saksmappe/" + saksmappe.getId() + "/journalpost",
            getJournalpostJSON(),
            journalenhet2Key);
    var journalpost4 = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Add four InnsynskravBestilling
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravDel = getInnsynskravDelJSON();
    innsynskravBestillingJSON.put("innsynskravDel", new JSONArray(List.of(innsynskravDel)));
    innsynskravDel.put("journalpost", journalpost1.getId());
    response = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestilling1DTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    var innsynskravDel1 = innsynskravBestilling1DTO.getInnsynskravDel().getFirst();
    innsynskravDel.put("journalpost", journalpost2.getId());
    response = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestilling2DTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    var innsynskravDel2 = innsynskravBestilling2DTO.getInnsynskravDel().getFirst();
    innsynskravDel.put("journalpost", journalpost3.getId());
    response = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestilling3DTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    var innsynskravDel3 = innsynskravBestilling3DTO.getInnsynskravDel().getFirst();
    innsynskravDel.put("journalpost", journalpost4.getId());
    response = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestilling4DTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    var innsynskravDel4 = innsynskravBestilling4DTO.getInnsynskravDel().getFirst();

    var type = new TypeToken<ResultList<InnsynskravDelDTO>>() {}.getType();

    // Check that journalenhet2 has one InnsynskravBestilling
    response = get("/enhet/" + journalenhet2Id + "/innsynskravDel", journalenhet2Key);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    ResultList<InnsynskravDelDTO> innsynskravDelList = gson.fromJson(response.getBody(), type);
    assertEquals(1, innsynskravDelList.getItems().size());
    assertEquals(innsynskravDel4.getId(), innsynskravDelList.getItems().get(0).getId());

    // List innsynskravDel (DESC)
    response = get("/enhet/" + journalenhetId + "/innsynskravDel");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravDelList = gson.fromJson(response.getBody(), type);
    assertEquals(3, innsynskravDelList.getItems().size());
    assertEquals(innsynskravDel1.getId(), innsynskravDelList.getItems().get(2).getId());
    assertEquals(innsynskravDel2.getId(), innsynskravDelList.getItems().get(1).getId());
    assertEquals(innsynskravDel3.getId(), innsynskravDelList.getItems().get(0).getId());

    // List innsynskravDel (DESC) startingAfter
    response =
        get(
            "/enhet/"
                + journalenhetId
                + "/innsynskravDel?startingAfter="
                + innsynskravDel2.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravDelList = gson.fromJson(response.getBody(), type);
    assertEquals(1, innsynskravDelList.getItems().size());
    assertEquals(innsynskravDel1.getId(), innsynskravDelList.getItems().get(0).getId());

    // List innsynskravDel (DESC) endingBefore
    response =
        get("/enhet/" + journalenhetId + "/innsynskravDel?endingBefore=" + innsynskravDel2.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravDelList = gson.fromJson(response.getBody(), type);
    assertEquals(1, innsynskravDelList.getItems().size());
    assertEquals(innsynskravDel3.getId(), innsynskravDelList.getItems().get(0).getId());

    // List innsynskravDel (ASC)
    response = get("/enhet/" + journalenhetId + "/innsynskravDel?sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravDelList = gson.fromJson(response.getBody(), type);
    assertEquals(3, innsynskravDelList.getItems().size());
    assertEquals(innsynskravDel1.getId(), innsynskravDelList.getItems().get(0).getId());
    assertEquals(innsynskravDel2.getId(), innsynskravDelList.getItems().get(1).getId());
    assertEquals(innsynskravDel3.getId(), innsynskravDelList.getItems().get(2).getId());

    // List innsynskravDel (ASC) startingAfter
    response =
        get(
            "/enhet/"
                + journalenhetId
                + "/innsynskravDel?sortOrder=asc&startingAfter="
                + innsynskravDel2.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravDelList = gson.fromJson(response.getBody(), type);
    assertEquals(1, innsynskravDelList.getItems().size());
    assertEquals(innsynskravDel3.getId(), innsynskravDelList.getItems().get(0).getId());

    // List innsynskravDel (ASC) endingBefore
    response =
        get(
            "/enhet/"
                + journalenhetId
                + "/innsynskravDel?sortOrder=asc&endingBefore="
                + innsynskravDel2.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravDelList = gson.fromJson(response.getBody(), type);
    assertEquals(1, innsynskravDelList.getItems().size());
    assertEquals(innsynskravDel1.getId(), innsynskravDelList.getItems().get(0).getId());

    // Clean up
    assertEquals(HttpStatus.OK, deleteAdmin("/arkiv/" + arkivDTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.OK,
        deleteAdmin("/innsynskravBestilling/" + innsynskravBestilling1DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.OK,
        deleteAdmin("/innsynskravBestilling/" + innsynskravBestilling2DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.OK,
        deleteAdmin("/innsynskravBestilling/" + innsynskravBestilling3DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.OK,
        deleteAdmin("/innsynskravBestilling/" + innsynskravBestilling4DTO.getId()).getStatusCode());
  }

  @Test
  void testEnhetArkiv() throws Exception {
    // Add three Arkiv
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkiv1 = gson.fromJson(response.getBody(), ArkivDTO.class);
    response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkiv2 = gson.fromJson(response.getBody(), ArkivDTO.class);
    response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkiv3 = gson.fromJson(response.getBody(), ArkivDTO.class);

    // Add Arkiv for another enhet
    response = post("/arkiv", getArkivJSON(), journalenhet2Key);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkiv4 = gson.fromJson(response.getBody(), ArkivDTO.class);

    var type = new TypeToken<ResultList<ArkivDTO>>() {}.getType();

    // Make sure journalenhet2 has one arkiv
    response = get("/enhet/" + journalenhet2Id + "/arkiv", journalenhet2Key);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    ResultList<ArkivDTO> arkivList = gson.fromJson(response.getBody(), type);
    assertEquals(1, arkivList.getItems().size());

    // List arkiv (DESC)
    response = get("/enhet/" + journalenhetId + "/arkiv");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    arkivList = gson.fromJson(response.getBody(), type);
    assertEquals(3, arkivList.getItems().size());
    assertEquals(arkiv1.getId(), arkivList.getItems().get(2).getId());
    assertEquals(arkiv2.getId(), arkivList.getItems().get(1).getId());
    assertEquals(arkiv3.getId(), arkivList.getItems().get(0).getId());

    // List arkiv (DESC) startingAfter
    response = get("/enhet/" + journalenhetId + "/arkiv?startingAfter=" + arkiv2.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    arkivList = gson.fromJson(response.getBody(), type);
    assertEquals(1, arkivList.getItems().size());
    assertEquals(arkiv1.getId(), arkivList.getItems().get(0).getId());

    // List arkiv (DESC) endingBefore
    response = get("/enhet/" + journalenhetId + "/arkiv?endingBefore=" + arkiv2.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    arkivList = gson.fromJson(response.getBody(), type);
    assertEquals(1, arkivList.getItems().size());
    assertEquals(arkiv3.getId(), arkivList.getItems().get(0).getId());

    // List arkiv (ASC)
    response = get("/enhet/" + journalenhetId + "/arkiv?sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    arkivList = gson.fromJson(response.getBody(), type);
    assertEquals(3, arkivList.getItems().size());
    assertEquals(arkiv1.getId(), arkivList.getItems().get(0).getId());
    assertEquals(arkiv2.getId(), arkivList.getItems().get(1).getId());
    assertEquals(arkiv3.getId(), arkivList.getItems().get(2).getId());

    // List arkiv (ASC) startingAfter
    response =
        get("/enhet/" + journalenhetId + "/arkiv?sortOrder=asc&startingAfter=" + arkiv2.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    arkivList = gson.fromJson(response.getBody(), type);
    assertEquals(1, arkivList.getItems().size());
    assertEquals(arkiv3.getId(), arkivList.getItems().get(0).getId());

    // List arkiv (ASC) endingBefore
    response =
        get("/enhet/" + journalenhetId + "/arkiv?sortOrder=asc&endingBefore=" + arkiv2.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    arkivList = gson.fromJson(response.getBody(), type);
    assertEquals(1, arkivList.getItems().size());
    assertEquals(arkiv1.getId(), arkivList.getItems().get(0).getId());

    // Clean up
    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkiv1.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkiv2.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkiv3.getId()).getStatusCode());
    assertEquals(
        HttpStatus.OK, delete("/arkiv/" + arkiv4.getId(), journalenhet2Key).getStatusCode());
  }

  // Support enhets with semicolon-separated enhetskode list
  @Test
  @SuppressWarnings("java:S5961") // Allow many asserts
  void addEnhetWithEnhetskodeList() throws Exception {
    var enhetJSON = getEnhetJSON();
    enhetJSON.put("enhetskode", "A;B;C  ;  D  ;EFG");
    enhetJSON.put("parent", journalenhetId);
    var underenhet1JSON = getEnhetJSON();
    underenhet1JSON.put("enhetskode", "test 1; test-2 ; test3");
    var underenhet2JSON = getEnhetJSON();
    underenhet2JSON.put("enhetskode", "foo;bar;baz,qux");
    var underenhet3JSON = getEnhetJSON();
    underenhet3JSON.put("enhetskode", "single");
    var underenhet4JSON = getEnhetJSON();
    underenhet4JSON.put("enhetskode", " untrimmed ");

    enhetJSON.put(
        "underenhet",
        new JSONArray(List.of(underenhet1JSON, underenhet2JSON, underenhet3JSON, underenhet4JSON)));
    var enhetResponse = post("/enhet/" + journalenhetId + "/underenhet", enhetJSON);
    assertEquals(HttpStatus.CREATED, enhetResponse.getStatusCode());
    var insertedEnhetDTO = gson.fromJson(enhetResponse.getBody(), EnhetDTO.class);
    assertEquals(
        enhetJSON.get("enhetstype").toString(), insertedEnhetDTO.getEnhetstype().toString());
    var enhetId = insertedEnhetDTO.getId();
    var sub1Id = insertedEnhetDTO.getUnderenhet().get(0).getId();
    var sub2Id = insertedEnhetDTO.getUnderenhet().get(1).getId();
    var sub3Id = insertedEnhetDTO.getUnderenhet().get(2).getId();
    var sub4Id = insertedEnhetDTO.getUnderenhet().get(3).getId();

    // Check that we can get the new enhet from the API
    enhetResponse = get("/enhet/" + enhetId);
    assertEquals(HttpStatus.OK, enhetResponse.getStatusCode());

    var arkivJSON = getArkivJSON();
    var arkivResponse = post("/arkiv", arkivJSON);
    var arkivDTO = gson.fromJson(arkivResponse.getBody(), ArkivDTO.class);

    // Add a saksmappe with one of the enhetskoder in administrativEnhet
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("administrativEnhet", "A");
    var saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    var saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(enhetId, saksmappe.getAdministrativEnhetObjekt().getId());

    saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("administrativEnhet", "A");
    saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(enhetId, saksmappe.getAdministrativEnhetObjekt().getId());

    saksmappeJSON.put("administrativEnhet", "B");
    saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(enhetId, saksmappe.getAdministrativEnhetObjekt().getId());

    saksmappeJSON.put("administrativEnhet", "C");
    saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(enhetId, saksmappe.getAdministrativEnhetObjekt().getId());

    saksmappeJSON.put("administrativEnhet", "D");
    saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(enhetId, saksmappe.getAdministrativEnhetObjekt().getId());

    saksmappeJSON.put("administrativEnhet", "EFG");
    saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(enhetId, saksmappe.getAdministrativEnhetObjekt().getId());

    // Add a journalpost with one of the enhetskoder in underenhet
    saksmappeJSON.put("administrativEnhet", "test 1");
    saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(sub1Id, saksmappe.getAdministrativEnhetObjekt().getId());

    saksmappeJSON.put("administrativEnhet", "test-2");
    saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(sub1Id, saksmappe.getAdministrativEnhetObjekt().getId());

    saksmappeJSON.put("administrativEnhet", "test3");
    saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(sub1Id, saksmappe.getAdministrativEnhetObjekt().getId());

    saksmappeJSON.put("administrativEnhet", "foo");
    saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(sub2Id, saksmappe.getAdministrativEnhetObjekt().getId());

    saksmappeJSON.put("administrativEnhet", "bar");
    saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(sub2Id, saksmappe.getAdministrativEnhetObjekt().getId());

    saksmappeJSON.put("administrativEnhet", "baz,qux");
    saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(sub2Id, saksmappe.getAdministrativEnhetObjekt().getId());

    saksmappeJSON.put("administrativEnhet", "single");
    saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(sub3Id, saksmappe.getAdministrativEnhetObjekt().getId());

    saksmappeJSON.put("administrativEnhet", "untrimmed");
    saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(sub4Id, saksmappe.getAdministrativEnhetObjekt().getId());

    // Wrong enhetskoder should not match
    saksmappeJSON.put("administrativEnhet", "wrong");
    saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(journalenhetId, saksmappe.getAdministrativEnhetObjekt().getId());

    saksmappeJSON.put("administrativEnhet", "baz");
    saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(journalenhetId, saksmappe.getAdministrativEnhetObjekt().getId());

    saksmappeJSON.put("administrativEnhet", "foo,bar");
    saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(journalenhetId, saksmappe.getAdministrativEnhetObjekt().getId());

    // Delete the enhet
    enhetResponse = delete("/enhet/" + enhetId);
    assertEquals(HttpStatus.OK, enhetResponse.getStatusCode());
    assertEquals(Boolean.TRUE, gson.fromJson(enhetResponse.getBody(), EnhetDTO.class).getDeleted());

    // Delete the Arkiv
    delete("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void testEnhetskodeWithRegexCharacters() throws Exception {
    var enhetJSON = getEnhetJSON();
    enhetJSON.put("enhetskode", "Enhet");
    enhetJSON.put("parent", journalenhetId);
    var underenhet1JSON = getEnhetJSON();
    underenhet1JSON.put("enhetskode", "A;(B);\\(C  ;  [D]  ;E{F}G;*|?+.");

    enhetJSON.put("underenhet", new JSONArray(List.of(underenhet1JSON)));
    var enhetResponse = post("/enhet/" + journalenhetId + "/underenhet", enhetJSON);
    assertEquals(HttpStatus.CREATED, enhetResponse.getStatusCode());
    var insertedEnhetDTO = gson.fromJson(enhetResponse.getBody(), EnhetDTO.class);
    assertEquals(
        enhetJSON.get("enhetstype").toString(), insertedEnhetDTO.getEnhetstype().toString());
    var enhetId = insertedEnhetDTO.getId();
    var sub1Id = insertedEnhetDTO.getUnderenhet().get(0).getId();

    // Check that we can get the new enhet from the API
    enhetResponse = get("/enhet/" + enhetId);
    assertEquals(HttpStatus.OK, enhetResponse.getStatusCode());

    var arkivJSON = getArkivJSON();
    var arkivResponse = post("/arkiv", arkivJSON);
    var arkivDTO = gson.fromJson(arkivResponse.getBody(), ArkivDTO.class);

    // Add a saksmappe with one of the enhetskoder in administr
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("administrativEnhet", "Enhet");
    var saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    var saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(enhetId, saksmappe.getAdministrativEnhetObjekt().getId());

    saksmappeJSON.put("administrativEnhet", "A");
    saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(sub1Id, saksmappe.getAdministrativEnhetObjekt().getId());

    saksmappeJSON.put("administrativEnhet", "(B)");
    saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(sub1Id, saksmappe.getAdministrativEnhetObjekt().getId());

    saksmappeJSON.put("administrativEnhet", "\\(C");
    saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(sub1Id, saksmappe.getAdministrativEnhetObjekt().getId());

    saksmappeJSON.put("administrativEnhet", "[D]");
    saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(sub1Id, saksmappe.getAdministrativEnhetObjekt().getId());

    saksmappeJSON.put("administrativEnhet", "E{F}G");
    saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(sub1Id, saksmappe.getAdministrativEnhetObjekt().getId());

    saksmappeJSON.put("administrativEnhet", "*|?+.");
    saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe.getId());
    assertEquals(sub1Id, saksmappe.getAdministrativEnhetObjekt().getId());

    delete("/arkiv/" + arkivDTO.getId());
    delete("/enhet/" + enhetId);
  }
}
