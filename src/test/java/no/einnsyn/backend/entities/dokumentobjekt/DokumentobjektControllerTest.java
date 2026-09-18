package no.einnsyn.backend.entities.dokumentobjekt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.exceptions.models.NetworkException;
import no.einnsyn.backend.common.statistics.models.StatisticsResponse;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.dokumentobjekt.models.DokumentobjektDTO;
import no.einnsyn.backend.entities.downloadcount.DownloadCountService;
import no.einnsyn.backend.entities.downloadcount.DownloadCountTestService;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.tasks.TaskTestService;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DokumentobjektControllerTest extends EinnsynControllerTestBase {

  private static final String SOURCE_URL = "http://example.com/dokument.pdf";
  private static final int DEFAULT_DOWNLOAD_PROXY_PORT = 3128;

  @Value("${application.baseUrl}")
  private String baseUrl;

  @Autowired private DownloadCountTestService downloadCountTestService;
  @Autowired private TaskTestService taskTestService;

  private ArkivDTO arkivDTO;
  private ArkivdelDTO arkivdelDTO;
  private SaksmappeDTO saksmappeDTO;
  private JournalpostDTO journalpostDTO;
  private DokumentbeskrivelseDTO dokumentbeskrivelseDTO;
  private DokumentobjektDTO dokumentobjektDTO;

  @BeforeEach
  void setup() throws Exception {
    setDownloadProxy("", DEFAULT_DOWNLOAD_PROXY_PORT);

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

  private record ProxyRequest(
      String method, String target, String hostHeader, String userAgentHeader) {}
}
