package no.einnsyn.backend.common.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.moetesak.models.MoetesakDTO;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MoetesakSearchTest extends EinnsynControllerTestBase {
  ArkivDTO arkivDTO;

  MoetemappeDTO moetemappeDTO;

  MoetesakDTO moetesakFooDTO;
  MoetesakDTO moetesakBarDTO;
  MoetesakDTO moetesakBazDTO;

  MoetemappeDTO moetemappeWithMoetedatoDTO;
  MoetesakDTO moetesakWithMoetedatoDTO;

  Type type = new TypeToken<PaginatedList<BaseDTO>>() {}.getType();

  @BeforeAll
  void setup() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    var moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("offentligTittel", "foo");
    moetemappeJSON.put("offentligTittelSensitiv", "foo sensitivfoo");
    moetemappeJSON.put("moetenummer", "1");
    moetemappeJSON.put("moetedato", "2020-01-01T00:00:00");
    moetemappeJSON.remove("moetesak");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    // Add moetesak with title "foo"
    var moetesakJSON = getMoetesakJSON();
    moetesakJSON.put("offentligTittel", "foo");
    moetesakJSON.put("offentligTittelSensitiv", "foo sensitivfoo");
    moetesakJSON.put("moetesakssekvensnummer", "1");
    moetesakJSON.put("moetesaksaar", 2023);
    response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesakJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    moetesakFooDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);

    // Add moetesak with title "bar"
    moetesakJSON.put("offentligTittel", "bar");
    moetesakJSON.put("offentligTittelSensitiv", "bar sensitivbar");
    moetesakJSON.put("moetesakssekvensnummer", "2");
    moetesakJSON.put("moetesaksaar", 2023);
    response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesakJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    moetesakBarDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);

    // Add moetesak with title "baz"
    moetesakJSON.put("offentligTittel", "baz");
    moetesakJSON.put("offentligTittelSensitiv", "baz sensitivbaz");
    moetesakJSON.put("moetesakssekvensnummer", "3");
    moetesakJSON.put("moetesaksaar", 2024);
    response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesakJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    moetesakBazDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);

    // Create moetedokument with fulltext document
    var dokumentobjektJSON = getDokumentobjektJSON();
    var dokumentbeskrivelseJSON = getDokumentbeskrivelseJSON();
    dokumentbeskrivelseJSON.put("dokumentobjekt", new JSONArray(List.of(dokumentobjektJSON)));
    var moetedokumentJSON = getMoetedokumentJSON();
    moetedokumentJSON.put("dokumentbeskrivelse", new JSONArray(List.of(dokumentbeskrivelseJSON)));
    moetemappeJSON.put("moetedokument", new JSONArray(List.of(moetedokumentJSON)));

    // Add another moetemappe with another moetedato
    moetemappeJSON.put("offentligTittel", "moetemappeWithMoetedato");
    moetemappeJSON.put("offentligTittelSensitiv", "bar sensitivbar");
    moetemappeJSON.put("moetenummer", "2");
    moetemappeJSON.put("moetedato", "2023-10-02T00:00:00Z");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    moetemappeWithMoetedatoDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    // Add moetesak to the new moetemappe
    moetesakJSON.put("offentligTittel", "moetesakWithMoetedato");
    moetesakJSON.put("offentligTittelSensitiv", "baz sensitivbaz");
    moetesakJSON.put("moetesakssekvensnummer", "12");
    moetesakJSON.put("moetesaksaar", 2022);
    response =
        post("/moetemappe/" + moetemappeWithMoetedatoDTO.getId() + "/moetesak", moetesakJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    moetesakWithMoetedatoDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);

    // Refresh indices
    esClient.indices().refresh(r -> r.index(elasticsearchIndex));
  }

  @AfterAll
  void teardown() throws Exception {
    // There is also a Saksmappe from journalenhet2, so use deleteAdmin
    deleteAdmin("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void testEmptySearch() throws Exception {
    var response = get("/search");
    PaginatedList<BaseDTO> result = gson.fromJson(response.getBody(), type);
    assertEquals(6, result.getItems().size());
    var searchResultIds = result.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(searchResultIds.contains(moetesakFooDTO.getId()));
    assertTrue(searchResultIds.contains(moetesakBarDTO.getId()));
    assertTrue(searchResultIds.contains(moetesakBazDTO.getId()));
    assertTrue(searchResultIds.contains(moetemappeDTO.getId()));
    assertTrue(searchResultIds.contains(moetesakWithMoetedatoDTO.getId()));
    assertTrue(searchResultIds.contains(moetemappeWithMoetedatoDTO.getId()));
  }

  @Test
  void testFilterByProperties() throws Exception {
    var response = get("/search?moetesaksaar=2023");
    PaginatedList<BaseDTO> result = gson.fromJson(response.getBody(), type);
    assertEquals(2, result.getItems().size());
    var searchResultIds = result.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(searchResultIds.contains(moetesakFooDTO.getId()));
    assertTrue(searchResultIds.contains(moetesakBarDTO.getId()));

    response = get("/search?moetesaksaar=2024");
    result = gson.fromJson(response.getBody(), type);
    assertEquals(1, result.getItems().size());
    searchResultIds = result.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(searchResultIds.contains(moetesakBazDTO.getId()));

    response = get("/search?moetesakssekvensnummer=1");
    result = gson.fromJson(response.getBody(), type);
    assertEquals(1, result.getItems().size());
    searchResultIds = result.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(searchResultIds.contains(moetesakFooDTO.getId()));

    response = get("/search?moetesakssekvensnummer=2&moetesakssekvensnummer=3");
    result = gson.fromJson(response.getBody(), type);
    assertEquals(2, result.getItems().size());
    searchResultIds = result.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(searchResultIds.contains(moetesakBarDTO.getId()));
    assertTrue(searchResultIds.contains(moetesakBazDTO.getId()));

    response =
        get(
            "/search?moetesakssekvensnummer=1&moetesakssekvensnummer=2&moetesakssekvensnummer=3&moetesaksaar=2023");
    result = gson.fromJson(response.getBody(), type);
    assertEquals(2, result.getItems().size());
    searchResultIds = result.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(searchResultIds.contains(moetesakFooDTO.getId()));
    assertTrue(searchResultIds.contains(moetesakBarDTO.getId()));
  }

  @Test
  void testFulltextSearch() throws Exception {
    var response = get("/search?fulltext=true");
    PaginatedList<BaseDTO> result = gson.fromJson(response.getBody(), type);
    assertEquals(1, result.getItems().size());
  }

  @Test
  void testFilterByMoetedato() throws Exception {
    // 2023-10-02T00:00:00 : Moetemappe + Moetesak
    var response = get("/search?moetedatoFrom=2023-10-01");
    PaginatedList<BaseDTO> result = gson.fromJson(response.getBody(), type);
    var items = result.getItems();
    assertEquals(2, result.getItems().size());
    assertTrue(
        items.stream().anyMatch(item -> item.getId().equals(moetesakWithMoetedatoDTO.getId())));
    assertTrue(
        items.stream().anyMatch(item -> item.getId().equals(moetemappeWithMoetedatoDTO.getId())));

    // 2020-01-01T00:00:00 : Moetemappe + 3 * Moetesak
    response = get("/search?moetedatoTo=2020-01-02");
    result = gson.fromJson(response.getBody(), type);
    items = result.getItems();
    assertEquals(4, result.getItems().size());
    assertTrue(items.stream().anyMatch(item -> item.getId().equals(moetesakFooDTO.getId())));
    assertTrue(items.stream().anyMatch(item -> item.getId().equals(moetesakBarDTO.getId())));
    assertTrue(items.stream().anyMatch(item -> item.getId().equals(moetesakBazDTO.getId())));
    assertTrue(items.stream().anyMatch(item -> item.getId().equals(moetemappeDTO.getId())));

    response = get("/search?moetedatoFrom=2023-10-02");
    result = gson.fromJson(response.getBody(), type);
    assertEquals(2, result.getItems().size());
    items = result.getItems();
    assertTrue(
        items.stream().anyMatch(item -> item.getId().equals(moetesakWithMoetedatoDTO.getId())));
    assertTrue(
        items.stream().anyMatch(item -> item.getId().equals(moetemappeWithMoetedatoDTO.getId())));

    response = get("/search?moetedatoFrom=2023-10-03");
    result = gson.fromJson(response.getBody(), type);
    assertEquals(0, result.getItems().size());

    // Test with both from and to
    response = get("/search?moetedatoFrom=2020-01-01&moetedatoTo=2020-01-01");
    result = gson.fromJson(response.getBody(), type);
    assertEquals(4, result.getItems().size());
    items = result.getItems();
    assertTrue(items.stream().anyMatch(item -> item.getId().equals(moetesakFooDTO.getId())));
    assertTrue(items.stream().anyMatch(item -> item.getId().equals(moetesakBarDTO.getId())));
    assertTrue(items.stream().anyMatch(item -> item.getId().equals(moetesakBazDTO.getId())));
    assertTrue(items.stream().anyMatch(item -> item.getId().equals(moetemappeDTO.getId())));

    // Test with both from and to, with a range that includes both moetemapper
    response = get("/search?moetedatoFrom=2020-01-01&moetedatoTo=2023-10-02");
    result = gson.fromJson(response.getBody(), type);
    assertEquals(6, result.getItems().size());

    response = get("/search?moetedatoFrom=2024-01-01");
    result = gson.fromJson(response.getBody(), type);
    assertEquals(0, result.getItems().size());

    response = get("/search?moetedatoTo=2019-12-31");
    result = gson.fromJson(response.getBody(), type);
    assertEquals(0, result.getItems().size());

    response = get("/search?moetedatoFrom=2021-01-01&moetedatoTo=2022-01-01");
    result = gson.fromJson(response.getBody(), type);
    assertEquals(0, result.getItems().size());
  }

  @Test
  void testFilterByMoetedatoDateTimeAndTimezone() throws Exception {
    var response = get("/search?moetedatoFrom=2023-10-02T00:00:00Z");
    PaginatedList<BaseDTO> result = gson.fromJson(response.getBody(), type);
    assertEquals(2, result.getItems().size());
    var ids = result.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(ids.contains(moetemappeWithMoetedatoDTO.getId()));
    assertTrue(ids.contains(moetesakWithMoetedatoDTO.getId()));

    response = get("/search?moetedatoFrom=2023-10-02T00:00:01Z");
    result = gson.fromJson(response.getBody(), type);
    assertEquals(0, result.getItems().size());

    response = get("/search?moetedatoFrom=2023-10-02T02:00:00+02:00");
    result = gson.fromJson(response.getBody(), type);
    assertEquals(0, result.getItems().size());

    response = get("/search?moetedatoTo=2020-01-01T00:00:00Z");
    result = gson.fromJson(response.getBody(), type);
    assertEquals(4, result.getItems().size());

    // This Moetemappe was inserted without zone offset, so it is stored as local time.
    response = get("/search?moetedatoTo=2019-12-31T23:59:59Z");
    result = gson.fromJson(response.getBody(), type);
    assertEquals(4, result.getItems().size());
    response = get("/search?moetedatoTo=2019-12-31T23:00:00Z");
    result = gson.fromJson(response.getBody(), type);
    assertEquals(4, result.getItems().size());
    response = get("/search?moetedatoTo=2019-12-31T22:59:59Z");
    result = gson.fromJson(response.getBody(), type);
    assertEquals(0, result.getItems().size());
  }
}
