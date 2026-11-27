package no.einnsyn.backend.entities.dokumentobjekt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import javax.sql.DataSource;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.exceptions.models.NetworkException;
import no.einnsyn.backend.common.statistics.models.StatisticsResponse;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.dokumentobjekt.models.Dokumentobjekt;
import no.einnsyn.backend.entities.dokumentobjekt.models.DokumentobjektDTO;
import no.einnsyn.backend.entities.downloadcount.DownloadCountService;
import no.einnsyn.backend.entities.downloadcount.DownloadCountTestService;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.tasks.TaskTestService;
import no.einnsyn.backend.tasks.handlers.linkcheck.FulltextLinkCheckScheduler;
import no.einnsyn.backend.utils.ApplicationShutdownListenerService;
import org.awaitility.Awaitility;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.AdditionalAnswers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DokumentobjektControllerTest extends EinnsynControllerTestBase {

  private static final String SOURCE_URL = "http://example.com/dokument.pdf";
  private static final String HTTPS_SOURCE_URL = "https://example.com/dokument.pdf";
  private static final String REFERENCE_URL = "http://reference.example.com/robots.txt";
  private static final int DEFAULT_DOWNLOAD_PROXY_PORT = 3128;

  @Value("${application.baseUrl}")
  private String baseUrl;

  @Autowired private DownloadCountTestService downloadCountTestService;
  @Autowired private TaskTestService taskTestService;
  @Autowired private FulltextLinkCheckScheduler fulltextLinkCheckScheduler;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private DataSource dataSource;

  private ArkivDTO arkivDTO;
  private ArkivdelDTO arkivdelDTO;
  private SaksmappeDTO saksmappeDTO;
  private JournalpostDTO journalpostDTO;
  private DokumentbeskrivelseDTO dokumentbeskrivelseDTO;
  private DokumentobjektDTO dokumentobjektDTO;

  @BeforeEach
  void setup() throws Exception {
    setDownloadProxy("", DEFAULT_DOWNLOAD_PROXY_PORT);
    setReferenceUrls(REFERENCE_URL);

    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", getJournalpostJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    response =
        post(
            "/journalpost/" + journalpostDTO.getId() + "/dokumentbeskrivelse",
            getDokumentbeskrivelseJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    dokumentbeskrivelseDTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);

    var dokumentobjektJSON = getDokumentobjektJSON();
    dokumentobjektJSON.put("referanseDokumentfil", SOURCE_URL);
    response =
        post(
            "/dokumentbeskrivelse/" + dokumentbeskrivelseDTO.getId() + "/dokumentobjekt",
            dokumentobjektJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    dokumentobjektDTO = gson.fromJson(response.getBody(), DokumentobjektDTO.class);
    assertNotNull(dokumentobjektDTO.getId());
  }

  @AfterEach
  void cleanup() throws Exception {
    // Download buckets outlive the fixtures below, so they are removed explicitly
    downloadCountTestService.deleteAll();
    if (saksmappeDTO != null) {
      var response = delete("/saksmappe/" + saksmappeDTO.getId());
      assertEquals(HttpStatus.OK, response.getStatusCode());
    }
    if (arkivDTO != null) {
      var response = delete("/arkiv/" + arkivDTO.getId());
      assertEquals(HttpStatus.OK, response.getStatusCode());
    }
  }

  @Test
  void downloadShouldReturnBadGatewayWhenProxyIsUnavailable() throws Exception {
    // We didn't start a proxy, so the download should fail with a masked network error
    var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");

    assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    var errorResponse = gson.fromJson(response.getBody(), NetworkException.ClientResponse.class);
    assertEquals("networkError", errorResponse.getType());
    assertEquals("Could not prepare download from source", errorResponse.getMessage());
  }

  @Test
  void downloadShouldReturnBadGatewayWhenClientDoesNotAcceptJson() throws Exception {
    var headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_OCTET_STREAM));
    var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download", headers);

    assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    assertTrue(MediaType.APPLICATION_JSON.isCompatibleWith(response.getHeaders().getContentType()));
    var errorResponse = gson.fromJson(response.getBody(), NetworkException.ClientResponse.class);
    assertEquals("networkError", errorResponse.getType());
  }

  @Test
  void createShouldNotReturnDownloadUrlWhenProxyIsNotConfigured() {
    assertNull(dokumentobjektDTO.getUrl());
  }

  @Test
  void getShouldReturnDownloadUrlWhenProxyIsConfigured() throws Exception {
    setDownloadProxy("localhost", DEFAULT_DOWNLOAD_PROXY_PORT);

    var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var refreshedDokumentobjektDTO = gson.fromJson(response.getBody(), DokumentobjektDTO.class);
    assertEquals(
        baseUrl + "/dokumentobjekt/" + refreshedDokumentobjektDTO.getId() + "/download",
        refreshedDokumentobjektDTO.getUrl());
  }

  @Test
  void downloadShouldUseProxyAndStreamSafeHeaders() throws Exception {
    try (var proxy =
        startProxyServer(
            HttpStatus.OK.value(),
            "application/pdf",
            "proxy-pdf-body".getBytes(StandardCharsets.UTF_8),
            "attachment; filename=\"another-title.pdf\"",
            null)) {
      var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals("application/pdf", response.getHeaders().getFirst("Content-Type"));
      assertEquals(
          "attachment; filename=\"dokument.pdf\"",
          response.getHeaders().getFirst("Content-Disposition"));
      assertEquals("proxy-pdf-body", response.getBody());

      var proxyRequests = proxy.requests();
      assertEquals(1, proxyRequests.size());
      var proxyRequest = proxyRequests.get(0);
      assertEquals("GET", proxyRequest.method());
      assertEquals(SOURCE_URL, proxyRequest.target());
      assertEquals("example.com", proxyRequest.hostHeader());
      assertEquals("eInnsyn", proxyRequest.userAgentHeader());
    }
  }

  @Test
  void anonymousDownloadShouldNotBeCached() throws Exception {
    try (var _ = startPdfProxy()) {
      var response = getAnon("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertTrue(response.getHeaders().getCacheControl().contains("no-store"));
    }
  }

  @Test
  void statisticsShouldCountStreamedDownloads() throws Exception {
    try (var _ = startPdfProxy()) {
      var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
      assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    var statisticsResponse = getJournalpostStatistics();
    assertEquals(1, statisticsResponse.getSummary().getDownloadCount());
    assertEquals(1, sumDownloadCount(statisticsResponse));
  }

  @Test
  void downloadByExternalIdShouldRecordStatisticsOnInternalId() throws Exception {
    var externalId = "external-download-id";
    var dokumentobjektJSON = getDokumentobjektJSON();
    dokumentobjektJSON.put("referanseDokumentfil", SOURCE_URL);
    dokumentobjektJSON.put("externalId", externalId);
    var response =
        post(
            "/dokumentbeskrivelse/" + dokumentbeskrivelseDTO.getId() + "/dokumentobjekt",
            dokumentobjektJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var externalDokumentobjektDTO = gson.fromJson(response.getBody(), DokumentobjektDTO.class);

    try (var _ = startPdfProxy()) {
      response = get("/dokumentobjekt/" + externalId + "/download");
      assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // The path variable is resolved to the internal id before it reaches the service. The bucket
    // must be keyed by that id, otherwise parent resolution and cleanup on delete never find it.
    assertEquals(1, downloadCountTestService.getDownloadCount(externalDokumentobjektDTO.getId()));
    assertEquals(0, downloadCountTestService.getDownloadCount(externalId));

    var statisticsResponse = getJournalpostStatistics();
    assertEquals(1, statisticsResponse.getSummary().getDownloadCount());
  }

  @Test
  void statisticsShouldCountMoetedokumentDownloadsOnMoetemappe() throws Exception {
    // A Moetedokument file has no Registrering above it, so its downloads attach to the Moetemappe.
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    response =
        post("/moetemappe/" + moetemappeDTO.getId() + "/moetedokument", getMoetedokumentJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetedokumentDTO = gson.fromJson(response.getBody(), MoetedokumentDTO.class);

    response =
        post(
            "/moetedokument/" + moetedokumentDTO.getId() + "/dokumentbeskrivelse",
            getDokumentbeskrivelseJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetedokumentDokbeskDTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);

    var dokumentobjektJSON = getDokumentobjektJSON();
    dokumentobjektJSON.put("referanseDokumentfil", SOURCE_URL);
    response =
        post(
            "/dokumentbeskrivelse/" + moetedokumentDokbeskDTO.getId() + "/dokumentobjekt",
            dokumentobjektJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetedokumentDokobjDTO = gson.fromJson(response.getBody(), DokumentobjektDTO.class);

    try (var _ = startPdfProxy()) {
      response = get("/dokumentobjekt/" + moetedokumentDokobjDTO.getId() + "/download");
      assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    var bucket = downloadCountTestService.findBuckets(moetedokumentDokobjDTO.getId()).getFirst();
    assertEquals(moetemappeDTO.getId(), bucket.getParentId());
    assertEquals(
        moetemappeDTO.getUtvalgObjekt().getId(),
        downloadCountTestService.getEnhetId(moetedokumentDokobjDTO.getId()));

    var statisticsResponse = getStatistics("Moetemappe");
    assertEquals(1, statisticsResponse.getSummary().getDownloadCount());
    assertEquals(1, sumDownloadCount(statisticsResponse));

    // The Journalpost in this test's fixture has no downloads, so the count is not leaking across.
    assertEquals(0, getJournalpostStatistics().getSummary().getDownloadCount());
  }

  @Test
  void downloadStatisticsShouldSurviveDeletingTheDokumentobjekt() throws Exception {
    try (var _ = startPdfProxy()) {
      var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
      assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // The download happened; deleting the file afterwards must not erase that.
    assertEquals(
        HttpStatus.OK, delete("/dokumentobjekt/" + dokumentobjektDTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/dokumentobjekt/" + dokumentobjektDTO.getId()).getStatusCode());

    var buckets = downloadCountTestService.findBuckets(dokumentobjektDTO.getId());
    assertEquals(1, buckets.size());
    var bucket = buckets.getFirst();
    assertEquals(1, bucket.getCount());

    // The bucket carries its own attribution, captured while the file still existed
    assertEquals(journalpostDTO.getId(), bucket.getParentId());
    assertEquals(
        journalpostDTO.getAdministrativEnhetObjekt().getId(),
        downloadCountTestService.getEnhetId(dokumentobjektDTO.getId()));

    // ...so it still counts for the Journalpost it belonged to
    var statisticsResponse = getJournalpostStatistics();
    assertEquals(1, statisticsResponse.getSummary().getDownloadCount());
    assertEquals(1, sumDownloadCount(statisticsResponse));
  }

  @Test
  void statisticsShouldAggregateMultipleDownloadsInSameBucket() throws Exception {
    try (var proxy = startPdfProxy()) {
      var firstResponse = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
      assertEquals(HttpStatus.OK, firstResponse.getStatusCode());

      var secondResponse = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
      assertEquals(HttpStatus.OK, secondResponse.getStatusCode());

      assertEquals(2, proxy.requests().size());
    }

    var statisticsResponse = getJournalpostStatistics();
    assertEquals(2, statisticsResponse.getSummary().getDownloadCount());
    assertEquals(2, sumDownloadCount(statisticsResponse));
  }

  @Test
  void statisticsShouldNotLoseConcurrentDownloads() throws Exception {
    var threads = 8;
    var downloadsPerThread = 5;
    var expectedDownloads = threads * downloadsPerThread;
    var dokumentobjektId = dokumentobjektDTO.getId();
    var startLatch = new CountDownLatch(1);
    var futures = new ArrayList<Future<?>>();

    try (var proxy = startPdfProxy();
        var executor = Executors.newFixedThreadPool(threads)) {
      for (var i = 0; i < threads; i++) {
        futures.add(
            executor.submit(
                () -> {
                  // Release all threads at once, so they contend for the same hourly bucket.
                  startLatch.await();
                  for (var j = 0; j < downloadsPerThread; j++) {
                    var response = get("/dokumentobjekt/" + dokumentobjektId + "/download");
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                  }
                  return null;
                }));
      }
      startLatch.countDown();
      for (var future : futures) {
        future.get(60, TimeUnit.SECONDS);
      }
      assertEquals(expectedDownloads, proxy.requests().size());
    }

    // A read-modify-write would lose downloads here, and would also fail requests outright once
    // the optimistic locking retries were exhausted.
    assertEquals(expectedDownloads, downloadCountTestService.getDownloadCount(dokumentobjektId));

    // The bucket is indexed once by the reindex scheduler, after all increments, so Elasticsearch
    // holds the final count rather than whichever concurrent index run happened to finish last.
    var statisticsResponse = getJournalpostStatistics();
    assertEquals(expectedDownloads, statisticsResponse.getSummary().getDownloadCount());
    assertEquals(expectedDownloads, sumDownloadCount(statisticsResponse));
  }

  @Test
  void downloadShouldNotHoldConnectionWhileWaitingForSource() throws Exception {
    var downloads = 3;
    var received = new CountDownLatch(downloads);
    var release = new CountDownLatch(1);
    var pool = dataSource.unwrap(HikariDataSource.class).getHikariPoolMXBean();
    var futures = new ArrayList<Future<ResponseEntity<String>>>();
    Runnable stall =
        () -> {
          received.countDown();
          try {
            release.await(30, TimeUnit.SECONDS);
          } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
          }
        };

    try (var _ =
            startProxyServer(
                HttpStatus.OK.value(),
                "application/pdf",
                "pdf-body".getBytes(StandardCharsets.UTF_8),
                null,
                null,
                stall);
        var executor = Executors.newFixedThreadPool(downloads)) {
      try {
        for (var i = 0; i < downloads; i++) {
          futures.add(
              executor.submit(
                  () -> getAnon("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download")));
        }
        assertTrue(received.await(10, TimeUnit.SECONDS));

        // Each held connection would also need a second one for the inner transactions
        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .until(() -> pool.getActiveConnections() == 0);
      } finally {
        release.countDown();
      }

      for (var future : futures) {
        assertEquals(HttpStatus.OK, future.get(30, TimeUnit.SECONDS).getStatusCode());
      }
    }
  }

  @Test
  void downloadShouldSucceedWhenRecordingStatisticsFails() throws Exception {
    var target = AopTestUtils.getTargetObject(dokumentobjektService);
    var originalDownloadCountService = ReflectionTestUtils.getField(target, "downloadCountService");
    var failingDownloadCountService = mock(DownloadCountService.class);
    doThrow(new DataIntegrityViolationException("simulated statistics failure"))
        .when(failingDownloadCountService)
        .recordDownload(anyString());
    ReflectionTestUtils.setField(target, "downloadCountService", failingDownloadCountService);

    try {
      try (var _ = startPdfProxy()) {
        var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");

        // The file was fetched successfully, so the failure to record statistics must not surface
        // to the client as a download error.
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("pdf-body", response.getBody());
      }
    } finally {
      ReflectionTestUtils.setField(target, "downloadCountService", originalDownloadCountService);
    }

    verify(failingDownloadCountService).recordDownload(anyString());
  }

  @Test
  void downloadShouldPreserveEncodedSourceUrlWhenProxying() throws Exception {
    var encodedSourceUrl = "http://example.com/file%20name.pdf?token=a%2Bb";
    var updateJson = new JSONObject();
    updateJson.put("referanseDokumentfil", encodedSourceUrl);
    patch("/dokumentobjekt/" + dokumentobjektDTO.getId(), updateJson);

    try (var proxy = startPdfProxy()) {
      var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(
          "attachment; filename=\"file-name.pdf\"",
          response.getHeaders().getFirst("Content-Disposition"));

      var proxyRequests = proxy.requests();
      assertEquals(1, proxyRequests.size());
      var proxyRequest = proxyRequests.get(0);
      assertEquals("GET", proxyRequest.method());
      assertEquals(encodedSourceUrl, proxyRequest.target());
      assertEquals("example.com", proxyRequest.hostHeader());
    }
  }

  @Test
  void downloadShouldReturnRedirectWhenProxyReturnsHtml() throws Exception {
    try (var proxy =
        startProxyServer(
            HttpStatus.OK.value(),
            "text/html; charset=utf-8",
            "<html><body>proxy</body></html>".getBytes(StandardCharsets.UTF_8),
            null,
            null)) {

      var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
      assertEquals(HttpStatus.FOUND, response.getStatusCode());
      assertEquals(SOURCE_URL, response.getHeaders().getFirst("Location"));

      var proxyRequests = proxy.requests();
      assertEquals(1, proxyRequests.size());
      var proxyRequest = proxyRequests.get(0);
      assertEquals("GET", proxyRequest.method());
      assertEquals(SOURCE_URL, proxyRequest.target());
      assertEquals("example.com", proxyRequest.hostHeader());
    }
  }

  @Test
  void statisticsShouldCountRedirectDownloads() throws Exception {
    try (var proxy =
        startProxyServer(
            HttpStatus.OK.value(),
            "text/html; charset=utf-8",
            "<html><body>proxy</body></html>".getBytes(StandardCharsets.UTF_8),
            null,
            null)) {

      var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
      assertEquals(HttpStatus.FOUND, response.getStatusCode());
      assertEquals(SOURCE_URL, response.getHeaders().getFirst("Location"));

      var proxyRequests = proxy.requests();
      assertEquals(1, proxyRequests.size());
    }

    var statisticsResponse = getJournalpostStatistics();
    assertEquals(1, statisticsResponse.getSummary().getDownloadCount());
    assertEquals(1, sumDownloadCount(statisticsResponse));
  }

  @Test
  void downloadShouldReturnBadGatewayWhenProxyReturnsNon2xx() throws Exception {
    try (var proxy =
        startProxyServer(
            HttpStatus.NOT_FOUND.value(),
            "text/plain",
            "missing".getBytes(StandardCharsets.UTF_8),
            null,
            null)) {

      var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
      assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
      var errorResponse = gson.fromJson(response.getBody(), NetworkException.ClientResponse.class);
      assertEquals("networkError", errorResponse.getType());
      assertEquals("Could not prepare download from source", errorResponse.getMessage());
      assertFalse(errorResponse.getMessage().contains(SOURCE_URL));
      assertNull(errorResponse.getBaseUrl());

      var proxyRequests = proxy.requests();
      assertEquals(1, proxyRequests.size());
      var proxyRequest = proxyRequests.get(0);
      assertEquals("GET", proxyRequest.method());
      assertEquals(SOURCE_URL, proxyRequest.target());
      assertEquals("example.com", proxyRequest.hostHeader());
    }
  }

  @Test
  void downloadFileNameUsesSourceFilename() throws Exception {
    // Source URL has "dokument.pdf" -> filename should be "dokument.pdf"
    try (var _ = startPdfProxy()) {
      var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(
          "attachment; filename=\"dokument.pdf\"",
          response.getHeaders().getFirst("Content-Disposition"));
    }
  }

  @Test
  void downloadFileNameSanitizesSourceFilename() throws Exception {
    // Update source URL to have special characters in filename
    var updateJson = new JSONObject();
    updateJson.put("referanseDokumentfil", "http://example.com/Ärende Rapport.pdf");
    patch("/dokumentobjekt/" + dokumentobjektDTO.getId(), updateJson);

    try (var _ = startPdfProxy()) {
      var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(
          "attachment; filename=\"arende-rapport.pdf\"",
          response.getHeaders().getFirst("Content-Disposition"));
    }
  }

  @Test
  void downloadFileNameUsesSourceFilenameWithoutExtension() throws Exception {
    var updateJson = new JSONObject();
    updateJson.put("referanseDokumentfil", "http://example.com/referansedokument");
    patch("/dokumentobjekt/" + dokumentobjektDTO.getId(), updateJson);

    try (var _ = startPdfProxy()) {
      var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(
          "attachment; filename=\"referansedokument\"",
          response.getHeaders().getFirst("Content-Disposition"));
    }
  }

  @Test
  void downloadFileNameUsesFormatWhenSourceFilenameHasNoExtension() throws Exception {
    var updateJson = new JSONObject();
    updateJson.put("referanseDokumentfil", "http://example.com/referansedokument");
    updateJson.put("format", "pdf");
    patch("/dokumentobjekt/" + dokumentobjektDTO.getId(), updateJson);

    try (var _ = startPdfProxy()) {
      var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(
          "attachment; filename=\"referansedokument.pdf\"",
          response.getHeaders().getFirst("Content-Disposition"));
    }
  }

  @Test
  void downloadFileNameFallsBackToTitleAndDokumentnummer() throws Exception {
    // Source URL has no filename -> fall back to dokumentbeskrivelse title + nummer
    var updateJson = new JSONObject();
    updateJson.put("referanseDokumentfil", "http://example.com/path/");
    patch("/dokumentobjekt/" + dokumentobjektDTO.getId(), updateJson);

    try (var _ = startPdfProxy()) {
      var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(
          "attachment; filename=\"testtittel-1\"",
          response.getHeaders().getFirst("Content-Disposition"));
    }
  }

  @Test
  void downloadFileNameFallsBackToTitleAndDokumentnummerWithFormat() throws Exception {
    // Source URL has no filename, but dokumentobjekt has a format
    var updateJson = new JSONObject();
    updateJson.put("referanseDokumentfil", "http://example.com/path/");
    updateJson.put("format", "pdf");
    patch("/dokumentobjekt/" + dokumentobjektDTO.getId(), updateJson);

    try (var _ = startPdfProxy()) {
      var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(
          "attachment; filename=\"testtittel-1.pdf\"",
          response.getHeaders().getFirst("Content-Disposition"));
    }
  }

  @Test
  void downloadFileNameFallsBackToDokumentnummerOnly() throws Exception {
    // Source URL has no filename, title is blank but dokumentnummer is still set
    var updateJson = new JSONObject();
    updateJson.put("referanseDokumentfil", "http://example.com/path/");
    patch("/dokumentobjekt/" + dokumentobjektDTO.getId(), updateJson);

    // Clear the title but keep dokumentnummer (1)
    var dokbeskJson = new JSONObject();
    dokbeskJson.put("tittel", " ");
    patch("/dokumentbeskrivelse/" + dokumentbeskrivelseDTO.getId(), dokbeskJson);

    try (var _ = startPdfProxy()) {
      var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(
          "attachment; filename=\"einnsyn-download-1\"",
          response.getHeaders().getFirst("Content-Disposition"));
    }
  }

  @Test
  void downloadShouldMarkLinkAsFailedWhenSourceRespondsWithError() throws Exception {
    useHttpsSource();
    try (var _ = startHttpsSourceProxy("HTTP/1.1 404 Not Found")) {
      var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
      assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    }

    assertNotNull(getDokumentobjekt().getLinkFailedSince());
  }

  @Test
  void downloadShouldNotCheckReferenceUrlsWithoutResponseFromSource() throws Exception {
    // The client shouldn't wait for a check of the reference URLs, so the link is left for the
    // link check
    useHttpsSource();
    try (var proxy = startTunnelProxy("HTTP/1.1 503 Service Unavailable")) {
      var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
      assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
      assertEquals(List.of("CONNECT example.com:443 HTTP/1.1"), proxy.requests());
    }

    var dokumentobjekt = getDokumentobjekt();
    assertNull(dokumentobjekt.getLinkCheckedAt());
    assertNull(dokumentobjekt.getLinkFailedSince());
    assertTrue(dokumentobjektService.isProxyWorking());
  }

  @Test
  void downloadShouldNotMarkLinkAsFailedWhenProxyIsUnavailable() throws Exception {
    // The proxy is unavailable, so the failure is on our side and says nothing about the link
    useHttpsSource();
    useUnavailableProxy();
    var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
    assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());

    assertNull(getDokumentobjekt().getLinkFailedSince());
  }

  @Test
  void downloadShouldMarkHttpLinkAsFailedEvenWhenSourceResponds() throws Exception {
    // Fulltext files must be served over HTTPS
    try (var _ = startPdfProxy()) {
      var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
      assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    assertNotNull(getDokumentobjekt().getLinkFailedSince());
  }

  @Test
  void checkLinkShouldMarkHttpLinkAsFailedWithoutRequest() throws Exception {
    try (var proxy = startPdfProxy()) {
      dokumentobjektService.checkLink(dokumentobjektDTO.getId());
      assertEquals(0, proxy.requests().size());
    }

    var dokumentobjekt = getDokumentobjekt();
    assertNotNull(dokumentobjekt.getLinkCheckedAt());
    assertNotNull(dokumentobjekt.getLinkFailedSince());
  }

  @Test
  void checkLinkShouldMarkLinkOnOtherPortThan443AsFailedWithoutRequest() throws Exception {
    // The proxy only allows port 443, so the link can never be downloaded
    useSource("https://example.com:8443/dokument.pdf");
    try (var proxy = startTunnelProxy("HTTP/1.1 403 Forbidden")) {
      dokumentobjektService.checkLink(dokumentobjektDTO.getId());
      assertEquals(0, proxy.requests().size());
    }

    var dokumentobjekt = getDokumentobjekt();
    assertNotNull(dokumentobjekt.getLinkCheckedAt());
    assertNotNull(dokumentobjekt.getLinkFailedSince());
  }

  @Test
  void checkLinkShouldAllowExplicitPort443() throws Exception {
    useSource("https://example.com:443/dokument.pdf");
    try (var proxy = startTunnelProxy("HTTP/1.1 403 Forbidden")) {
      dokumentobjektService.checkLink(dokumentobjektDTO.getId());
      assertEquals("CONNECT example.com:443 HTTP/1.1", proxy.requests().getFirst());
    }
  }

  @Test
  void checkLinkShouldMarkLinkAsFailedWhenReferenceUrlWorks() throws Exception {
    useHttpsSource();
    try (var proxy = startTunnelProxy("HTTP/1.1 503 Service Unavailable")) {
      dokumentobjektService.checkLink(dokumentobjektDTO.getId());
      assertEquals(
          List.of("CONNECT example.com:443 HTTP/1.1", "GET " + REFERENCE_URL + " HTTP/1.1"),
          proxy.requests());
    }

    assertNotNull(getDokumentobjekt().getLinkFailedSince());
  }

  @Test
  void checkLinkShouldNotMarkLinkAsFailedWhenReferenceUrlAlsoFails() throws Exception {
    // When the reference URLs can't be reached either, our network is down, not the source. The
    // link is left as it is, so it is checked first when the proxy works again.
    useHttpsSource();
    try (var _ = startTunnelProxy("HTTP/1.1 503 Service Unavailable", null)) {
      dokumentobjektService.checkLink(dokumentobjektDTO.getId());
    }

    var dokumentobjekt = getDokumentobjekt();
    assertNull(dokumentobjekt.getLinkCheckedAt());
    assertNull(dokumentobjekt.getLinkFailedSince());
    assertFalse(dokumentobjektService.isProxyWorking());
  }

  @Test
  void checkLinkShouldNotCheckReferenceUrlsWhenSourceResponds() throws Exception {
    // A response from the source proves that the proxy works
    useHttpsSource();
    try (var proxy = startHttpsSourceProxy("HTTP/1.1 404 Not Found")) {
      dokumentobjektService.checkLink(dokumentobjektDTO.getId());
      assertEquals(List.of("CONNECT example.com:443 HTTP/1.1"), proxy.requests());
    }

    assertNotNull(getDokumentobjekt().getLinkFailedSince());
  }

  @Test
  void checkLinkShouldNotMarkLinkAsFailedWhenProxyIsUnavailable() throws Exception {
    // The proxy is unavailable, so the link is left as it is
    useHttpsSource();
    useUnavailableProxy();
    dokumentobjektService.checkLink(dokumentobjektDTO.getId());

    var dokumentobjekt = getDokumentobjekt();
    assertNull(dokumentobjekt.getLinkCheckedAt());
    assertNull(dokumentobjekt.getLinkFailedSince());
  }

  @Test
  void checkLinkShouldKeepFirstFailureTimeWhenLinkKeepsFailing() {
    dokumentobjektService.checkLink(dokumentobjektDTO.getId());
    var firstCheck = getDokumentobjekt();
    assertNotNull(firstCheck.getLinkFailedSince());

    dokumentobjektService.checkLink(dokumentobjektDTO.getId());
    var secondCheck = getDokumentobjekt();
    assertEquals(firstCheck.getLinkFailedSince(), secondCheck.getLinkFailedSince());
    assertTrue(secondCheck.getLinkCheckedAt().isAfter(firstCheck.getLinkCheckedAt()));
  }

  @Test
  void updateShouldNotOverwriteLinkStatusFromConcurrentCheck() {
    // The entity is loaded before the link check commits, and saved after it
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            _ -> {
              var dokumentobjekt = getDokumentobjekt();
              dokumentobjektRepository.setLinkFailed(
                  dokumentobjektDTO.getId(), SOURCE_URL, Instant.now());
              dokumentobjekt.setDokumentFormat("application/pdf");
            });

    var dokumentobjekt = getDokumentobjekt();
    assertEquals("application/pdf", dokumentobjekt.getDokumentFormat());
    assertNotNull(dokumentobjekt.getLinkCheckedAt());
    assertNotNull(dokumentobjekt.getLinkFailedSince());
  }

  @Test
  void updateShouldResetLinkStatusWhenUrlChanges() throws Exception {
    dokumentobjektService.checkLink(dokumentobjektDTO.getId());
    assertNotNull(getDokumentobjekt().getLinkFailedSince());

    useHttpsSource();

    var dokumentobjekt = getDokumentobjekt();
    assertNull(dokumentobjekt.getLinkCheckedAt());
    assertNull(dokumentobjekt.getLinkFailedSince());
  }

  @Test
  void linkStatusShouldNotBeRecordedForAnOldUrl() throws Exception {
    // A check of the old URL finishes after the URL was changed, and must not touch the new one
    useHttpsSource();
    var checkedAt = Instant.now();
    dokumentobjektRepository.setLinkFailed(dokumentobjektDTO.getId(), SOURCE_URL, checkedAt);
    dokumentobjektRepository.setLinkChecked(dokumentobjektDTO.getId(), SOURCE_URL, checkedAt);
    dokumentobjektRepository.setLinkOk(dokumentobjektDTO.getId(), SOURCE_URL, checkedAt);

    var dokumentobjekt = getDokumentobjekt();
    assertNull(dokumentobjekt.getLinkCheckedAt());
    assertNull(dokumentobjekt.getLinkFailedSince());

    // The status of the URL that is stored is recorded as usual
    dokumentobjektRepository.setLinkFailed(dokumentobjektDTO.getId(), HTTPS_SOURCE_URL, checkedAt);
    assertNotNull(getDokumentobjekt().getLinkFailedSince());
  }

  @Test
  void updateShouldKeepLinkStatusWhenUrlIsUnchanged() throws Exception {
    dokumentobjektService.checkLink(dokumentobjektDTO.getId());

    var updateJson = new JSONObject();
    updateJson.put("referanseDokumentfil", SOURCE_URL);
    var response = patch("/dokumentobjekt/" + dokumentobjektDTO.getId(), updateJson);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var dokumentobjekt = getDokumentobjekt();
    assertNotNull(dokumentobjekt.getLinkCheckedAt());
    assertNotNull(dokumentobjekt.getLinkFailedSince());
  }

  @Test
  void linkCheckSchedulerShouldCheckLinks() throws Exception {
    useHttpsSource();
    try (var proxy = startTunnelProxy("HTTP/1.1 503 Service Unavailable")) {
      fulltextLinkCheckScheduler.checkLinks();
      // The link check, and the reference check after no response
      assertEquals(2, proxy.requests().size());
    }

    var dokumentobjekt = getDokumentobjekt();
    assertNotNull(dokumentobjekt.getLinkCheckedAt());
    assertNotNull(dokumentobjekt.getLinkFailedSince());
  }

  @Test
  void linkCheckSchedulerShouldDoNothingWhenRateIsZero() throws Exception {
    useHttpsSource();
    var target = AopTestUtils.getTargetObject(fulltextLinkCheckScheduler);
    var originalRate = ReflectionTestUtils.getField(target, "rate");
    ReflectionTestUtils.setField(target, "rate", 0.0);
    try (var proxy = startTunnelProxy("HTTP/1.1 503 Service Unavailable")) {
      fulltextLinkCheckScheduler.checkLinks();
      assertEquals(0, proxy.requests().size());
    } finally {
      ReflectionTestUtils.setField(target, "rate", originalRate);
    }

    assertNull(getDokumentobjekt().getLinkCheckedAt());
  }

  @Test
  void linkCheckSchedulerShouldStopWhenShuttingDown() throws Exception {
    assertLinkCheckSkippedOnShutdown(true);
  }

  @Test
  void linkCheckSchedulerShouldNotStartCheckWhenShutdownStartsWhileWaitingForSlot()
      throws Exception {
    // The loop sees no shutdown, but the shutdown starts before the check gets a free slot
    assertLinkCheckSkippedOnShutdown(false, true);
  }

  private void assertLinkCheckSkippedOnShutdown(Boolean first, Boolean... next) throws Exception {
    useHttpsSource();
    var target = AopTestUtils.getTargetObject(fulltextLinkCheckScheduler);
    var shutdownListener = mock(ApplicationShutdownListenerService.class);
    when(shutdownListener.isShuttingDown()).thenReturn(first, next);
    var originalShutdownListener =
        ReflectionTestUtils.getField(target, "applicationShutdownListenerService");
    ReflectionTestUtils.setField(target, "applicationShutdownListenerService", shutdownListener);
    try (var proxy = startTunnelProxy("HTTP/1.1 503 Service Unavailable")) {
      fulltextLinkCheckScheduler.checkLinks();
      assertEquals(List.of(), proxy.requests());
    } finally {
      ReflectionTestUtils.setField(
          target, "applicationShutdownListenerService", originalShutdownListener);
    }

    // The link is left for the next run
    assertNull(getDokumentobjekt().getLinkCheckedAt());
  }

  @Test
  void linkCheckSchedulerShouldCheckOldestLinksFirstEvenIfOthersFailed() throws Exception {
    // Failed links must not be prioritized, or many failures could starve the other links
    var response =
        post(
            "/dokumentbeskrivelse/" + dokumentbeskrivelseDTO.getId() + "/dokumentobjekt",
            getDokumentobjektJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var okDTO = gson.fromJson(response.getBody(), DokumentobjektDTO.class);

    var okCheckedAt = Instant.now().minus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
    var failedCheckedAt = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
    dokumentobjektRepository.setLinkOk(okDTO.getId(), HTTPS_SOURCE_URL, okCheckedAt);
    dokumentobjektRepository.setLinkFailed(dokumentobjektDTO.getId(), SOURCE_URL, failedCheckedAt);

    try (var _ = startTunnelProxy("HTTP/1.1 503 Service Unavailable")) {
      checkLinksWithBatchSize(1);
    }

    var ok = dokumentobjektRepository.findById(okDTO.getId()).orElseThrow();
    assertTrue(ok.getLinkCheckedAt().isAfter(okCheckedAt));
    assertEquals(failedCheckedAt, getDokumentobjekt().getLinkCheckedAt());
  }

  @Test
  void linkCheckSchedulerShouldCheckUncheckedLinksFirst() throws Exception {
    var response =
        post(
            "/dokumentbeskrivelse/" + dokumentbeskrivelseDTO.getId() + "/dokumentobjekt",
            getDokumentobjektJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var uncheckedDTO = gson.fromJson(response.getBody(), DokumentobjektDTO.class);

    var checkedAt = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
    dokumentobjektRepository.setLinkOk(dokumentobjektDTO.getId(), SOURCE_URL, checkedAt);

    try (var _ = startTunnelProxy("HTTP/1.1 503 Service Unavailable")) {
      checkLinksWithBatchSize(1);
    }

    var unchecked = dokumentobjektRepository.findById(uncheckedDTO.getId()).orElseThrow();
    assertNotNull(unchecked.getLinkCheckedAt());
    assertEquals(checkedAt, getDokumentobjekt().getLinkCheckedAt());
  }

  @Test
  void linkCheckSchedulerShouldPauseWhenReferenceUrlsFail() throws Exception {
    useHttpsSource();
    try (var proxy = startTunnelProxy("HTTP/1.1 503 Service Unavailable", null)) {
      // The link fails without a response, and the reference URLs fail too, so the job pauses
      fulltextLinkCheckScheduler.checkLinks();
      assertFalse(dokumentobjektService.isProxyWorking());

      // While paused, neither the job nor the link checks make any requests
      proxy.requests().clear();
      fulltextLinkCheckScheduler.checkLinks();
      dokumentobjektService.checkLink(dokumentobjektDTO.getId());
      assertEquals(List.of(), proxy.requests());

      // The reference URLs are checked every minute while the proxy is down. The JDK retries a GET
      // once when the connection is closed without a response, so only distinct requests count.
      fulltextLinkCheckScheduler.checkProxyWhileDown();
      assertEquals(
          List.of("GET " + REFERENCE_URL + " HTTP/1.1"),
          proxy.requests().stream().distinct().toList());
    }

    assertNull(getDokumentobjekt().getLinkCheckedAt());
  }

  @Test
  void linkCheckSchedulerShouldResumeWhenReferenceUrlWorksAgain() throws Exception {
    useHttpsSource();
    try (var _ = startTunnelProxy("HTTP/1.1 503 Service Unavailable", null)) {
      fulltextLinkCheckScheduler.checkLinks();
    }
    assertNull(getDokumentobjekt().getLinkCheckedAt());

    try (var _ = startTunnelProxy("HTTP/1.1 503 Service Unavailable")) {
      fulltextLinkCheckScheduler.checkProxyWhileDown();
      assertTrue(dokumentobjektService.isProxyWorking());
      fulltextLinkCheckScheduler.checkLinks();
    }
    assertNotNull(getDokumentobjekt().getLinkFailedSince());
  }

  @ParameterizedTest
  @ValueSource(ints = {429, 500})
  void checkProxyWhileDownShouldAcceptAnyResponseFromHttpsReferenceUrl(int status)
      throws Exception {
    // Over HTTPS, the response came through the tunnel from the reference URL itself, so the proxy
    // works, even when the reference URL is rate limited after many checks
    setReferenceUrls("https://example.com/generate_204");
    setProxyWorking(false);
    try (var proxy = startHttpsSourceProxy("HTTP/1.1 " + status + " Error")) {
      fulltextLinkCheckScheduler.checkProxyWhileDown();
      assertEquals(List.of("CONNECT example.com:443 HTTP/1.1"), proxy.requests());
    }

    assertTrue(dokumentobjektService.isProxyWorking());
  }

  @Test
  void checkProxyWhileDownShouldNotCheckReferenceUrlsWhenProxyWorks() throws Exception {
    try (var proxy = startTunnelProxy("HTTP/1.1 503 Service Unavailable")) {
      fulltextLinkCheckScheduler.checkProxyWhileDown();
      assertEquals(List.of(), proxy.requests());
    }
  }

  @Test
  void checkLinkShouldMarkLinkAsOkWhenHttpsSourceResponds() throws Exception {
    useHttpsSource();
    try (var _ = startHttpsSourceProxy("HTTP/1.1 200 OK")) {
      dokumentobjektService.checkLink(dokumentobjektDTO.getId());
    }

    var dokumentobjekt = getDokumentobjekt();
    assertNotNull(dokumentobjekt.getLinkCheckedAt());
    assertNull(dokumentobjekt.getLinkFailedSince());
  }

  @Test
  void downloadShouldNotMarkLinkAsFailedWhenSourceIsRateLimited() throws Exception {
    useHttpsSource();
    try (var _ = startHttpsSourceProxy("HTTP/1.1 429 Too Many Requests")) {
      var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
      assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    }

    var dokumentobjekt = getDokumentobjekt();
    assertNull(dokumentobjekt.getLinkCheckedAt());
    assertNull(dokumentobjekt.getLinkFailedSince());
  }

  @Test
  void checkLinkShouldOnlyRequestTheFirstByte() throws Exception {
    useHttpsSource();
    try (var proxy =
        startHttpsSourceProxy(
            range -> range != null ? "HTTP/1.1 206 Partial Content" : "HTTP/1.1 200 OK")) {
      dokumentobjektService.checkLink(dokumentobjektDTO.getId());
      assertEquals(List.of("bytes=0-0"), proxy.sourceRanges());
    }

    var dokumentobjekt = getDokumentobjekt();
    assertNotNull(dokumentobjekt.getLinkCheckedAt());
    assertNull(dokumentobjekt.getLinkFailedSince());
  }

  @Test
  void checkLinkShouldAskSourceToCloseConnection() throws Exception {
    // Otherwise the JDK reads the rest of a small file to keep the connection alive
    useHttpsSource();
    try (var proxy =
        startHttpsSourceProxy(
            range -> range != null ? "HTTP/1.1 416 Range Not Satisfiable" : "HTTP/1.1 200 OK")) {
      dokumentobjektService.checkLink(dokumentobjektDTO.getId());
      // Also when retrying without range
      assertEquals(List.of("close", "close"), proxy.sourceConnections());
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 403, 405, 416, 500, 501, 503})
  void checkLinkShouldRetryWithoutRangeWhenSourceRejectsIt(int status) throws Exception {
    useHttpsSource();
    try (var proxy =
        startHttpsSourceProxy(
            range -> range != null ? "HTTP/1.1 " + status + " Error" : "HTTP/1.1 200 OK")) {
      dokumentobjektService.checkLink(dokumentobjektDTO.getId());
      assertEquals(Arrays.asList("bytes=0-0", null), proxy.sourceRanges());
    }

    var dokumentobjekt = getDokumentobjekt();
    assertNotNull(dokumentobjekt.getLinkCheckedAt());
    assertNull(dokumentobjekt.getLinkFailedSince());
  }

  @Test
  void checkLinkShouldMarkLinkAsFailedWhenRetryWithoutRangeFails() throws Exception {
    useHttpsSource();
    try (var proxy = startHttpsSourceProxy("HTTP/1.1 400 Bad Request")) {
      dokumentobjektService.checkLink(dokumentobjektDTO.getId());
      assertEquals(Arrays.asList("bytes=0-0", null), proxy.sourceRanges());
    }

    assertNotNull(getDokumentobjekt().getLinkFailedSince());
  }

  @ParameterizedTest
  @ValueSource(ints = {404, 410, 429})
  void checkLinkShouldNotRetryWithoutRangeOnFinalStatuses(int status) throws Exception {
    // These don't depend on the range, so asking again would only double the requests
    useHttpsSource();
    try (var proxy = startHttpsSourceProxy("HTTP/1.1 " + status + " Error")) {
      dokumentobjektService.checkLink(dokumentobjektDTO.getId());
      assertEquals(List.of("bytes=0-0"), proxy.sourceRanges());
    }
  }

  @Test
  void downloadShouldNotSendRangeHeader() throws Exception {
    useHttpsSource();
    try (var proxy = startHttpsSourceProxy("HTTP/1.1 200 OK")) {
      get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
      assertEquals(Arrays.asList((String) null), proxy.sourceRanges());
      assertNotEquals("close", proxy.sourceConnections().getFirst());
    }
  }

  @Test
  void checkLinkShouldNotMarkLinkAsFailedWhenRecordingItAsOkFails() throws Exception {
    // A database failure after the source responded must not be taken for a failed link
    useHttpsSource();
    var target = AopTestUtils.getTargetObject(dokumentobjektService);
    var repository = ReflectionTestUtils.getField(target, "repository");
    var failingRepository =
        mock(DokumentobjektRepository.class, AdditionalAnswers.delegatesTo(repository));
    doThrow(new IllegalStateException("Database unavailable"))
        .when(failingRepository)
        .setLinkOk(anyString(), anyString(), any());
    ReflectionTestUtils.setField(target, "repository", failingRepository);
    try (var proxy = startHttpsSourceProxy("HTTP/1.1 200 OK")) {
      assertThrows(
          IllegalStateException.class,
          () -> dokumentobjektService.checkLink(dokumentobjektDTO.getId()));
      // The reference URLs are not checked
      assertEquals(List.of("CONNECT example.com:443 HTTP/1.1"), proxy.requests());
    } finally {
      ReflectionTestUtils.setField(target, "repository", repository);
    }

    var dokumentobjekt = getDokumentobjekt();
    assertNull(dokumentobjekt.getLinkCheckedAt());
    assertNull(dokumentobjekt.getLinkFailedSince());
  }

  @Test
  void checkLinkShouldSkipLinkWhenSourceIsRateLimited() throws Exception {
    // The link is moved to the back of the queue without being marked as failed
    useHttpsSource();
    try (var proxy = startHttpsSourceProxy("HTTP/1.1 429 Too Many Requests")) {
      dokumentobjektService.checkLink(dokumentobjektDTO.getId());
      assertEquals(List.of("CONNECT example.com:443 HTTP/1.1"), proxy.requests());
    }

    var dokumentobjekt = getDokumentobjekt();
    assertNotNull(dokumentobjekt.getLinkCheckedAt());
    assertNull(dokumentobjekt.getLinkFailedSince());
  }

  @Test
  void checkLinkShouldUseLinkCheckReadTimeout() throws Exception {
    // A proxy that accepts connections, but never responds
    useHttpsSource();
    var target = AopTestUtils.getTargetObject(dokumentobjektService);
    try (var silentProxy = new ServerSocket(0)) {
      List<Socket> sockets = Collections.synchronizedList(new ArrayList<>());
      Thread.ofVirtual()
          .start(
              () -> {
                while (!silentProxy.isClosed()) {
                  try {
                    sockets.add(silentProxy.accept());
                  } catch (IOException _) {
                    // The server socket was closed
                  }
                }
              });
      setDownloadProxy("localhost", silentProxy.getLocalPort());
      ReflectionTestUtils.setField(target, "linkCheckReadTimeout", Duration.ofMillis(200));

      var start = Instant.now();
      dokumentobjektService.checkLink(dokumentobjektDTO.getId());
      // The link check and the reference URL time out, far below the download timeout
      assertTrue(Duration.between(start, Instant.now()).toSeconds() < 5);
      for (var socket : sockets) {
        socket.close();
      }
    } finally {
      ReflectionTestUtils.setField(target, "linkCheckReadTimeout", Duration.ofSeconds(30));
    }

    // The reference URL timed out too, so the failure is on our side
    assertNull(getDokumentobjekt().getLinkFailedSince());
    assertFalse(dokumentobjektService.isProxyWorking());
  }

  @Test
  void linkCheckSchedulerShouldStartChecksAtTheConfiguredRate() throws Exception {
    // checkLink stores when it started in linkCheckedAt, so the gaps between the starts are
    // measured
    // without the time it takes to set up and close the proxy
    var ids = new ArrayList<>(List.of(dokumentobjektDTO.getId()));
    for (var i = 0; i < 3; i++) {
      var response =
          post(
              "/dokumentbeskrivelse/" + dokumentbeskrivelseDTO.getId() + "/dokumentobjekt",
              getDokumentobjektJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      ids.add(gson.fromJson(response.getBody(), DokumentobjektDTO.class).getId());
    }

    useHttpsSource();
    var target = AopTestUtils.getTargetObject(fulltextLinkCheckScheduler);
    var originalRate = ReflectionTestUtils.getField(target, "rate");
    ReflectionTestUtils.setField(target, "rate", 10.0);
    try (var proxy = startHttpsSourceProxy("HTTP/1.1 404 Not Found")) {
      fulltextLinkCheckScheduler.checkLinks();
      assertEquals(4, proxy.requests().size());
    } finally {
      ReflectionTestUtils.setField(target, "rate", originalRate);
    }

    // Ten a second is one every 100 ms. Without the rate, the checks start within a few ms.
    var startTimes =
        ids.stream()
            .map(id -> dokumentobjektRepository.findById(id).orElseThrow().getLinkCheckedAt())
            .sorted()
            .toList();
    for (var i = 1; i < startTimes.size(); i++) {
      var gap = Duration.between(startTimes.get(i - 1), startTimes.get(i));
      assertTrue(gap.toMillis() >= 75, "Gap between checks was " + gap);
    }
  }

  @Test
  void linkCheckSchedulerShouldStopBatchWhenProxyGoesDown() throws Exception {
    // The first link fails without a response, and so do the reference URLs. The rest of the batch
    // is left for the next run, instead of being ticked through at the rate without any checks.
    useHttpsSource();
    var otherIds = new ArrayList<String>();
    for (var i = 0; i < 4; i++) {
      var response =
          post(
              "/dokumentbeskrivelse/" + dokumentbeskrivelseDTO.getId() + "/dokumentobjekt",
              getDokumentobjektJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      otherIds.add(gson.fromJson(response.getBody(), DokumentobjektDTO.class).getId());
    }
    // The https link is first in the queue
    var checkedAt = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
    for (var id : otherIds) {
      dokumentobjektRepository.setLinkOk(id, HTTPS_SOURCE_URL, checkedAt);
    }

    var target = AopTestUtils.getTargetObject(fulltextLinkCheckScheduler);
    var originalRate = ReflectionTestUtils.getField(target, "rate");
    ReflectionTestUtils.setField(target, "rate", 1.0);
    var start = System.nanoTime();
    try (var proxy = startTunnelProxy("HTTP/1.1 503 Service Unavailable", null)) {
      fulltextLinkCheckScheduler.checkLinks();
      assertEquals(
          List.of("CONNECT example.com:443 HTTP/1.1", "GET " + REFERENCE_URL + " HTTP/1.1"),
          proxy.requests().stream().distinct().toList());
    } finally {
      ReflectionTestUtils.setField(target, "rate", originalRate);
    }

    // Five links at one a second would take at least four seconds
    assertTrue(Duration.ofNanos(System.nanoTime() - start).toMillis() < 3000);
    assertFalse(dokumentobjektService.isProxyWorking());
    for (var id : otherIds) {
      assertEquals(
          checkedAt, dokumentobjektRepository.findById(id).orElseThrow().getLinkCheckedAt());
    }
  }

  private void checkLinksWithBatchSize(int batchSize) {
    var target = AopTestUtils.getTargetObject(fulltextLinkCheckScheduler);
    var originalBatchSize = ReflectionTestUtils.getField(target, "batchSize");
    ReflectionTestUtils.setField(target, "batchSize", batchSize);
    try {
      fulltextLinkCheckScheduler.checkLinks();
    } finally {
      ReflectionTestUtils.setField(target, "batchSize", originalBatchSize);
    }
  }

  @Test
  void linkCheckSchedulerShouldSkipWhenProxyIsNotConfigured() {
    fulltextLinkCheckScheduler.checkLinks();

    assertNull(getDokumentobjekt().getLinkCheckedAt());
  }

  private Dokumentobjekt getDokumentobjekt() {
    return dokumentobjektRepository.findById(dokumentobjektDTO.getId()).orElseThrow();
  }

  private void useHttpsSource() throws Exception {
    useSource(HTTPS_SOURCE_URL);
  }

  private void useSource(String url) throws Exception {
    var updateJson = new JSONObject();
    updateJson.put("referanseDokumentfil", url);
    var response = patch("/dokumentobjekt/" + dokumentobjektDTO.getId(), updateJson);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  /** Point the download proxy to a local port that nothing listens on. */
  private void useUnavailableProxy() throws Exception {
    int port;
    try (var socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }
    setDownloadProxy("localhost", port);
  }

  /**
   * Start a proxy that answers HTTPS tunnel requests (CONNECT) with the given status line, like a
   * proxy that refuses or can't open the tunnel. The JDK HttpServer used by startProxyServer()
   * doesn't support CONNECT.
   */
  private StartedTunnelProxy startTunnelProxy(String statusLine) throws Exception {
    return startTunnelProxy(statusLine, "HTTP/1.1 200 OK");
  }

  /**
   * Like startTunnelProxy(String), but answers plain HTTP requests (e.g. to the reference URLs)
   * with the given status line instead of 200 OK, or closes the connection without a response when
   * it is null.
   */
  private StartedTunnelProxy startTunnelProxy(String connectStatusLine, String httpStatusLine)
      throws Exception {
    return startTunnelProxy(connectStatusLine, httpStatusLine, null, null);
  }

  /**
   * Start a proxy that opens HTTPS tunnels to a source answering with the given status line, like a
   * source responding through the proxy. The source's certificate is self-signed for example.com,
   * and trusted until the proxy is closed. Plain HTTP requests (e.g. to the reference URLs) are
   * answered with 200 OK.
   */
  private StartedTunnelProxy startHttpsSourceProxy(String sourceStatusLine) throws Exception {
    return startHttpsSourceProxy(_ -> sourceStatusLine);
  }

  /**
   * Like startHttpsSourceProxy(String), but the source's status line depends on the Range header of
   * the request (null without one). The Range and Connection headers are recorded in sourceRanges()
   * and sourceConnections().
   */
  private StartedTunnelProxy startHttpsSourceProxy(Function<String, String> sourceStatusForRange)
      throws Exception {
    var keyStoreFile = Files.createTempDirectory("source").resolve("source.p12");
    var keytool = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
    var keytoolProcess =
        new ProcessBuilder(
                keytool,
                "-genkeypair",
                "-keyalg",
                "RSA",
                "-alias",
                "source",
                "-validity",
                "1",
                "-dname",
                "CN=example.com",
                "-ext",
                "SAN=dns:example.com",
                "-storetype",
                "PKCS12",
                "-keystore",
                keyStoreFile.toString(),
                "-storepass",
                "changeit")
            .redirectErrorStream(true)
            .start();
    keytoolProcess.getInputStream().readAllBytes();
    assertEquals(0, keytoolProcess.waitFor());
    var keyStore = KeyStore.getInstance(keyStoreFile.toFile(), "changeit".toCharArray());

    var keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keyStore, "changeit".toCharArray());
    var trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(keyStore);
    var sslContext = SSLContext.getInstance("TLS");
    sslContext.init(
        keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

    var defaultSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
    HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
    var proxy = startTunnelProxy(null, "HTTP/1.1 200 OK", sslContext, sourceStatusForRange);
    return new StartedTunnelProxy(
        proxy.serverSocket(),
        proxy.requests(),
        proxy.sourceRanges(),
        proxy.sourceConnections(),
        () -> HttpsURLConnection.setDefaultSSLSocketFactory(defaultSocketFactory));
  }

  /**
   * Without a tunnelSslContext, CONNECT requests are answered with connectStatusLine. With it, the
   * tunnel is opened, and the request inside it is answered over TLS with the status line that
   * sourceStatusForRange gives for its Range header.
   */
  private StartedTunnelProxy startTunnelProxy(
      String connectStatusLine,
      String httpStatusLine,
      SSLContext tunnelSslContext,
      Function<String, String> sourceStatusForRange)
      throws Exception {
    var serverSocket = new ServerSocket(0);
    List<String> requests = Collections.synchronizedList(new ArrayList<>());
    List<String> sourceRanges = Collections.synchronizedList(new ArrayList<>());
    List<String> sourceConnections = Collections.synchronizedList(new ArrayList<>());
    Thread.ofVirtual()
        .start(
            () -> {
              while (!serverSocket.isClosed()) {
                try (var socket = serverSocket.accept()) {
                  var request = readRequest(socket);
                  requests.add(request);
                  var isConnect = request != null && request.startsWith("CONNECT ");
                  if (isConnect && tunnelSslContext != null) {
                    respond(socket, "HTTP/1.1 200 Connection established");
                    try (var tlsSocket =
                        (SSLSocket)
                            tunnelSslContext
                                .getSocketFactory()
                                .createSocket(socket, null, socket.getPort(), false)) {
                      tlsSocket.setUseClientMode(false);
                      var lines = readRequestLines(tlsSocket);
                      var range = header(lines, "Range");
                      sourceRanges.add(range);
                      sourceConnections.add(header(lines, "Connection"));
                      respond(tlsSocket, sourceStatusForRange.apply(range));
                    }
                  } else {
                    var statusLine = isConnect ? connectStatusLine : httpStatusLine;
                    if (statusLine != null) {
                      respond(socket, statusLine);
                    }
                  }
                } catch (IOException _) {
                  // The server socket was closed
                }
              }
            });

    setDownloadProxy("localhost", serverSocket.getLocalPort());
    return new StartedTunnelProxy(
        serverSocket, requests, sourceRanges, sourceConnections, () -> {});
  }

  /** Read a request, and return its request line. The headers are skipped. */
  private static String readRequest(Socket socket) throws IOException {
    return readRequestLines(socket).stream().findFirst().orElse(null);
  }

  /** The value of a header in the given request lines, or null without one. */
  private static String header(List<String> requestLines, String name) {
    var prefix = name + ":";
    return requestLines.stream()
        .filter(line -> line.regionMatches(true, 0, prefix, 0, prefix.length()))
        .map(line -> line.substring(prefix.length()).strip())
        .findFirst()
        .orElse(null);
  }

  /** Read a request, and return its request line and headers. */
  private static List<String> readRequestLines(Socket socket) throws IOException {
    // Read byte by byte, so nothing after the headers (e.g. a TLS handshake) is consumed
    var input = socket.getInputStream();
    var lines = new ArrayList<String>();
    var line = new StringBuilder();
    int b;
    while ((b = input.read()) != -1) {
      if (b == '\n') {
        var text = line.toString().strip();
        if (text.isEmpty()) {
          break;
        }
        lines.add(text);
        line.setLength(0);
      } else {
        line.append((char) b);
      }
    }
    return lines;
  }

  private static void respond(Socket socket, String statusLine) throws IOException {
    var response = statusLine + "\r\nContent-Length: 0\r\nConnection: close\r\n\r\n";
    socket.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
    socket.getOutputStream().flush();
  }

  private StartedProxy startPdfProxy() throws Exception {
    return startProxyServer(
        HttpStatus.OK.value(),
        "application/pdf",
        "pdf-body".getBytes(StandardCharsets.UTF_8),
        null,
        null);
  }

  /** When the source omits the Content-Type, it is guessed from the file name. */
  @Test
  void downloadShouldGuessContentTypeWhenSourceOmitsIt() throws Exception {
    try (var proxy =
        startProxyServer(
            HttpStatus.OK.value(), null, "pdf-body".getBytes(StandardCharsets.UTF_8), null, null)) {
      var response = get("/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals("application/pdf", response.getHeaders().getFirst("Content-Type"));
      assertEquals("pdf-body", response.getBody());
    }
  }

  /** Unknown file types without a Content-Type fall back to application/octet-stream. */
  @Test
  void downloadShouldFallBackToOctetStreamForUnknownContentType() throws Exception {
    var dokumentobjektJSON = getDokumentobjektJSON();
    dokumentobjektJSON.put("referanseDokumentfil", "http://example.com/dokument.ukjent");
    var response =
        post(
            "/dokumentbeskrivelse/" + dokumentbeskrivelseDTO.getId() + "/dokumentobjekt",
            dokumentobjektJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var unknownTypeDTO = gson.fromJson(response.getBody(), DokumentobjektDTO.class);

    try (var proxy =
        startProxyServer(
            HttpStatus.OK.value(), null, "body".getBytes(StandardCharsets.UTF_8), null, null)) {
      response = get("/dokumentobjekt/" + unknownTypeDTO.getId() + "/download");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals("application/octet-stream", response.getHeaders().getFirst("Content-Type"));
    }
  }

  private StartedProxy startProxyServer(
      int returnStatusCode,
      String returnContentType,
      byte[] returnBody,
      String returnContentDisposition,
      String returnLocation)
      throws Exception {
    return startProxyServer(
        returnStatusCode,
        returnContentType,
        returnBody,
        returnContentDisposition,
        returnLocation,
        () -> {});
  }

  private StartedProxy startProxyServer(
      int returnStatusCode,
      String returnContentType,
      byte[] returnBody,
      String returnContentDisposition,
      String returnLocation,
      Runnable beforeResponse)
      throws Exception {
    List<ProxyRequest> requests = Collections.synchronizedList(new ArrayList<>());
    var server = HttpServer.create(new InetSocketAddress(0), 0);
    // Without an executor, handlers run on the single dispatcher thread, which would serialize
    // concurrent downloads.
    var executor = Executors.newCachedThreadPool();
    server.setExecutor(executor);
    server.createContext(
        "/",
        exchange -> {
          requests.add(
              new ProxyRequest(
                  exchange.getRequestMethod(),
                  exchange.getRequestURI().toString(),
                  exchange.getRequestHeaders().getFirst("Host"),
                  exchange.getRequestHeaders().getFirst("User-Agent")));
          beforeResponse.run();

          if (returnContentType != null) {
            exchange.getResponseHeaders().set("Content-Type", returnContentType);
          }
          if (returnContentDisposition != null) {
            exchange.getResponseHeaders().set("Content-Disposition", returnContentDisposition);
          }
          if (returnLocation != null) {
            exchange.getResponseHeaders().set("Location", returnLocation);
          }

          exchange.sendResponseHeaders(returnStatusCode, returnBody.length);
          try (var outputStream = exchange.getResponseBody()) {
            outputStream.write(returnBody);
          }
        });
    server.start();

    // Update the service to use our proxy server
    setDownloadProxy("localhost", server.getAddress().getPort());

    return new StartedProxy(server, executor, requests);
  }

  /** Set the reference URLs, and forget any earlier result of checking them. */
  private void setReferenceUrls(String... urls) {
    var target = AopTestUtils.getTargetObject(dokumentobjektService);
    ReflectionTestUtils.setField(target, "referenceUrls", List.of(urls));
    ReflectionTestUtils.setField(target, "proxyWorking", true);
  }

  private void setProxyWorking(boolean proxyWorking) {
    var target = AopTestUtils.getTargetObject(dokumentobjektService);
    ReflectionTestUtils.setField(target, "proxyWorking", proxyWorking);
  }

  private void setDownloadProxy(String host, int port) {
    var target = AopTestUtils.getTargetObject(dokumentobjektService);
    ReflectionTestUtils.setField(target, "downloadProxyHost", host);
    ReflectionTestUtils.setField(target, "downloadProxyPort", port);
  }

  private StatisticsResponse getJournalpostStatistics() throws Exception {
    return getStatistics("Journalpost");
  }

  private StatisticsResponse getStatistics(String entity) throws Exception {
    // Downloads are not indexed per request; the hourly reindex scheduler picks up the buckets.
    taskTestService.updateOutdatedDocuments();
    esClient.indices().refresh(r -> r.index(elasticsearchIndex));
    // The statistics endpoint interprets the dates as UTC, while the buckets were created just now
    // in local time. Pad the range by a day on each side so this does not fail around midnight.
    var from = LocalDate.now().minusDays(1).toString();
    var to = LocalDate.now().plusDays(1).toString();
    var response =
        get("/statistics?aggregateFrom=" + from + "&aggregateTo=" + to + "&entity=" + entity);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    return gson.fromJson(response.getBody(), StatisticsResponse.class);
  }

  private int sumDownloadCount(StatisticsResponse statisticsResponse) {
    return statisticsResponse.getTimeSeries().stream()
        .map(StatisticsResponse.TimeSeries::getDownloadCount)
        .filter(Objects::nonNull)
        .mapToInt(Integer::intValue)
        .sum();
  }

  private record StartedProxy(
      HttpServer server, ExecutorService executor, List<ProxyRequest> requests)
      implements AutoCloseable {
    @Override
    public void close() {
      server.stop(0);
      executor.shutdown();
    }
  }

  private record StartedTunnelProxy(
      ServerSocket serverSocket,
      List<String> requests,
      List<String> sourceRanges,
      List<String> sourceConnections,
      Runnable onClose)
      implements AutoCloseable {

    @Override
    public void close() throws IOException {
      serverSocket.close();
      onClose.run();
    }
  }

  private record ProxyRequest(
      String method, String target, String hostHeader, String userAgentHeader) {}
}
