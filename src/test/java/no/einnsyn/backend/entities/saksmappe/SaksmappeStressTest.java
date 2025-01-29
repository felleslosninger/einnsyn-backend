package no.einnsyn.backend.entities.saksmappe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import no.einnsyn.backend.EinnsynLegacyElasticTestBase;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SaksmappeStressTest extends EinnsynLegacyElasticTestBase {

  @Test
  void testInsertPerformance() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    var threadCount = 5;
    var requestsPerThread = 100;
    var requests = threadCount * requestsPerThread;

    Runnable task =
        () -> {
          try {
            for (int i = 0; i < requestsPerThread; i++) {
              var saksmappeJSON = getSaksmappeJSON();
              saksmappeJSON.put(
                  "journalpost",
                  new JSONArray(
                      List.of(getJournalpostJSON(), getJournalpostJSON(), getJournalpostJSON())));
              var subResponse =
                  post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
              assertEquals(HttpStatus.CREATED, subResponse.getStatusCode());
            }
          } catch (Exception e) {
          }
        };

    var start = System.currentTimeMillis();
    var threads = new Thread[threadCount];
    for (int i = 0; i < threadCount; i++) {
      threads[i] = new Thread(task);
      threads[i].start();
    }

    for (int i = 0; i < threadCount; i++) {
      threads[i].join();
    }

    var end = System.currentTimeMillis();
    var requestsPerSecond = requests / ((end - start) / 1000.0);
    assertTrue(
        requestsPerSecond > 10,
        "should be able to handle at least 10 requests per second, was " + requestsPerSecond);

    // Wait for all documents to be indexed
    captureIndexedDocuments(4 * requests);

    response = delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    captureDeletedDocuments(4 * requests); // Each request has 1 saksmappe, 3 journalpost
  }
}
