package no.einnsyn.apiv3.entities.enhet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.reflect.TypeToken;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class EnhetControllerTest extends EinnsynControllerTestBase {

  @Test
  void insertEnhet() throws Exception {
    JSONObject enhetJSON = getEnhetJSON();
    ResponseEntity<String> enhetResponse = post("/enhet", enhetJSON);
    assertEquals(HttpStatus.CREATED, enhetResponse.getStatusCode());
    EnhetDTO insertedEnhetDTO = gson.fromJson(enhetResponse.getBody(), EnhetDTO.class);
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
    enhetResponse = put("/enhet/" + enhetId, enhetJSON);
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
    JSONObject parentEnhetDTO = getEnhetJSON();
    ResponseEntity<String> parentEnhetResponse = post("/enhet", parentEnhetDTO);
    assertEquals(HttpStatus.CREATED, parentEnhetResponse.getStatusCode());
    EnhetDTO insertedParentEnhetDTO = gson.fromJson(parentEnhetResponse.getBody(), EnhetDTO.class);
    String parentEnhetId = insertedParentEnhetDTO.getId();

    JSONObject childEnhetDTO = getEnhetJSON();
    childEnhetDTO.put("parent", parentEnhetId);
    ResponseEntity<String> childEnhetResponse = post("/enhet", childEnhetDTO);
    assertEquals(HttpStatus.CREATED, childEnhetResponse.getStatusCode());
    EnhetDTO insertedChildEnhetDTO = gson.fromJson(childEnhetResponse.getBody(), EnhetDTO.class);
    String childEnhetId = insertedChildEnhetDTO.getId();

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

  /** Add new enhet, update it later with "parent" field */
  @Test
  void updateUnderenhetWithParent() throws Exception {
    JSONObject parentEnhetDTO = getEnhetJSON();
    ResponseEntity<String> parentEnhetResponse = post("/enhet", parentEnhetDTO);
    assertEquals(HttpStatus.CREATED, parentEnhetResponse.getStatusCode());
    EnhetDTO insertedParentEnhetDTO = gson.fromJson(parentEnhetResponse.getBody(), EnhetDTO.class);
    String parentEnhetId = insertedParentEnhetDTO.getId();

    JSONObject childEnhetDTO = getEnhetJSON();
    childEnhetDTO.put("orgnummer", "112345678");
    ResponseEntity<String> childEnhetResponse = post("/enhet", childEnhetDTO);
    assertEquals(HttpStatus.CREATED, childEnhetResponse.getStatusCode());
    EnhetDTO insertedChildEnhetDTO = gson.fromJson(childEnhetResponse.getBody(), EnhetDTO.class);
    String childEnhetId = insertedChildEnhetDTO.getId();

    // Check that the childEnhet has no parent
    childEnhetResponse = get("/enhet/" + childEnhetId);
    assertEquals(HttpStatus.OK, childEnhetResponse.getStatusCode());
    insertedChildEnhetDTO = gson.fromJson(childEnhetResponse.getBody(), EnhetDTO.class);
    assertEquals(null, insertedChildEnhetDTO.getParent());

    // Check that the parent has no underenhets
    parentEnhetResponse = get("/enhet/" + parentEnhetId);
    assertEquals(HttpStatus.OK, parentEnhetResponse.getStatusCode());
    insertedParentEnhetDTO = gson.fromJson(parentEnhetResponse.getBody(), EnhetDTO.class);
    assertEquals(0, insertedParentEnhetDTO.getUnderenhet().size());

    // Update the childEnhet with parent
    childEnhetDTO = new JSONObject();
    childEnhetDTO.put("parent", parentEnhetId);
    childEnhetResponse = put("/enhet/" + childEnhetId, childEnhetDTO);
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
  @SuppressWarnings("unchecked")
  void addUnderenheter() throws Exception {
    var resultListType = new TypeToken<ResultList<EnhetDTO>>() {}.getType();
    var parentEnhetResponse = post("/enhet", getEnhetJSON());
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
    var underenheterDTO =
        (ResultList<EnhetDTO>) gson.fromJson(underenheterResponse.getBody(), resultListType);
    var items = underenheterDTO.getItems();
    assertEquals(4, items.size());
    assertEquals(child4EnhetDTO.getId(), items.get(0).getId());
    assertEquals(child3EnhetDTO.getId(), items.get(1).getId());
    assertEquals(child2EnhetDTO.getId(), items.get(2).getId());
    assertEquals(child1EnhetDTO.getId(), items.get(3).getId());

    // Get ascending list
    underenheterResponse = get("/enhet/" + parentId + "/underenhet?sortOrder=asc");
    assertEquals(HttpStatus.OK, underenheterResponse.getStatusCode());
    underenheterDTO =
        (ResultList<EnhetDTO>) gson.fromJson(underenheterResponse.getBody(), resultListType);
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
    underenheterDTO =
        (ResultList<EnhetDTO>) gson.fromJson(underenheterResponse.getBody(), resultListType);
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
    underenheterDTO =
        (ResultList<EnhetDTO>) gson.fromJson(underenheterResponse.getBody(), resultListType);
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
    underenheterDTO =
        (ResultList<EnhetDTO>) gson.fromJson(underenheterResponse.getBody(), resultListType);
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
    underenheterDTO =
        (ResultList<EnhetDTO>) gson.fromJson(underenheterResponse.getBody(), resultListType);
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
    underenheterDTO =
        (ResultList<EnhetDTO>) gson.fromJson(underenheterResponse.getBody(), resultListType);
    items = underenheterDTO.getItems();
    assertEquals(3, underenheterDTO.getItems().size());
    assertEquals(child4EnhetDTO.getId(), items.get(0).getId());
    assertEquals(child3EnhetDTO.getId(), items.get(1).getId());
    assertEquals(child2EnhetDTO.getId(), items.get(2).getId());
  }
}
