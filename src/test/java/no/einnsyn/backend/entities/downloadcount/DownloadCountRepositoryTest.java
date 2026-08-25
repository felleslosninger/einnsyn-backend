package no.einnsyn.backend.entities.downloadcount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.dokumentobjekt.models.DokumentobjektDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DownloadCountRepositoryTest extends EinnsynControllerTestBase {

  private static final long AWAIT_TIMEOUT_SECONDS = 60;

  @Autowired private DownloadCountService downloadCountService;
  @Autowired private DownloadCountTestService downloadCountTestService;

  private ArkivDTO arkivDTO;
  private SaksmappeDTO saksmappeDTO;
  private DokumentobjektDTO dokumentobjektDTO;

  @BeforeEach
  void setup() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", getJournalpostJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    response =
        post(
            "/journalpost/" + journalpostDTO.getId() + "/dokumentbeskrivelse",
            getDokumentbeskrivelseJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var dokumentbeskrivelseDTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);

    response =
        post(
            "/dokumentbeskrivelse/" + dokumentbeskrivelseDTO.getId() + "/dokumentobjekt",
            getDokumentobjektJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    dokumentobjektDTO = gson.fromJson(response.getBody(), DokumentobjektDTO.class);
    assertNotNull(dokumentobjektDTO.getId());
  }

  @AfterEach
  void cleanup() throws Exception {
    if (saksmappeDTO != null) {
      assertEquals(HttpStatus.OK, delete("/saksmappe/" + saksmappeDTO.getId()).getStatusCode());
    }
    if (arkivDTO != null) {
      assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());
    }
  }

  /**
   * A transaction that starts before another but reaches the bucket afterwards must not stamp the
   * update timestamp with its own (older) start time. If it did, the timestamp would move backwards
   * while the count kept changing, and the reindex scheduler's {@code last_indexed < _updated}
   * check would never select the row again, leaving Elasticsearch permanently behind the database.
   */
  @Test
  void recordDownloadShouldNotMoveUpdatedTimestampBackwards() throws Exception {
    var dokumentobjektId = dokumentobjektDTO.getId();
    var earlyTransactionStarted = new CountDownLatch(1);
    var lateDownloadRecorded = new CountDownLatch(1);

    try (var executor = Executors.newSingleThreadExecutor()) {
      var early =
          executor.submit(
              () -> {
                downloadCountTestService.recordDownloadInPinnedTransaction(
                    dokumentobjektId, earlyTransactionStarted, lateDownloadRecorded);
                return null;
              });

      assertTrue(
          earlyTransactionStarted.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
          "Timed out waiting for the early transaction to start");

      // Creates the bucket from a transaction that starts after the one above.
      downloadCountService.recordDownload(dokumentobjektId);
      var updatedAfterLateDownload = downloadCountTestService.getUpdated(dokumentobjektId);

      lateDownloadRecorded.countDown();
      early.get(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

      var updatedAfterEarlyDownload = downloadCountTestService.getUpdated(dokumentobjektId);
      assertEquals(2, downloadCountTestService.getDownloadCount(dokumentobjektId));
      assertFalse(
          updatedAfterEarlyDownload.isBefore(updatedAfterLateDownload),
          "Update timestamp moved backwards: "
              + updatedAfterEarlyDownload
              + " is before "
              + updatedAfterLateDownload);
    }
  }
}
