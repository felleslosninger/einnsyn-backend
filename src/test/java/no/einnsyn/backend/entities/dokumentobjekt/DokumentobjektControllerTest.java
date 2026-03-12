package no.einnsyn.backend.entities.dokumentobjekt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.exceptions.models.NetworkException;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.dokumentobjekt.models.DokumentobjektDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DokumentobjektControllerTest extends EinnsynControllerTestBase {

  private static final String SOURCE_URL = "http://example.com/dokument.pdf";

  @Value("${application.baseUrl}")
  private String baseUrl;

  private ArkivDTO arkivDTO;
  private SaksmappeDTO saksmappeDTO;
  private DokumentbeskrivelseDTO dokumentbeskrivelseDTO;
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
  void createShouldReturnDownloadUrl() {
    assertEquals(
        baseUrl + "/dokumentobjekt/" + dokumentobjektDTO.getId() + "/download",
        dokumentobjektDTO.getUrl());
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
    }
  }

  @Test
  void downloadShouldPreserveEncodedSourceUrlWhenProxying() throws Exception {
    var encodedSourceUrl = "http://example.com/file%20name.pdf?token=a%2Bb";
    var updateJson = new org.json.JSONObject();
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
    var updateJson = new org.json.JSONObject();
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
  void downloadFileNameFallsBackToTitleAndDokumentnummer() throws Exception {
    // Source URL has no filename -> fall back to dokumentbeskrivelse title + nummer
    var updateJson = new org.json.JSONObject();
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
    var updateJson = new org.json.JSONObject();
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
    var updateJson = new org.json.JSONObject();
    updateJson.put("referanseDokumentfil", "http://example.com/path/");
    patch("/dokumentobjekt/" + dokumentobjektDTO.getId(), updateJson);

    // Clear the title but keep dokumentnummer (1)
    var dokbeskJson = new org.json.JSONObject();
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

  private StartedProxy startProxyServer(
      int returnStatusCode,
      String returnContentType,
      byte[] returnBody,
      String returnContentDisposition,
      String returnLocation)
      throws Exception {
    List<ProxyRequest> requests = Collections.synchronizedList(new ArrayList<>());
    var server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/",
        exchange -> {
          requests.add(
              new ProxyRequest(
                  exchange.getRequestMethod(),
                  exchange.getRequestURI().toString(),
                  exchange.getRequestHeaders().getFirst("Host")));

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
    var target = AopTestUtils.getTargetObject(dokumentobjektService);
    ReflectionTestUtils.setField(target, "downloadProxyHost", "localhost");
    ReflectionTestUtils.setField(target, "downloadProxyPort", server.getAddress().getPort());

    return new StartedProxy(server, requests);
  }

  private record StartedProxy(HttpServer server, List<ProxyRequest> requests)
      implements AutoCloseable {
    @Override
    public void close() {
      server.stop(0);
    }
  }

  private record ProxyRequest(String method, String target, String hostHeader) {}
}
