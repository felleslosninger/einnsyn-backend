package no.einnsyn.backend.common.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.reflect.TypeToken;
import java.time.Instant;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class JournalpostSearchTest extends EinnsynControllerTestBase {

  ArkivDTO arkivDTO;

  SaksmappeDTO saksmappeDTO;

  JournalpostDTO journalpostFooDTO;
  JournalpostDTO journalpostBarDTO;
  JournalpostDTO journalpostBazDTO;

  @BeforeAll
  void setup() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Add journalpost with title "foo"
    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("offentligTittel", "foo");
    journalpostJSON.put("offentligTittelSensitiv", "foo sensitivfoo");
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    journalpostFooDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Add journalpost with title "bar"
    journalpostJSON.put("offentligTittel", "bar");
    journalpostJSON.put("offentligTittelSensitiv", "bar sensitivbar");
    journalpostJSON.put("administrativEnhetObjekt", journalenhet2Id);
    response =
        post(
            "/saksmappe/" + saksmappeDTO.getId() + "/journalpost",
            journalpostJSON,
            journalenhet2Key);
    journalpostBarDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Add journalpost to underenhet
    journalpostJSON.put("offentligTittel", "baz");
    journalpostJSON.put("offentligTittelSensitiv", "baz sensitivbaz");
    journalpostJSON.put("administrativEnhetObjekt", underenhetId);
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    journalpostBazDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Refresh indices
    esClient.indices().refresh(r -> r.index(elasticsearchIndex));
  }

  @AfterAll
  void teardown() throws Exception {
    // There is also a Saksmappe from journalenhet2, so use deleteAdmin
    deleteAdmin("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void testSearchQuery() throws Exception {
    var response = get("/search?query=foo");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var type = new TypeToken<PaginatedList<JournalpostDTO>>() {}.getType();
    PaginatedList<JournalpostDTO> searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(journalpostFooDTO.getId(), searchResult.getItems().getFirst().getId());

    response = get("/search?query=bar");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(journalpostBarDTO.getId(), searchResult.getItems().getFirst().getId());

    response = get("/search?query=foo bar");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(2, searchResult.getItems().size());
    var searchResultIds = searchResult.getItems().stream().map(JournalpostDTO::getId).toList();
    assertTrue(searchResultIds.contains(journalpostFooDTO.getId()));
    assertTrue(searchResultIds.contains(journalpostBarDTO.getId()));
  }

  // TODO: ES scoring doesn't seem to be 100% reliable, and fails occasionally
  // @Test
  // void testQueryScore() throws Exception {
  //   // Score higher with two matches
  //   var response = get("/search?query=foo bar sensitivbar");
  //   assertEquals(HttpStatus.OK, response.getStatusCode());
  //   var type = new TypeToken<PaginatedList<BaseDTO>>() {}.getType();
  //   PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), type);
  //   assertNotNull(searchResult);
  //   assertEquals(2, searchResult.getItems().size());
  //   assertEquals(journalpostBarDTO.getId(), searchResult.getItems().getFirst().getId());
  //   assertEquals(journalpostFooDTO.getId(), searchResult.getItems().getLast().getId());

  //   // Reverse
  //   response = get("/search?query=sensitivbar bar foo&sortOrder=asc");
  //   assertEquals(HttpStatus.OK, response.getStatusCode());
  //   searchResult = gson.fromJson(response.getBody(), type);
  //   assertNotNull(searchResult);
  //   assertEquals(2, searchResult.getItems().size());
  //   assertEquals(journalpostFooDTO.getId(), searchResult.getItems().getFirst().getId());
  //   assertEquals(journalpostBarDTO.getId(), searchResult.getItems().getLast().getId());
  // }

  @Test
  void matchAdministrativEnhet() throws Exception {
    var response = get("/search?administrativEnhet=" + journalenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var type = new TypeToken<PaginatedList<BaseDTO>>() {}.getType();
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(3, searchResult.getItems().size());
    var searchResultIds = searchResult.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(searchResultIds.contains(journalpostFooDTO.getId()));
    assertTrue(searchResultIds.contains(journalpostBazDTO.getId()));
    assertTrue(searchResultIds.contains(saksmappeDTO.getId()));

    response = get("/search?administrativEnhet=" + underenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(journalpostBazDTO.getId(), searchResult.getItems().getFirst().getId());

    response = get("/search?administrativEnhet=" + journalenhet2Id);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(journalpostBarDTO.getId(), searchResult.getItems().getFirst().getId());
  }

  @Test
  void testAdministrativEnhetAndEntity() throws Exception {
    var response = get("/search?entity=Journalpost&administrativEnhet=" + journalenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var type = new TypeToken<PaginatedList<BaseDTO>>() {}.getType();
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(2, searchResult.getItems().size());
    var searchResultIds = searchResult.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(searchResultIds.contains(journalpostFooDTO.getId()));
    assertTrue(searchResultIds.contains(journalpostBazDTO.getId()));

    response = get("/search?entity=Saksmappe&administrativEnhet=" + journalenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(saksmappeDTO.getId(), searchResult.getItems().getFirst().getId());
  }

  @Test
  void testAdministrativEnhetAndQuery() throws Exception {
    var response = get("/search?query=foo&administrativEnhet=" + journalenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var type = new TypeToken<PaginatedList<JournalpostDTO>>() {}.getType();
    PaginatedList<JournalpostDTO> searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(journalpostFooDTO.getId(), searchResult.getItems().getFirst().getId());

    response = get("/search?query=baz&administrativEnhet=" + underenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(journalpostBazDTO.getId(), searchResult.getItems().getFirst().getId());

    response = get("/search?query=foo&administrativEnhet=" + journalenhet2Id);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(0, searchResult.getItems().size());
  }

  @Test
  void testAdministrativEnhetExact() throws Exception {
    var response = get("/search?administrativEnhetExact=" + journalenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var type = new TypeToken<PaginatedList<BaseDTO>>() {}.getType();
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(2, searchResult.getItems().size());
    var searchResultIds = searchResult.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(searchResultIds.contains(journalpostFooDTO.getId()));
    assertTrue(searchResultIds.contains(saksmappeDTO.getId()));

    response = get("/search?administrativEnhetExact=" + underenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(journalpostBazDTO.getId(), searchResult.getItems().getFirst().getId());

    response = get("/search?administrativEnhetExact=" + journalenhet2Id);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(journalpostBarDTO.getId(), searchResult.getItems().getFirst().getId());
  }

  @Test
  void testExcludeAdministrativEnhet() throws Exception {
    var response = get("/search?excludeAdministrativEnhet=" + journalenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var type = new TypeToken<PaginatedList<BaseDTO>>() {}.getType();
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(journalpostBarDTO.getId(), searchResult.getItems().getFirst().getId());

    response = get("/search?excludeAdministrativEnhet=" + underenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(3, searchResult.getItems().size());
    var searchResultIds = searchResult.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(searchResultIds.contains(journalpostFooDTO.getId()));
    assertTrue(searchResultIds.contains(journalpostBarDTO.getId()));
    assertTrue(searchResultIds.contains(saksmappeDTO.getId()));

    response = get("/search?excludeAdministrativEnhet=" + journalenhet2Id);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(3, searchResult.getItems().size());
    searchResultIds = searchResult.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(searchResultIds.contains(journalpostFooDTO.getId()));
    assertTrue(searchResultIds.contains(journalpostBazDTO.getId()));
    assertTrue(searchResultIds.contains(saksmappeDTO.getId()));
  }

  @Test
  void testFilterById() throws Exception {
    var response = get("/search?ids=" + journalpostFooDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var type = new TypeToken<PaginatedList<BaseDTO>>() {}.getType();
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(journalpostFooDTO.getId(), searchResult.getItems().getFirst().getId());

    response =
        get("/search?ids=" + journalpostFooDTO.getId() + "&ids=" + journalpostBarDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(2, searchResult.getItems().size());
    var searchResultIds = searchResult.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(searchResultIds.contains(journalpostFooDTO.getId()));
    assertTrue(searchResultIds.contains(journalpostBarDTO.getId()));

    response =
        get(
            "/search?query=foo&ids="
                + journalpostFooDTO.getId()
                + "&ids="
                + journalpostBarDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(journalpostFooDTO.getId(), searchResult.getItems().getFirst().getId());
  }

  @Test
  void testAccessibleFilter() throws Exception {
    // Insert journalpost that is accessible in the future
    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("offentligTittel", "future");
    journalpostJSON.put("offentligTittelSensitiv", "future sensitivfuture");
    journalpostJSON.put("administrativEnhetObjekt", journalenhetId);
    journalpostJSON.put("accessibleAfter", Instant.now().plusSeconds(2));
    var response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    var futureJournalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    esClient.indices().refresh(r -> r.index(elasticsearchIndex));

    // Test that the journalpost is returned for the journalenhet
    response = get("/search?query=future");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var type = new TypeToken<PaginatedList<BaseDTO>>() {}.getType();
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(futureJournalpostDTO.getId(), searchResult.getItems().getFirst().getId());

    // Test that the journalpost is not returned for anonymous
    response = getAnon("/search?query=future");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(0, searchResult.getItems().size());

    // Test that the future journalpost is returned when accessible
    Awaitility.await()
        .untilAsserted(
            () -> {
              var responseFuture = getAnon("/search?query=future");
              assertEquals(HttpStatus.OK, responseFuture.getStatusCode());
              PaginatedList<BaseDTO> searchResultFuture =
                  gson.fromJson(responseFuture.getBody(), type);
              assertNotNull(searchResultFuture);
              assertEquals(1, searchResultFuture.getItems().size());
              assertEquals(
                  futureJournalpostDTO.getId(), searchResultFuture.getItems().getFirst().getId());
            });

    delete("/journalpost/" + futureJournalpostDTO.getId());
  }

  @Test
  void testLimit() throws Exception {
    var response = get("/search?limit=1");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var type = new TypeToken<PaginatedList<BaseDTO>>() {}.getType();
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    assertNull(searchResult.getPrevious());

    response = get("/search?limit=2");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(2, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    assertNull(searchResult.getPrevious());

    response = get("/search?limit=3");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(3, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    assertNull(searchResult.getPrevious());

    response = get("/search?limit=4");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(4, searchResult.getItems().size());
    assertNull(searchResult.getNext());
    assertNull(searchResult.getPrevious());

    response = get("/search?limit=5");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(4, searchResult.getItems().size());
    assertNull(searchResult.getNext());
    assertNull(searchResult.getPrevious());
  }

  @Test
  void testIdPagination() throws Exception {
    var response = get("/search?limit=1&orderBy=id");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var type = new TypeToken<PaginatedList<JournalpostDTO>>() {}.getType();
    PaginatedList<JournalpostDTO> searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    assertNull(searchResult.getPrevious());
    var firstId = searchResult.getItems().getFirst().getId();

    response = get(searchResult.getNext());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    assertNotNull(searchResult.getPrevious());
    var secondId = searchResult.getItems().getFirst().getId();

    response = get(searchResult.getNext());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    assertNotNull(searchResult.getPrevious());
    var thirdId = searchResult.getItems().getFirst().getId();

    response = get(searchResult.getNext());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertNull(searchResult.getNext());
    assertNotNull(searchResult.getPrevious());

    response = get(searchResult.getPrevious());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    assertNotNull(searchResult.getPrevious());
    assertEquals(thirdId, searchResult.getItems().getFirst().getId());

    response = get(searchResult.getPrevious());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    assertNotNull(searchResult.getPrevious());
    assertEquals(secondId, searchResult.getItems().getFirst().getId());

    response = get(searchResult.getPrevious());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(firstId, searchResult.getItems().getFirst().getId());
    assertNotNull(searchResult.getNext());
    assertNull(searchResult.getPrevious());
  }

  @Test
  void testWithoutQueryString() throws Exception {
    var response = get("/search");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var type = new TypeToken<PaginatedList<JournalpostDTO>>() {}.getType();
    PaginatedList<JournalpostDTO> searchResult = gson.fromJson(response.getBody(), type);
    assertNotNull(searchResult);
    assertEquals(4, searchResult.getItems().size());
  }

  @Test
  void testPaginationWithSortBy() throws Exception {
    var response = get("/search?limit=1&sortBy=score");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var type = new TypeToken<PaginatedList<JournalpostDTO>>() {}.getType();
    PaginatedList<JournalpostDTO> searchResult = gson.fromJson(response.getBody(), type);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    response = get(searchResult.getNext());
    System.err.println(response.getBody());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());

    response = get("/search?limit=1&sortBy=id");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    response = get(searchResult.getNext());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());

    response = get("/search?limit=1&sortBy=entity");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    response = get(searchResult.getNext());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());

    response = get("/search?limit=1&sortBy=publisertDato");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    response = get(searchResult.getNext());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());

    response = get("/search?limit=1&sortBy=oppdatertDato");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    response = get(searchResult.getNext());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());

    response = get("/search?limit=1&sortBy=moetedato");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    response = get(searchResult.getNext());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());

    // TODO: response = get("/search?limit=1&sortBy=fulltekst");

    response = get("/search?limit=1&sortBy=type");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    response = get(searchResult.getNext());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), type);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
  }
}
