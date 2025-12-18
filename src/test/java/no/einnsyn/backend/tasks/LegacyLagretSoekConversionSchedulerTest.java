package no.einnsyn.backend.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import no.einnsyn.backend.EinnsynLegacyElasticTestBase;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import no.einnsyn.backend.entities.lagretsoek.models.LagretSoekDTO;
import no.einnsyn.backend.tasks.handlers.lagretsoek.LegacyLagretSoekConversionScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LegacyLagretSoekConversionSchedulerTest extends EinnsynLegacyElasticTestBase {

  @Autowired private LegacyLagretSoekConversionScheduler scheduler;

  private BrukerDTO brukerDTO;
  private String brukerToken;
  private LagretSoekDTO lagretSoekDTO;

  @BeforeEach
  void setUp() throws Exception {
    // Create user
    var brukerResponse = post("/bruker", getBrukerJSON());
    assertEquals(HttpStatus.CREATED, brukerResponse.getStatusCode());
    brukerDTO = gson.fromJson(brukerResponse.getBody(), BrukerDTO.class);
    var brukerId = brukerDTO.getId();
    var brukerEntity = brukerRepository.findById(brukerId).orElseThrow();
    brukerToken = jwtService.generateToken(brukerEntity);

    // Add lagretSoek
    var lagretSoekJSON = getLagretSoekJSON();
    lagretSoekJSON.put("legacyQuery", "{\"searchTerm\":\"test\"}");
    lagretSoekJSON.remove("searchParameters");
    var response = post("/bruker/" + brukerId + "/lagretSoek", lagretSoekJSON, brukerToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    lagretSoekDTO = gson.fromJson(response.getBody(), LagretSoekDTO.class);
  }

  @AfterEach
  void tearDown() throws Exception {
    delete("/lagretSoek/" + lagretSoekDTO.getId(), brukerToken);
    delete("/bruker/" + brukerDTO.getId(), brukerToken);
  }

  @Test
  void testConvertLegacyLagretSoek() throws Exception {
    ReflectionTestUtils.setField(scheduler, "dryRun", false);

    scheduler.convertLegacyLagretSoek();
    awaitSideEffects();

    var response = get("/lagretSoek/" + lagretSoekDTO.getId(), brukerToken);
    var updatedLagretSoek = gson.fromJson(response.getBody(), LagretSoekDTO.class);

    // Verify that searchParameters got populated
    assertNotNull(updatedLagretSoek.getSearchParameters());
    assertEquals("test", updatedLagretSoek.getSearchParameters().getQuery());

    // Verify that delete was called on ES client
    captureDeletedDocuments(1);
  }

  @Test
  void testConvertLegacyLagretSoekWithTittelPhrase() throws Exception {
    ReflectionTestUtils.setField(scheduler, "dryRun", false);

    // Create a legacy query with a search_tittel field
    var lagretSoekJSON = getLagretSoekJSON();
    lagretSoekJSON.put(
        "legacyQuery",
        """
        {
          "searchTerms":[{
            "field":"search_tittel",
            "searchTerm":"multiple words",
            "operator":"PHRASE"
          }]
        }
        """);
    lagretSoekJSON.remove("searchParameters");
    var brukerId = brukerDTO.getId();
    var response = post("/bruker/" + brukerId + "/lagretSoek", lagretSoekJSON, brukerToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var localLagretSoekDTO = gson.fromJson(response.getBody(), LagretSoekDTO.class);

    // Run conversion
    scheduler.convertLegacyLagretSoek();
    awaitSideEffects();

    response = get("/lagretSoek/" + localLagretSoekDTO.getId(), brukerToken);
    var updatedLagretSoek = gson.fromJson(response.getBody(), LagretSoekDTO.class);

    // Verify that searchParameters.tittel contains the expected value
    assertNotNull(updatedLagretSoek.getSearchParameters());
    assertNotNull(updatedLagretSoek.getSearchParameters().getTittel());
    assertEquals(1, updatedLagretSoek.getSearchParameters().getTittel().size());
    assertEquals("\"multiple words\"", updatedLagretSoek.getSearchParameters().getTittel().get(0));

    // Clean up
    delete("/lagretSoek/" + localLagretSoekDTO.getId(), brukerToken);
  }

  @Test
  void testConvertLegacyLagretSoekWithTittelAnd() throws Exception {
    ReflectionTestUtils.setField(scheduler, "dryRun", false);

    // Create a legacy query with a search_tittel field
    var lagretSoekJSON = getLagretSoekJSON();
    lagretSoekJSON.put(
        "legacyQuery",
        """
        {
          "searchTerms":[{
            "field":"search_tittel",
            "searchTerm":"multiple words",
            "operator":"AND"
          }]
        }
        """);
    lagretSoekJSON.remove("searchParameters");
    var brukerId = brukerDTO.getId();
    var response = post("/bruker/" + brukerId + "/lagretSoek", lagretSoekJSON, brukerToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var localLagretSoekDTO = gson.fromJson(response.getBody(), LagretSoekDTO.class);

    // Run conversion
    scheduler.convertLegacyLagretSoek();
    awaitSideEffects();

    response = get("/lagretSoek/" + localLagretSoekDTO.getId(), brukerToken);
    var updatedLagretSoek = gson.fromJson(response.getBody(), LagretSoekDTO.class);

    // Verify that searchParameters.tittel contains the expected value
    assertNotNull(updatedLagretSoek.getSearchParameters());
    assertNotNull(updatedLagretSoek.getSearchParameters().getTittel());
    assertEquals(1, updatedLagretSoek.getSearchParameters().getTittel().size());
    assertEquals("(+multiple +words)", updatedLagretSoek.getSearchParameters().getTittel().get(0));

    // Clean up
    delete("/lagretSoek/" + localLagretSoekDTO.getId(), brukerToken);
  }

  @Test
  void testConvertLegacyLagretSoekWithTittelOr() throws Exception {
    ReflectionTestUtils.setField(scheduler, "dryRun", false);

    // Create a legacy query with a search_tittel field
    var lagretSoekJSON = getLagretSoekJSON();
    lagretSoekJSON.put(
        "legacyQuery",
        """
        {
          "searchTerms":[{
            "field":"search_tittel",
            "searchTerm":"multiple words",
            "operator":"OR"
          }]
        }
        """);
    lagretSoekJSON.remove("searchParameters");
    var brukerId = brukerDTO.getId();
    var response = post("/bruker/" + brukerId + "/lagretSoek", lagretSoekJSON, brukerToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var localLagretSoekDTO = gson.fromJson(response.getBody(), LagretSoekDTO.class);

    // Run conversion
    scheduler.convertLegacyLagretSoek();
    awaitSideEffects();

    response = get("/lagretSoek/" + localLagretSoekDTO.getId(), brukerToken);
    var updatedLagretSoek = gson.fromJson(response.getBody(), LagretSoekDTO.class);

    // Verify that searchParameters.tittel contains the expected value
    assertNotNull(updatedLagretSoek.getSearchParameters());
    assertNotNull(updatedLagretSoek.getSearchParameters().getTittel());
    assertEquals(1, updatedLagretSoek.getSearchParameters().getTittel().size());
    assertEquals("(multiple | words)", updatedLagretSoek.getSearchParameters().getTittel().get(0));

    // Clean up
    delete("/lagretSoek/" + localLagretSoekDTO.getId(), brukerToken);
  }

  @Test
  void testConvertLegacyLagretSoekDryRun() throws Exception {
    ReflectionTestUtils.setField(scheduler, "dryRun", true);

    scheduler.convertLegacyLagretSoek();
    awaitSideEffects();

    var response = get("/lagretSoek/" + lagretSoekDTO.getId(), brukerToken);
    var updatedLagretSoek = gson.fromJson(response.getBody(), LagretSoekDTO.class);

    // Verify that searchParameters did NOT get populated
    assertNull(updatedLagretSoek.getSearchParameters());

    // Verify that delete was not called on ES client
    captureDeletedDocuments(0);
  }

  // Test that streaming works as expected
  @Test
  void testConvertManyLegacyLagretSoek() throws Exception {
    ReflectionTestUtils.setField(scheduler, "dryRun", false);

    var createdLagretSoek = new ArrayList<LagretSoekDTO>();

    // Create 100 legacy lagretSoek
    for (int i = 0; i < 100; i++) {
      var lagretSoekJSON = getLagretSoekJSON();
      lagretSoekJSON.put("legacyQuery", "{\"searchTerm\":\"test" + i + "\"}");
      lagretSoekJSON.remove("searchParameters");
      var response =
          post("/bruker/" + brukerDTO.getId() + "/lagretSoek", lagretSoekJSON, brukerToken);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      createdLagretSoek.add(gson.fromJson(response.getBody(), LagretSoekDTO.class));
    }

    // Run conversion
    scheduler.convertLegacyLagretSoek();
    awaitSideEffects();

    // Verify all 100 lagretSoek were converted
    for (int i = 0; i < 100; i++) {
      var response = get("/lagretSoek/" + createdLagretSoek.get(i).getId(), brukerToken);
      var updatedLagretSoek = gson.fromJson(response.getBody(), LagretSoekDTO.class);

      // Verify that searchParameters got populated
      assertNotNull(updatedLagretSoek.getSearchParameters());
      assertEquals("test" + i, updatedLagretSoek.getSearchParameters().getQuery());
    }

    // Verify that delete was called on ES client (100 times + 1 from setup)
    captureDeletedDocuments(101);

    // Clean up all created lagretSoek
    for (var lagretSoek : createdLagretSoek) {
      delete("/lagretSoek/" + lagretSoek.getId(), brukerToken);
    }
  }
}
