package no.einnsyn.backend.common.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.moetesak.models.MoetesakDTO;
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
    assertEquals(4, result.getItems().size());
    var searchResultIds = result.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(searchResultIds.contains(moetesakFooDTO.getId()));
    assertTrue(searchResultIds.contains(moetesakBarDTO.getId()));
    assertTrue(searchResultIds.contains(moetesakBazDTO.getId()));
    assertTrue(searchResultIds.contains(moetemappeDTO.getId()));
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
}
