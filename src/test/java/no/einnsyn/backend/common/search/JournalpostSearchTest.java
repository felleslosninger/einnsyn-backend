package no.einnsyn.backend.common.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
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

  SaksmappeDTO saksmappeFooDTO;
  SaksmappeDTO saksmappeBarDTO;

  JournalpostDTO journalpostFooDTO;
  JournalpostDTO journalpostBarDTO;
  JournalpostDTO journalpostBazDTO;

  Type jptype = new TypeToken<PaginatedList<BaseDTO>>() {}.getType();

  @BeforeAll
  void setup() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("saksaar", "2023");
    saksmappeJSON.put("sakssekvensnummer", "1");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappeFooDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    saksmappeJSON.put("saksaar", "2024");
    saksmappeJSON.put("sakssekvensnummer", "2");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappeBarDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Add journalpost with title "foo"
    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("offentligTittel", "foo");
    journalpostJSON.put("offentligTittelSensitiv", "foo sensitivfoo");
    journalpostJSON.put("journalsekvensnummer", "1");
    journalpostJSON.put("journalpostnummer", 101);
    journalpostJSON.put("journalposttype", "inngaaende_dokument");
    response = post("/saksmappe/" + saksmappeFooDTO.getId() + "/journalpost", journalpostJSON);
    journalpostFooDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Add journalpost with title "bar"
    journalpostJSON.put("offentligTittel", "bar");
    journalpostJSON.put("offentligTittelSensitiv", "bar sensitivbar");
    journalpostJSON.put("administrativEnhetObjekt", journalenhet2Id);
    journalpostJSON.put("journalsekvensnummer", "2");
    journalpostJSON.put("journalpostnummer", 102);
    journalpostJSON.put("journalposttype", "utgaaende_dokument");
    response =
        post(
            "/saksmappe/" + saksmappeFooDTO.getId() + "/journalpost",
            journalpostJSON,
            journalenhet2Key);
    journalpostBarDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Add journalpost to underenhet
    journalpostJSON.put("offentligTittel", "baz");
    journalpostJSON.put("offentligTittelSensitiv", "baz sensitivbaz");
    journalpostJSON.put("administrativEnhetObjekt", underenhetId);
    journalpostJSON.put("journalsekvensnummer", "1");
    journalpostJSON.put("journalpostnummer", 101);
    journalpostJSON.put("journalposttype", "organinternt_dokument_uten_oppfoelging");
    response = post("/saksmappe/" + saksmappeBarDTO.getId() + "/journalpost", journalpostJSON);
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
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(journalpostFooDTO.getId(), searchResult.getItems().getFirst().getId());

    response = get("/search?query=bar");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(journalpostBarDTO.getId(), searchResult.getItems().getFirst().getId());

    response = get("/search?query=foo bar");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(2, searchResult.getItems().size());
    var searchResultIds = searchResult.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(searchResultIds.contains(journalpostFooDTO.getId()));
    assertTrue(searchResultIds.contains(journalpostBarDTO.getId()));
  }

  // TODO: ES scoring doesn't seem to be 100% reliable, and fails occasionally
  // @Test
  // void testQueryScore() throws Exception {
  //   // Score higher with two matches
  //   var response = get("/search?query=foo bar sensitivbar");
  //   assertEquals(HttpStatus.OK, response.getStatusCode());
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
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(4, searchResult.getItems().size());
    var searchResultIds = searchResult.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(searchResultIds.contains(journalpostFooDTO.getId()));
    assertTrue(searchResultIds.contains(journalpostBazDTO.getId()));
    assertTrue(searchResultIds.contains(saksmappeFooDTO.getId()));
    assertTrue(searchResultIds.contains(saksmappeBarDTO.getId()));

    response = get("/search?administrativEnhet=" + underenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(journalpostBazDTO.getId(), searchResult.getItems().getFirst().getId());

    response = get("/search?administrativEnhet=" + journalenhet2Id);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(journalpostBarDTO.getId(), searchResult.getItems().getFirst().getId());
  }

  @Test
  void testAdministrativEnhetAndEntity() throws Exception {
    var response = get("/search?entity=Journalpost&administrativEnhet=" + journalenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(2, searchResult.getItems().size());
    var searchResultIds = searchResult.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(searchResultIds.contains(journalpostFooDTO.getId()));
    assertTrue(searchResultIds.contains(journalpostBazDTO.getId()));

    response = get("/search?entity=Saksmappe&administrativEnhet=" + journalenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(2, searchResult.getItems().size());
    searchResultIds = searchResult.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(searchResultIds.contains(saksmappeFooDTO.getId()));
    assertTrue(searchResultIds.contains(saksmappeBarDTO.getId()));
  }

  @Test
  void testAdministrativEnhetAndQuery() throws Exception {
    var response = get("/search?query=foo&administrativEnhet=" + journalenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(journalpostFooDTO.getId(), searchResult.getItems().getFirst().getId());

    response = get("/search?query=baz&administrativEnhet=" + underenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(journalpostBazDTO.getId(), searchResult.getItems().getFirst().getId());

    response = get("/search?query=foo&administrativEnhet=" + journalenhet2Id);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(0, searchResult.getItems().size());
  }

  @Test
  void testAdministrativEnhetExact() throws Exception {
    var response = get("/search?administrativEnhetExact=" + journalenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(3, searchResult.getItems().size());
    var searchResultIds = searchResult.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(searchResultIds.contains(journalpostFooDTO.getId()));
    assertTrue(searchResultIds.contains(saksmappeFooDTO.getId()));
    assertTrue(searchResultIds.contains(saksmappeBarDTO.getId()));

    response = get("/search?administrativEnhetExact=" + underenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(journalpostBazDTO.getId(), searchResult.getItems().getFirst().getId());

    response = get("/search?administrativEnhetExact=" + journalenhet2Id);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(journalpostBarDTO.getId(), searchResult.getItems().getFirst().getId());
  }

  @Test
  void testExcludeAdministrativEnhet() throws Exception {
    var response = get("/search?excludeAdministrativEnhet=" + journalenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(journalpostBarDTO.getId(), searchResult.getItems().getFirst().getId());

    response = get("/search?excludeAdministrativEnhet=" + underenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(4, searchResult.getItems().size());
    var searchResultIds = searchResult.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(searchResultIds.contains(journalpostFooDTO.getId()));
    assertTrue(searchResultIds.contains(journalpostBarDTO.getId()));
    assertTrue(searchResultIds.contains(saksmappeFooDTO.getId()));
    assertTrue(searchResultIds.contains(saksmappeBarDTO.getId()));

    response = get("/search?excludeAdministrativEnhet=" + journalenhet2Id);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(4, searchResult.getItems().size());
    searchResultIds = searchResult.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(searchResultIds.contains(journalpostFooDTO.getId()));
    assertTrue(searchResultIds.contains(journalpostBazDTO.getId()));
    assertTrue(searchResultIds.contains(saksmappeFooDTO.getId()));
    assertTrue(searchResultIds.contains(saksmappeBarDTO.getId()));
  }

  @Test
  void testFilterById() throws Exception {
    var response = get("/search?ids=" + journalpostFooDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(journalpostFooDTO.getId(), searchResult.getItems().getFirst().getId());

    response =
        get("/search?ids=" + journalpostFooDTO.getId() + "&ids=" + journalpostBarDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
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
    searchResult = gson.fromJson(response.getBody(), jptype);
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
    var response = post("/saksmappe/" + saksmappeFooDTO.getId() + "/journalpost", journalpostJSON);
    var futureJournalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    esClient.indices().refresh(r -> r.index(elasticsearchIndex));

    // Test that the journalpost is returned for the journalenhet
    response = get("/search?query=future");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(futureJournalpostDTO.getId(), searchResult.getItems().getFirst().getId());

    // Test that the journalpost is not returned for anonymous
    response = getAnon("/search?query=future");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(0, searchResult.getItems().size());

    // Test that the future journalpost is returned when accessible
    Awaitility.await()
        .untilAsserted(
            () -> {
              var responseFuture = getAnon("/search?query=future");
              assertEquals(HttpStatus.OK, responseFuture.getStatusCode());
              PaginatedList<BaseDTO> searchResultFuture =
                  gson.fromJson(responseFuture.getBody(), jptype);
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
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    assertNull(searchResult.getPrevious());

    response = get("/search?limit=2");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(2, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    assertNull(searchResult.getPrevious());

    response = get("/search?limit=3");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(3, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    assertNull(searchResult.getPrevious());

    response = get("/search?limit=4");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(4, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    assertNull(searchResult.getPrevious());

    response = get("/search?limit=5");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(5, searchResult.getItems().size());
    assertNull(searchResult.getNext());
    assertNull(searchResult.getPrevious());

    response = get("/search?limit=6");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(5, searchResult.getItems().size());
    assertNull(searchResult.getNext());
    assertNull(searchResult.getPrevious());
  }

  @Test
  void testIdPagination() throws Exception {
    var response = get("/search?limit=1&orderBy=id");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    assertNull(searchResult.getPrevious());
    var firstId = searchResult.getItems().getFirst().getId();

    response = get(searchResult.getNext());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    assertNotNull(searchResult.getPrevious());
    var secondId = searchResult.getItems().getFirst().getId();

    response = get(searchResult.getNext());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    assertNotNull(searchResult.getPrevious());
    var thirdId = searchResult.getItems().getFirst().getId();

    response = get(searchResult.getNext());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    assertNotNull(searchResult.getPrevious());
    var fourthId = searchResult.getItems().getFirst().getId();

    response = get(searchResult.getNext());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertNull(searchResult.getNext());
    assertNotNull(searchResult.getPrevious());

    response = get(searchResult.getPrevious());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    assertNotNull(searchResult.getPrevious());
    assertEquals(fourthId, searchResult.getItems().getFirst().getId());

    response = get(searchResult.getPrevious());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    assertNotNull(searchResult.getPrevious());
    assertEquals(thirdId, searchResult.getItems().getFirst().getId());

    response = get(searchResult.getPrevious());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    assertNotNull(searchResult.getPrevious());
    assertEquals(secondId, searchResult.getItems().getFirst().getId());

    response = get(searchResult.getPrevious());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
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
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(5, searchResult.getItems().size());
  }

  @Test
  void testPaginationWithSortBy() throws Exception {
    var response = get("/search?limit=1&sortBy=score");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    response = get(searchResult.getNext());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());

    response = get("/search?limit=1&sortBy=id");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    response = get(searchResult.getNext());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());

    response = get("/search?limit=1&sortBy=entity");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    response = get(searchResult.getNext());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());

    response = get("/search?limit=1&sortBy=publisertDato");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    response = get(searchResult.getNext());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());

    response = get("/search?limit=1&sortBy=oppdatertDato");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    response = get(searchResult.getNext());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());

    response = get("/search?limit=1&sortBy=moetedato");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    response = get(searchResult.getNext());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());

    // TODO: response = get("/search?limit=1&sortBy=fulltekst");

    response = get("/search?limit=1&sortBy=type");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    response = get(searchResult.getNext());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
  }

  @Test
  void testFilterByProperties() throws Exception {

    var response = get("/search?saksaar=2023");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(3, searchResult.getItems().size()); // Saksmappe + 2 journalposts

    response = get("/search?saksaar=2024");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(2, searchResult.getItems().size()); // Saksmappe + 1 journalpost

    response = get("/search?saksaar=2023&entity=Journalpost");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(2, searchResult.getItems().size()); // 2 journalposts

    response = get("/search?journalpostnummer=101");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(2, searchResult.getItems().size());

    response = get("/search?saksaar=2023&journalpostnummer=101");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(1, searchResult.getItems().size());

    response = get("/search?journalsekvensnummer=1");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(2, searchResult.getItems().size());

    response = get("/search?journalsekvensnummer=1&saksaar=2023");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(1, searchResult.getItems().size());

    response = get("/search?saksaar=2023&journalsekvensnummer=1&journalpostnummer=101");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(1, searchResult.getItems().size());

    response = get("/search?saksaar=2023&journalsekvensnummer=1&journalpostnummer=102");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(0, searchResult.getItems().size());

    response = get("/search?administrativEnhet=" + journalenhet2Orgnummer);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(1, searchResult.getItems().size());
    assertEquals(journalpostBarDTO.getId(), searchResult.getItems().getFirst().getId());

    response = get("/search?administrativEnhet=" + journalenhetOrgnummer);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(4, searchResult.getItems().size());
    var searchResultIds = searchResult.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(searchResultIds.contains(journalpostFooDTO.getId()));
    assertTrue(searchResultIds.contains(journalpostBazDTO.getId()));
    assertTrue(searchResultIds.contains(saksmappeFooDTO.getId()));
    assertTrue(searchResultIds.contains(saksmappeBarDTO.getId()));

    response =
        get(
            "/search?administrativEnhet="
                + journalenhetOrgnummer
                + "&administrativEnhet="
                + journalenhet2Orgnummer);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(5, searchResult.getItems().size());

    response = get("/search?saksaar=2023&saksaar=2024");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(5, searchResult.getItems().size());
  }

  @Test
  void testFilterByOppdatertPublisertDato() throws Exception {
    // Add a journalpost with publisertDato and oppdatertDato in 2023
    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("offentligTittel", "old publish");
    journalpostJSON.put("offentligTittelSensitiv", "old publish sensitivold publish");
    journalpostJSON.put("administrativEnhetObjekt", journalenhetId);
    journalpostJSON.put("publisertDato", "2023-06-10T00:00:00Z");
    journalpostJSON.put("oppdatertDato", "2023-03-10T00:00:00Z");
    var response =
        postAdmin("/saksmappe/" + saksmappeBarDTO.getId() + "/journalpost", journalpostJSON);
    var journalpostOldPublishDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Refresh indices
    esClient.indices().refresh(r -> r.index(elasticsearchIndex));

    // Test filtering by publisertDatoFrom
    response = get("/search?publisertDatoFrom=2024-01-01");
    PaginatedList<BaseDTO> result = gson.fromJson(response.getBody(), jptype);
    var items = result.getItems();
    assertEquals(5, items.size());

    response = get("/search?publisertDatoFrom=2023-01-01");
    result = gson.fromJson(response.getBody(), jptype);
    items = result.getItems();
    assertEquals(6, items.size());

    response = get("/search?publisertDatoFrom=2023-01-01&publisertDatoTo=2023-12-31");
    result = gson.fromJson(response.getBody(), jptype);
    items = result.getItems();
    assertEquals(1, items.size());

    response = get("/search?publisertDatoTo=2023-12-31");
    result = gson.fromJson(response.getBody(), jptype);
    items = result.getItems();
    assertEquals(1, items.size());
    assertEquals(journalpostOldPublishDTO.getId(), items.get(0).getId());

    // Test filtering by oppdatertDatoFrom
    response = get("/search?oppdatertDatoFrom=2024-01-01");
    result = gson.fromJson(response.getBody(), jptype);
    items = result.getItems();
    assertEquals(5, items.size()); // Does not include the journalpost with old publish date

    response = get("/search?oppdatertDatoFrom=2023-01-01&oppdatertDatoTo=2023-12-31");
    result = gson.fromJson(response.getBody(), jptype);
    items = result.getItems();
    assertEquals(1, items.size());
    assertEquals(journalpostOldPublishDTO.getId(), items.get(0).getId());

    response = get("/search?publisertDatoFrom=2023-01-01&oppdatertDatoTo=2023-12-31");
    result = gson.fromJson(response.getBody(), jptype);
    items = result.getItems();
    assertEquals(1, items.size());
    assertEquals(journalpostOldPublishDTO.getId(), items.get(0).getId());

    response = get("/search?oppdatertDatoFrom=2023-01-01&oppdatertDatoTo=2023-01-01");
    result = gson.fromJson(response.getBody(), jptype);
    items = result.getItems();
    assertEquals(0, items.size());

    response = get("/search?publisertDatoFrom=2023-01-01&oppdatertDatoTo=2023-01-01");
    result = gson.fromJson(response.getBody(), jptype);
    items = result.getItems();
    assertEquals(0, items.size());

    // Clean up
    deleteAdmin("/journalpost/" + journalpostOldPublishDTO.getId());
  }

  @Test
  void testFilterByJournalposttype() throws Exception {
    var response = get("/search?journalposttype=inngaaende_dokument");
    PaginatedList<BaseDTO> result = gson.fromJson(response.getBody(), jptype);
    var items = result.getItems();
    assertEquals(1, items.size());
    assertEquals(journalpostFooDTO.getId(), items.get(0).getId());

    response = get("/search?journalposttype=utgaaende_dokument");
    result = gson.fromJson(response.getBody(), jptype);
    items = result.getItems();
    assertEquals(1, items.size());
    assertEquals(journalpostBarDTO.getId(), items.get(0).getId());

    response = get("/search?journalposttype=organinternt_dokument_uten_oppfoelging");
    result = gson.fromJson(response.getBody(), jptype);
    items = result.getItems();
    assertEquals(1, items.size());
    assertEquals(journalpostBazDTO.getId(), items.get(0).getId());

    response =
        get(
            "/search?journalposttype=inngaaende_dokument&journalposttype=utgaaende_dokument&journalposttype=organinternt_dokument_uten_oppfoelging");
    result = gson.fromJson(response.getBody(), jptype);
    items = result.getItems();
    assertEquals(3, items.size());

    response = get("/search?journalposttype=saksframlegg");
    result = gson.fromJson(response.getBody(), jptype);
    items = result.getItems();
    assertEquals(0, items.size());

    response = get("/search?journalposttype=saksframlegg&journalposttype=inngaaende_dokument");
    result = gson.fromJson(response.getBody(), jptype);
    items = result.getItems();
    assertEquals(1, items.size());
    assertEquals(journalpostFooDTO.getId(), items.get(0).getId());
  }
}
