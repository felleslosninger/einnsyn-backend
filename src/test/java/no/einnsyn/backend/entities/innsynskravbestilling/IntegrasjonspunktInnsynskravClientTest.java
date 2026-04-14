package no.einnsyn.backend.entities.innsynskravbestilling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import no.einnsyn.backend.common.exceptions.models.NetworkException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class IntegrasjonspunktInnsynskravClientTest {

  private static final String MOVE_URL_WITH_TRAILING_SLASHES = "http://integrasjonspunkt:9093///";
  private static final String UPLOAD_URL =
      "http://integrasjonspunkt:9093/api/messages/out/multipart";
  private static final String APPLICATION_NAME = "einnsyn-backend";
  private static final String IP_ORGNUMMER = "123456789";
  private static final int EXPECTED_RESPONSE_TIMEOUT_DAYS = 30;
  private static final String USERNAME = "integrasjonspunkt-user";
  private static final String PASSWORD = "integrasjonspunkt-pass";

  private ObjectMapper objectMapper;

  @BeforeEach
  void setup() {
    objectMapper = new ObjectMapper().findAndRegisterModules();
  }

  @Test
  void sendInnsynskravShouldPostExpectedMultipartRequest() throws Exception {
    var client =
        new IntegrasjonspunktInnsynskravClient(
            new Gson(),
            APPLICATION_NAME,
            MOVE_URL_WITH_TRAILING_SLASHES,
            EXPECTED_RESPONSE_TIMEOUT_DAYS,
            "",
            "",
            IP_ORGNUMMER);
    var requestHeadersHolder = new AtomicReference<HttpHeaders>();
    var requestBodyHolder = new AtomicReference<byte[]>();
    var server = getServer(client);
    server
        .expect(requestTo(UPLOAD_URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(
            request -> {
              var mockRequest = (MockClientHttpRequest) request;
              requestHeadersHolder.set(request.getHeaders());
              requestBodyHolder.set(mockRequest.getBodyAsBytes());
            })
        .andRespond(withSuccess());

    var orderXml = "<order>test</order>";
    var handteresAvOrgnummer = "987654321";
    var dataOwnerOrgnummer = "112233445";
    var email = "innsyn@example.com";
    var emailText = "This is a test";
    var before = OffsetDateTime.now();

    var transactionId =
        client.sendInnsynskrav(
            orderXml, handteresAvOrgnummer, dataOwnerOrgnummer, email, emailText);

    var after = OffsetDateTime.now();
    server.verify();

    var headers = requestHeadersHolder.get();
    assertNotNull(headers);
    assertNotNull(headers.getContentType());
    assertTrue(headers.getContentType().toString().startsWith(MediaType.MULTIPART_FORM_DATA_VALUE));
    assertEquals(APPLICATION_NAME, headers.getFirst(HttpHeaders.USER_AGENT));

    var parts = parseMultipartBody(requestBodyHolder.get(), headers.getContentType());
    assertEquals(3, parts.size());

    var sbdPart = parts.get("sbd");
    assertNotNull(sbdPart);
    assertNull(sbdPart.filename());

    var orderXmlPart = parts.get("order.xml");
    assertNotNull(orderXmlPart);
    assertEquals("order.xml", orderXmlPart.filename());
    assertEquals(orderXml, orderXmlPart.content());

    var emailTextPart = parts.get("emailtext");
    assertNotNull(emailTextPart);
    assertEquals("emailtext", emailTextPart.filename());
    assertEquals(emailText, emailTextPart.content());

    var sbd = objectMapper.readTree(sbdPart.content());
    assertEquals(2, sbd.size());
    assertFalse(sbd.has("publisering"));

    // Header
    assertEquals("1.0", sbd.at("/standardBusinessDocumentHeader/headerVersion").asText());
    assertEquals(5, sbd.at("/standardBusinessDocumentHeader").size());

    // Sender
    assertEquals(
        "0192:" + IP_ORGNUMMER,
        sbd.at("/standardBusinessDocumentHeader/sender/0/identifier/value").asText());
    assertEquals(
        "iso6523-actorid-upis",
        sbd.at("/standardBusinessDocumentHeader/sender/0/identifier/authority").asText());
    assertEquals(1, sbd.at("/standardBusinessDocumentHeader/sender/0").size());

    // Receiver
    assertEquals(
        "0192:" + handteresAvOrgnummer,
        sbd.at("/standardBusinessDocumentHeader/receiver/0/identifier/value").asText());
    assertEquals(1, sbd.at("/standardBusinessDocumentHeader/receiver/0").size());

    // Document identification
    assertEquals(
        "urn:no:difi:einnsyn:xsd::innsynskrav",
        sbd.at("/standardBusinessDocumentHeader/documentIdentification/standard").asText());
    assertEquals(
        "1.0",
        sbd.at("/standardBusinessDocumentHeader/documentIdentification/typeVersion").asText());
    assertEquals(
        "innsynskrav",
        sbd.at("/standardBusinessDocumentHeader/documentIdentification/type").asText());
    var messageId =
        sbd.at("/standardBusinessDocumentHeader/documentIdentification/instanceIdentifier")
            .asText();
    assertFalse(messageId.isBlank());
    assertEquals(4, sbd.at("/standardBusinessDocumentHeader/documentIdentification").size());
    assertTrue(
        sbd.at("/standardBusinessDocumentHeader/documentIdentification/creationDateAndTime")
            .isMissingNode());
    assertTrue(
        sbd.at("/standardBusinessDocumentHeader/documentIdentification/multipleType")
            .isMissingNode());

    // Manifest
    assertTrue(sbd.at("/standardBusinessDocumentHeader/manifest").isMissingNode());

    // Business scope
    assertEquals(
        "ConversationId",
        sbd.at("/standardBusinessDocumentHeader/businessScope/scope/0/type").asText());
    assertEquals(
        "urn:no:difi:profile:einnsyn:innsynskrav:ver1.0",
        sbd.at("/standardBusinessDocumentHeader/businessScope/scope/0/identifier").asText());
    var conversationId =
        sbd.at("/standardBusinessDocumentHeader/businessScope/scope/0/instanceIdentifier").asText();
    assertFalse(conversationId.isBlank());
    assertEquals(transactionId, conversationId);
    assertNotEquals(messageId, conversationId);
    assertEquals(4, sbd.at("/standardBusinessDocumentHeader/businessScope/scope/0").size());
    assertEquals(
        1,
        sbd.at("/standardBusinessDocumentHeader/businessScope/scope/0/scopeInformation/0").size());
    assertTrue(
        sbd.at(
                "/standardBusinessDocumentHeader/businessScope/scope/0/scopeInformation/0/requestingDocumentCreationDateTime")
            .isMissingNode());
    assertTrue(
        sbd.at(
                "/standardBusinessDocumentHeader/businessScope/scope/0/scopeInformation/0/requestingDocumentInstanceIdentifier")
            .isMissingNode());

    var expectedResponseDateTime =
        OffsetDateTime.parse(
            sbd.at(
                    "/standardBusinessDocumentHeader/businessScope/scope/0/scopeInformation/0/expectedResponseDateTime")
                .asText());
    assertFalse(
        expectedResponseDateTime.isBefore(
            before.plusDays(EXPECTED_RESPONSE_TIMEOUT_DAYS).minusSeconds(1)));
    assertFalse(
        expectedResponseDateTime.isAfter(
            after.plusDays(EXPECTED_RESPONSE_TIMEOUT_DAYS).plusSeconds(1)));

    // Innsynskrav
    assertEquals(dataOwnerOrgnummer, sbd.at("/innsynskrav/orgnr").asText());
    assertEquals(email, sbd.at("/innsynskrav/epost").asText());
    assertEquals(2, sbd.at("/innsynskrav").size());
    assertTrue(sbd.at("/innsynskrav/sikkerhetsnivaa").isMissingNode());
    assertTrue(sbd.at("/innsynskrav/hoveddokument").isMissingNode());
  }

  @Test
  void sendInnsynskravShouldSkipUserAgentWhenApplicationNameIsMissing() throws Exception {
    var client =
        new IntegrasjonspunktInnsynskravClient(
            new Gson(),
            "",
            MOVE_URL_WITH_TRAILING_SLASHES,
            EXPECTED_RESPONSE_TIMEOUT_DAYS,
            "",
            "",
            IP_ORGNUMMER);
    var requestHeadersHolder = new AtomicReference<HttpHeaders>();
    var server = getServer(client);
    server
        .expect(requestTo(UPLOAD_URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(request -> requestHeadersHolder.set(request.getHeaders()))
        .andRespond(withSuccess());

    client.sendInnsynskrav(
        "<order>test</order>", "987654321", "112233445", "innsyn@example.com", "This is a test");
    server.verify();

    assertNull(requestHeadersHolder.get().getFirst(HttpHeaders.USER_AGENT));
  }

  @Test
  void sendInnsynskravShouldSetBasicAuthWhenConfigured() throws Exception {
    var client =
        new IntegrasjonspunktInnsynskravClient(
            new Gson(),
            APPLICATION_NAME,
            MOVE_URL_WITH_TRAILING_SLASHES,
            EXPECTED_RESPONSE_TIMEOUT_DAYS,
            USERNAME,
            PASSWORD,
            IP_ORGNUMMER);
    var requestHeadersHolder = new AtomicReference<HttpHeaders>();
    var server = getServer(client);
    server
        .expect(requestTo(UPLOAD_URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(request -> requestHeadersHolder.set(request.getHeaders()))
        .andRespond(withSuccess());

    client.sendInnsynskrav(
        "<order>test</order>", "987654321", "112233445", "innsyn@example.com", "This is a test");
    server.verify();

    assertEquals(
        expectedBasicAuth(), requestHeadersHolder.get().getFirst(HttpHeaders.AUTHORIZATION));
  }

  @Test
  void sendInnsynskravShouldSkipBasicAuthWhenConfigurationIsIncomplete() throws Exception {
    var client =
        new IntegrasjonspunktInnsynskravClient(
            new Gson(),
            APPLICATION_NAME,
            MOVE_URL_WITH_TRAILING_SLASHES,
            EXPECTED_RESPONSE_TIMEOUT_DAYS,
            USERNAME,
            "",
            IP_ORGNUMMER);
    var requestHeadersHolder = new AtomicReference<HttpHeaders>();
    var server = getServer(client);
    server
        .expect(requestTo(UPLOAD_URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(request -> requestHeadersHolder.set(request.getHeaders()))
        .andRespond(withSuccess());

    client.sendInnsynskrav(
        "<order>test</order>", "987654321", "112233445", "innsyn@example.com", "This is a test");
    server.verify();

    assertNull(requestHeadersHolder.get().getFirst(HttpHeaders.AUTHORIZATION));
  }

  @Test
  void sendInnsynskravShouldThrowWhenRestTemplateFails() {
    var client =
        new IntegrasjonspunktInnsynskravClient(
            new Gson(),
            "",
            MOVE_URL_WITH_TRAILING_SLASHES,
            EXPECTED_RESPONSE_TIMEOUT_DAYS,
            "",
            "",
            IP_ORGNUMMER);
    var server = getServer(client);
    server
        .expect(requestTo(UPLOAD_URL))
        .andRespond(withException(new IOException("connection refused")));

    var ex =
        assertThrows(
            NetworkException.class,
            () ->
                client.sendInnsynskrav(
                    "<order/>", "987654321", "112233445", "e@example.com", "text"));

    server.verify();
    assertTrue(ex.getMessage().contains("Could not send innsynskrav"));
  }

  private MockRestServiceServer getServer(IntegrasjonspunktInnsynskravClient client) {
    return MockRestServiceServer.bindTo(getRestTemplate(client)).build();
  }

  private RestTemplate getRestTemplate(IntegrasjonspunktInnsynskravClient client) {
    return (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
  }

  private Map<String, MultipartPart> parseMultipartBody(byte[] body, MediaType contentType)
      throws Exception {
    var multipart = new MimeMultipart(new ByteArrayDataSource(body, contentType.toString()));
    var parts = new LinkedHashMap<String, MultipartPart>();

    for (var index = 0; index < multipart.getCount(); index++) {
      var bodyPart = multipart.getBodyPart(index);
      var dispositionHeaders = bodyPart.getHeader(HttpHeaders.CONTENT_DISPOSITION);
      assertNotNull(dispositionHeaders, "Missing content disposition in multipart part");
      assertEquals(1, dispositionHeaders.length);

      var disposition = ContentDisposition.parse(dispositionHeaders[0]);
      var name = disposition.getName();
      assertNotNull(name, "Missing part name in multipart content disposition");
      var filename = disposition.getFilename();
      var content = new String(bodyPart.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      parts.put(name, new MultipartPart(filename, content));
    }
    return parts;
  }

  private String expectedBasicAuth() {
    return "Basic "
        + Base64.getEncoder()
            .encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
  }

  private record MultipartPart(String filename, String content) {}
}
