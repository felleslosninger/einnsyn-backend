package no.einnsyn.backend.entities.lagretsak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.authentication.bruker.models.TokenResponse;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.lagretsak.models.LagretSakDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.saksmappe.models.Saksmappe;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.hibernate.SessionFactory;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.shaded.com.google.common.reflect.TypeToken;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LagretSakControllerTest extends EinnsynControllerTestBase {

  @Autowired private EntityManagerFactory entityManagerFactory;

  BrukerDTO brukerDTO;
  ArkivDTO arkivDTO;
  ArkivdelDTO arkivdelDTO;
  SaksmappeDTO saksmappeDTO;
  MoetemappeDTO moetemappeDTO;
  String accessToken;

  @BeforeAll
  void setup() throws Exception {
    // Create user
    var brukerJSON = getBrukerJSON();
    var response = post("/bruker", brukerJSON);
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

    // Create arkiv
    response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    // Create Arkivdel
    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    // Create saksmappe
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Create moetemappe
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
  }

  @AfterAll
  void cleanup() throws Exception {
    deleteAdmin("/bruker/" + brukerDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, getAdmin("/bruker/" + brukerDTO.getId()).getStatusCode());

    delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, getAdmin("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }

  @Test
  void testLagretSakLifecycle() throws Exception {
    var lagretSakJSON = getLagretSakJSON();
    lagretSakJSON.put("saksmappe", saksmappeDTO.getId());

    // Unauthorized add, not logged in. A caller who may not see the Bruker is answered as if it
    // did not exist, so that {id} cannot be used to look up e-mail addresses.
    var response = post("/bruker/" + brukerDTO.getId() + "/lagretSak", lagretSakJSON);
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    // Logged in
    response = post("/bruker/" + brukerDTO.getId() + "/lagretSak", lagretSakJSON, accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var lagretSakDTO = gson.fromJson(response.getBody(), LagretSakDTO.class);

    // Unauthorized get, not logged in
    response = get("/lagretSak/" + lagretSakDTO.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Get
    response = get("/lagretSak/" + lagretSakDTO.getId(), accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Unauthorized delete, not logged in
    response = deleteAnon("/lagretSak/" + lagretSakDTO.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Delete
    response = delete("/lagretSak/" + lagretSakDTO.getId(), accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Get
    response = get("/lagretSak/" + lagretSakDTO.getId(), accessToken);
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void testMappeShowsOwnLagretSak() throws Exception {
    // A second user, who has saved another Saksmappe
    var bruker2JSON = getBrukerJSON();
    var response = post("/bruker", bruker2JSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var bruker2DTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    var bruker2Secret = brukerService.find(bruker2DTO.getId()).getSecret();
    response = patch("/bruker/" + bruker2DTO.getId() + "/activate/" + bruker2Secret);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = post("/auth/token", getLoginJSON(bruker2JSON));
    var bruker2Token = gson.fromJson(response.getBody(), TokenResponse.class).getToken();

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var otherSaksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    var saksmappeLagretSakDTO = addLagretSak(brukerDTO, accessToken, "saksmappe", saksmappeDTO);
    var moetemappeLagretSakDTO = addLagretSak(brukerDTO, accessToken, "moetemappe", moetemappeDTO);
    var otherLagretSakDTO = addLagretSak(bruker2DTO, bruker2Token, "saksmappe", otherSaksmappeDTO);
    // Saved after the first user's, so it is only first in the collection when filtered
    var otherMoetemappeLagretSakDTO =
        addLagretSak(bruker2DTO, bruker2Token, "moetemappe", moetemappeDTO);
    esClient.indices().refresh(r -> r.index(elasticsearchIndex));
    var listType = new TypeToken<PaginatedList<SaksmappeDTO>>() {}.getType();

    // Search: only the user's own LagretSak
    response =
        get(
            "/search?entity=Saksmappe&saksmappe="
                + saksmappeDTO.getId()
                + "&saksmappe="
                + otherSaksmappeDTO.getId(),
            accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<SaksmappeDTO> searchResult = gson.fromJson(response.getBody(), listType);
    assertEquals(2, searchResult.getItems().size());
    for (var dto : searchResult.getItems()) {
      var expected = dto.getId().equals(saksmappeDTO.getId()) ? saksmappeLagretSakDTO : null;
      assertEquals(getId(expected), getId(dto.getLagretSak()));
    }

    // List
    response = get("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", bruker2Token);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<SaksmappeDTO> listResult = gson.fromJson(response.getBody(), listType);
    for (var dto : listResult.getItems()) {
      var expected = dto.getId().equals(otherSaksmappeDTO.getId()) ? otherLagretSakDTO : null;
      assertEquals(getId(expected), getId(dto.getLagretSak()));
    }

    // Get Moetemappe, with expand
    response = get("/moetemappe/" + moetemappeDTO.getId() + "?expand=lagretSak", bruker2Token);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var expandedLagretSak =
        gson.fromJson(response.getBody(), MoetemappeDTO.class).getLagretSak().getExpandedObject();
    assertEquals(otherMoetemappeLagretSakDTO.getId(), expandedLagretSak.getId());
    assertEquals(moetemappeDTO.getId(), expandedLagretSak.getMoetemappe().getId());

    // Not set for anonymous users or Enhets
    response = getAnon("/saksmappe/" + saksmappeDTO.getId());
    assertNull(gson.fromJson(response.getBody(), SaksmappeDTO.class).getLagretSak());
    response = get("/saksmappe/" + saksmappeDTO.getId());
    assertNull(gson.fromJson(response.getBody(), SaksmappeDTO.class).getLagretSak());

    for (var lagretSakDTO :
        List.of(
            saksmappeLagretSakDTO,
            moetemappeLagretSakDTO,
            otherLagretSakDTO,
            otherMoetemappeLagretSakDTO)) {
      assertEquals(
          HttpStatus.OK, deleteAdmin("/lagretSak/" + lagretSakDTO.getId()).getStatusCode());
    }
    assertEquals(HttpStatus.OK, delete("/saksmappe/" + otherSaksmappeDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, deleteAdmin("/bruker/" + bruker2DTO.getId()).getStatusCode());
  }

  @Test
  void testExpandedMappeShowsOwnLagretSak() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var otherSaksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var journalpostIds = new ArrayList<String>();
    for (var saksmappe : List.of(saksmappeDTO, saksmappeDTO, otherSaksmappeDTO)) {
      response = post("/saksmappe/" + saksmappe.getId() + "/journalpost", getJournalpostJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      journalpostIds.add(gson.fromJson(response.getBody(), JournalpostDTO.class).getId());
    }
    var lagretSakDTO = addLagretSak(brukerDTO, accessToken, "saksmappe", saksmappeDTO);
    esClient.indices().refresh(r -> r.index(elasticsearchIndex));

    // Search: every hit's Saksmappe has its LagretSak, batch loaded with one query for the page
    var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();
    response =
        get(
            "/search?entity=Journalpost&saksmappe="
                + saksmappeDTO.getId()
                + "&saksmappe="
                + otherSaksmappeDTO.getId()
                + "&expand=saksmappe.lagretSak",
            accessToken);
    var lagretSakStatistics =
        statistics.getCollectionStatistics(Saksmappe.class.getName() + ".lagretSak");
    var lagretSakFetchCount = lagretSakStatistics.getFetchCount();
    var lagretSakLoadCount = lagretSakStatistics.getLoadCount();
    statistics.setStatisticsEnabled(false);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var journalpostListType = new TypeToken<PaginatedList<JournalpostDTO>>() {}.getType();
    PaginatedList<JournalpostDTO> searchResult =
        gson.fromJson(response.getBody(), journalpostListType);
    assertEquals(3, searchResult.getItems().size());
    for (var dto : searchResult.getItems()) {
      var saksmappe = dto.getSaksmappe().getExpandedObject();
      if (saksmappe.getId().equals(saksmappeDTO.getId())) {
        assertEquals(lagretSakDTO.getId(), saksmappe.getLagretSak().getExpandedObject().getId());
      } else {
        assertNull(saksmappe.getLagretSak());
      }
    }
    assertEquals(2, lagretSakLoadCount);
    assertEquals(1, lagretSakFetchCount);

    assertEquals(HttpStatus.OK, deleteAdmin("/lagretSak/" + lagretSakDTO.getId()).getStatusCode());
    for (var journalpostId : journalpostIds) {
      assertEquals(HttpStatus.OK, delete("/journalpost/" + journalpostId).getStatusCode());
    }
    assertEquals(HttpStatus.OK, delete("/saksmappe/" + otherSaksmappeDTO.getId()).getStatusCode());
  }

  private LagretSakDTO addLagretSak(
      BrukerDTO bruker, String token, String mappeProperty, BaseDTO mappe) throws Exception {
    var lagretSakJSON = getLagretSakJSON();
    lagretSakJSON.put(mappeProperty, mappe.getId());
    var response = post("/bruker/" + bruker.getId() + "/lagretSak", lagretSakJSON, token);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    return gson.fromJson(response.getBody(), LagretSakDTO.class);
  }

  private String getId(LagretSakDTO lagretSakDTO) {
    return lagretSakDTO != null ? lagretSakDTO.getId() : null;
  }

  private String getId(ExpandableField<LagretSakDTO> lagretSakField) {
    return lagretSakField != null ? lagretSakField.getId() : null;
  }

  // Only the owner can update a LagretSak
  @Test
  void testLagretSakUpdateAuthorization() throws Exception {
    var lagretSakJSON = getLagretSakJSON();
    lagretSakJSON.put("saksmappe", saksmappeDTO.getId());
    var response = post("/bruker/" + brukerDTO.getId() + "/lagretSak", lagretSakJSON, accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var lagretSakDTO = gson.fromJson(response.getBody(), LagretSakDTO.class);

    // Unauthorized update, not logged in
    response = patchAnon("/lagretSak/" + lagretSakDTO.getId(), new JSONObject());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // An Enhet cannot update
    response = patch("/lagretSak/" + lagretSakDTO.getId(), new JSONObject());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Admin cannot update, only the owner can
    response = patchAdmin("/lagretSak/" + lagretSakDTO.getId(), new JSONObject());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // The owner can update
    response = patch("/lagretSak/" + lagretSakDTO.getId(), new JSONObject(), accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Clean up
    response = delete("/lagretSak/" + lagretSakDTO.getId(), accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testLagretSakPagination() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    var saksmappe1DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe1DTO.getId());

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    var saksmappe2DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe2DTO.getId());

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    var saksmappe3DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe3DTO.getId());

    var lagretSakJSON = getLagretSakJSON();
    lagretSakJSON.put("saksmappe", saksmappe1DTO.getId());
    response = post("/bruker/" + brukerDTO.getId() + "/lagretSak", lagretSakJSON, accessToken);
    var lagretSak1DTO = gson.fromJson(response.getBody(), LagretSakDTO.class);
    assertNotNull(lagretSak1DTO.getId());

    lagretSakJSON.put("saksmappe", saksmappe2DTO.getId());
    response = post("/bruker/" + brukerDTO.getId() + "/lagretSak", lagretSakJSON, accessToken);
    var lagretSak2DTO = gson.fromJson(response.getBody(), LagretSakDTO.class);
    assertNotNull(lagretSak2DTO.getId());

    lagretSakJSON.put("saksmappe", saksmappe3DTO.getId());
    response = post("/bruker/" + brukerDTO.getId() + "/lagretSak", lagretSakJSON, accessToken);
    var lagretSak3DTO = gson.fromJson(response.getBody(), LagretSakDTO.class);
    assertNotNull(lagretSak3DTO.getId());

    var type = new TypeToken<PaginatedList<LagretSakDTO>>() {}.getType();
    PaginatedList<LagretSakDTO> resultList;

    // DESC
    response = get("/bruker/" + brukerDTO.getId() + "/lagretSak", accessToken);
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(3, resultList.getItems().size());
    assertEquals(lagretSak3DTO.getId(), resultList.getItems().get(0).getId());
    assertEquals(lagretSak2DTO.getId(), resultList.getItems().get(1).getId());
    assertEquals(lagretSak1DTO.getId(), resultList.getItems().get(2).getId());

    // DESC startingAfter
    response =
        get(
            "/bruker/" + brukerDTO.getId() + "/lagretSak?startingAfter=" + lagretSak2DTO.getId(),
            accessToken);
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(lagretSak1DTO.getId(), resultList.getItems().get(0).getId());

    // DESC endingBefore
    response =
        get(
            "/bruker/" + brukerDTO.getId() + "/lagretSak?endingBefore=" + lagretSak2DTO.getId(),
            accessToken);
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(lagretSak3DTO.getId(), resultList.getItems().get(0).getId());

    // ASC
    response = get("/bruker/" + brukerDTO.getId() + "/lagretSak?sortOrder=asc", accessToken);
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(3, resultList.getItems().size());
    assertEquals(lagretSak1DTO.getId(), resultList.getItems().get(0).getId());
    assertEquals(lagretSak2DTO.getId(), resultList.getItems().get(1).getId());
    assertEquals(lagretSak3DTO.getId(), resultList.getItems().get(2).getId());

    // ASC startingAfter
    response =
        get(
            "/bruker/"
                + brukerDTO.getId()
                + "/lagretSak?sortOrder=asc&startingAfter="
                + lagretSak2DTO.getId(),
            accessToken);
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(lagretSak3DTO.getId(), resultList.getItems().get(0).getId());

    // ASC endingBefore
    response =
        get(
            "/bruker/"
                + brukerDTO.getId()
                + "/lagretSak?sortOrder=asc&endingBefore="
                + lagretSak2DTO.getId(),
            accessToken);
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(lagretSak1DTO.getId(), resultList.getItems().get(0).getId());

    // Delete the LagretSaks
    assertEquals(
        HttpStatus.OK, delete("/lagretSak/" + lagretSak1DTO.getId(), accessToken).getStatusCode());
    assertEquals(
        HttpStatus.OK, delete("/lagretSak/" + lagretSak2DTO.getId(), accessToken).getStatusCode());
    assertEquals(
        HttpStatus.OK, delete("/lagretSak/" + lagretSak3DTO.getId(), accessToken).getStatusCode());

    // Delete Saksmappe
    assertEquals(HttpStatus.OK, delete("/saksmappe/" + saksmappe1DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/saksmappe/" + saksmappe2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/saksmappe/" + saksmappe3DTO.getId()).getStatusCode());
  }
}
