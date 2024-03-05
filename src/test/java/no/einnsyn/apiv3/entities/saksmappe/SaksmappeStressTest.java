package no.einnsyn.apiv3.entities.saksmappe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SaksmappeStressTest extends EinnsynControllerTestBase {

  @Test
  void testInsertPerformance() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    var testArkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    var threadCount = 10;
    var requestsPerThread = 150;
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
                  post("/arkiv/" + testArkivDTO.getId() + "/saksmappe", saksmappeJSON);
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
        requestsPerSecond > 100, "should be able to handle at least 100 requests per second");

    response = delete("/arkiv/" + testArkivDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}
