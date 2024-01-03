package no.einnsyn.apiv3.entities.enhet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import no.einnsyn.apiv3.entities.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.enhet.models.EnhetJSON;
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
    EnhetJSON insertedEnhetJSON = gson.fromJson(enhetResponse.getBody(), EnhetJSON.class);
    assertEquals(enhetJSON.get("navn"), insertedEnhetJSON.getNavn());
    assertEquals(enhetJSON.get("navnNynorsk"), insertedEnhetJSON.getNavnNynorsk());
    assertEquals(enhetJSON.get("navnEngelsk"), insertedEnhetJSON.getNavnEngelsk());
    assertEquals(enhetJSON.get("navnSami"), insertedEnhetJSON.getNavnSami());
    assertEquals(
        LocalDate.parse(enhetJSON.get("avsluttetDato").toString()),
        insertedEnhetJSON.getAvsluttetDato());
    assertEquals(enhetJSON.get("innsynskravEpost"), insertedEnhetJSON.getInnsynskravEpost());
    assertEquals(enhetJSON.get("kontaktpunktAdresse"), insertedEnhetJSON.getKontaktpunktAdresse());
    assertEquals(enhetJSON.get("kontaktpunktEpost"), insertedEnhetJSON.getKontaktpunktEpost());
    assertEquals(enhetJSON.get("kontaktpunktTelefon"), insertedEnhetJSON.getKontaktpunktTelefon());
    assertEquals(enhetJSON.get("orgnummer"), insertedEnhetJSON.getOrgnummer());
    assertEquals(enhetJSON.get("enhetskode"), insertedEnhetJSON.getEnhetskode());
    assertEquals(
        enhetJSON.get("enhetstype").toString(), insertedEnhetJSON.getEnhetstype().toString());
    assertEquals(enhetJSON.get("skjult"), insertedEnhetJSON.getSkjult());
    assertEquals(
        LocalDate.parse(enhetJSON.get("avsluttetDato").toString()),
        insertedEnhetJSON.getAvsluttetDato());
    String enhetId = insertedEnhetJSON.getId();

    // Check that we can get the new enhet from the API
    enhetResponse = get("/enhet/" + enhetId);
    assertEquals(HttpStatus.OK, enhetResponse.getStatusCode());

    // Check that we can update the enhet
    enhetJSON.put("navn", "updatedNavn");
    enhetResponse = put("/enhet/" + enhetId, enhetJSON);
    assertEquals(HttpStatus.OK, enhetResponse.getStatusCode());
    insertedEnhetJSON = gson.fromJson(enhetResponse.getBody(), EnhetJSON.class);
    assertEquals(enhetJSON.get("navn"), insertedEnhetJSON.getNavn());

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
    JSONObject parentEnhetJSON = getEnhetJSON();
    ResponseEntity<String> parentEnhetResponse = post("/enhet", parentEnhetJSON);
    assertEquals(HttpStatus.CREATED, parentEnhetResponse.getStatusCode());
    EnhetJSON insertedParentEnhetJSON =
        gson.fromJson(parentEnhetResponse.getBody(), EnhetJSON.class);
    String parentEnhetId = insertedParentEnhetJSON.getId();

    JSONObject childEnhetJSON = getEnhetJSON();
    childEnhetJSON.put("parent", parentEnhetId);
    ResponseEntity<String> childEnhetResponse = post("/enhet", childEnhetJSON);
    assertEquals(HttpStatus.CREATED, childEnhetResponse.getStatusCode());
    EnhetJSON insertedChildEnhetJSON = gson.fromJson(childEnhetResponse.getBody(), EnhetJSON.class);
    String childEnhetId = insertedChildEnhetJSON.getId();

    // Check that the childEnhet has the parentEnhet as parent
    childEnhetResponse = get("/enhet/" + childEnhetId);
    assertEquals(HttpStatus.OK, childEnhetResponse.getStatusCode());
    insertedChildEnhetJSON = gson.fromJson(childEnhetResponse.getBody(), EnhetJSON.class);
    assertEquals(parentEnhetId, insertedChildEnhetJSON.getParent().getId());

    // Check that the parent has one underenhet
    parentEnhetResponse = get("/enhet/" + parentEnhetId);
    assertEquals(HttpStatus.OK, parentEnhetResponse.getStatusCode());
    insertedParentEnhetJSON = gson.fromJson(parentEnhetResponse.getBody(), EnhetJSON.class);
    assertEquals(1, insertedParentEnhetJSON.getUnderenhet().size());

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
    JSONObject parentEnhetJSON = getEnhetJSON();
    ResponseEntity<String> parentEnhetResponse = post("/enhet", parentEnhetJSON);
    assertEquals(HttpStatus.CREATED, parentEnhetResponse.getStatusCode());
    EnhetJSON insertedParentEnhetJSON =
        gson.fromJson(parentEnhetResponse.getBody(), EnhetJSON.class);
    String parentEnhetId = insertedParentEnhetJSON.getId();

    JSONObject childEnhetJSON = getEnhetJSON();
    childEnhetJSON.put("orgnummer", "112345678");
    ResponseEntity<String> childEnhetResponse = post("/enhet", childEnhetJSON);
    assertEquals(HttpStatus.CREATED, childEnhetResponse.getStatusCode());
    EnhetJSON insertedChildEnhetJSON = gson.fromJson(childEnhetResponse.getBody(), EnhetJSON.class);
    String childEnhetId = insertedChildEnhetJSON.getId();

    // Check that the childEnhet has no parent
    childEnhetResponse = get("/enhet/" + childEnhetId);
    assertEquals(HttpStatus.OK, childEnhetResponse.getStatusCode());
    insertedChildEnhetJSON = gson.fromJson(childEnhetResponse.getBody(), EnhetJSON.class);
    assertEquals(null, insertedChildEnhetJSON.getParent());

    // Check that the parent has no underenhets
    parentEnhetResponse = get("/enhet/" + parentEnhetId);
    assertEquals(HttpStatus.OK, parentEnhetResponse.getStatusCode());
    insertedParentEnhetJSON = gson.fromJson(parentEnhetResponse.getBody(), EnhetJSON.class);
    assertEquals(0, insertedParentEnhetJSON.getUnderenhet().size());

    // Update the childEnhet with parent
    childEnhetJSON = new JSONObject();
    childEnhetJSON.put("parent", parentEnhetId);
    childEnhetResponse = put("/enhet/" + childEnhetId, childEnhetJSON);
    assertEquals(HttpStatus.OK, childEnhetResponse.getStatusCode());
    insertedChildEnhetJSON = gson.fromJson(childEnhetResponse.getBody(), EnhetJSON.class);
    assertEquals(parentEnhetId, insertedChildEnhetJSON.getParent().getId());

    // Check that the parent has one underenhet
    parentEnhetResponse = get("/enhet/" + parentEnhetId);
    assertEquals(HttpStatus.OK, parentEnhetResponse.getStatusCode());
    insertedParentEnhetJSON = gson.fromJson(parentEnhetResponse.getBody(), EnhetJSON.class);
    assertEquals(1, insertedParentEnhetJSON.getUnderenhet().size());

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
}
