package no.einnsyn.backend.entities.innsynskravbestilling;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import no.einnsyn.backend.common.exceptions.models.NetworkException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class IntegrasjonspunktInnsynskravClient {

  private static final String UPLOAD_PATH = "/api/messages/out/multipart";
  private static final String MESSAGE_TYPE = "innsynskrav";
  private static final String MESSAGE_TYPE_VERSION = "1.0";
  private static final String SBD_STANDARD = "urn:no:difi:einnsyn:xsd::innsynskrav";
  private static final String ISO6523_AUTHORITY = "iso6523-actorid-upis";
  private static final String ISO6523_PREFIX = "0192:";
  private static final String PROFILE_IDENTIFIER = "urn:no:difi:profile:einnsyn:innsynskrav:ver1.0";
  private static final String CONVERSATION_ID_SCOPE_TYPE = "ConversationId";
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  private final RestTemplate restTemplate;
  private final Gson gson;
  private final String applicationName;
  private final String moveUrl;
  private final int expectedResponseTimeoutDays;
  private final String integrasjonspunktOrgnummer;
  private final String username;
  private final String password;

  public IntegrasjonspunktInnsynskravClient(
      @Qualifier("compact") Gson gson,
      @Value("${spring.application.name:}") String applicationName,
      @Value("${application.integrasjonspunkt.moveUrl}") String moveUrl,
      @Value("${application.integrasjonspunkt.expectedResponseTimeoutDays:30}")
          int expectedResponseTimeoutDays,
      @Value("${application.integrasjonspunkt.username:}") String username,
      @Value("${application.integrasjonspunkt.password:}") String password,
      @Value("${application.integrasjonspunkt.orgnummer:000000000}")
          String integrasjonspunktOrgnummer) {
    var requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(5));
    requestFactory.setReadTimeout(Duration.ofSeconds(30));
    this.restTemplate = new RestTemplate(requestFactory);
    this.gson = gson;
    this.applicationName = applicationName;
    this.moveUrl = moveUrl.replaceFirst("/+$", "");
    this.expectedResponseTimeoutDays = expectedResponseTimeoutDays;
    this.username = username;
    this.password = password;
    this.integrasjonspunktOrgnummer = integrasjonspunktOrgnummer;
  }

  public String sendInnsynskrav(
      String orderXml,
      String handteresAvOrgnummer,
      String dataOwnerOrgnummer,
      String email,
      String emailText)
      throws NetworkException {
    var messageId = UUID.randomUUID().toString();
    var transactionId = UUID.randomUUID().toString();
    var body = new LinkedMultiValueMap<String, Object>();
    try {
      var expectedResponseDateTime =
          DATE_TIME_FORMATTER.format(OffsetDateTime.now().plusDays(expectedResponseTimeoutDays));
      body.add(
          "sbd",
          createSbdJson(
              messageId,
              transactionId,
              handteresAvOrgnummer,
              dataOwnerOrgnummer,
              email,
              expectedResponseDateTime));
    } catch (RuntimeException e) {
      throw new NetworkException("Could not serialize innsynskrav request", e, moveUrl);
    }
    body.add("order.xml", new NamedByteArrayResource(orderXml, "order.xml"));
    body.add("emailtext", new NamedByteArrayResource(emailText, "emailtext"));

    var headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    // Set user agent
    if (applicationName != null) {
      headers.set(HttpHeaders.USER_AGENT, applicationName);
    }

    // Set basic auth if configured
    if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
      headers.setBasicAuth(username, password, StandardCharsets.UTF_8);
    }

    try {
      var uploadUrl = moveUrl + UPLOAD_PATH;
      restTemplate.postForEntity(uploadUrl, new HttpEntity<>(body, headers), String.class);
    } catch (RestClientException e) {
      throw new NetworkException("Could not send innsynskrav to integrasjonspunkt", e, moveUrl);
    }

    return transactionId;
  }

  private String createSbdJson(
      String messageId,
      String transactionId,
      String handteresAvOrgnummer,
      String dataOwnerOrgnummer,
      String email,
      String expectedResponseDateTime) {
    var sbd = new JsonObject();
    var header = new JsonObject();
    sbd.add("standardBusinessDocumentHeader", header);

    var businessScope = new JsonObject();
    var scopeArray = new JsonArray();
    var scope = new JsonObject();
    var scopeInformationArray = new JsonArray();
    var scopeInformation = new JsonObject();
    scopeInformation.addProperty("expectedResponseDateTime", expectedResponseDateTime);
    scopeInformationArray.add(scopeInformation);
    scope.add("scopeInformation", scopeInformationArray);
    scope.addProperty("identifier", PROFILE_IDENTIFIER);
    scope.addProperty("instanceIdentifier", transactionId);
    scope.addProperty("type", CONVERSATION_ID_SCOPE_TYPE);
    scopeArray.add(scope);
    businessScope.add("scope", scopeArray);
    header.add("businessScope", businessScope);

    var documentIdentification = new JsonObject();
    documentIdentification.addProperty("instanceIdentifier", messageId);
    documentIdentification.addProperty("standard", SBD_STANDARD);
    documentIdentification.addProperty("type", MESSAGE_TYPE);
    documentIdentification.addProperty("typeVersion", MESSAGE_TYPE_VERSION);
    header.add("documentIdentification", documentIdentification);

    header.addProperty("headerVersion", MESSAGE_TYPE_VERSION);

    var receiverArray = new JsonArray();
    receiverArray.add(createSbdhPartner(handteresAvOrgnummer));
    header.add("receiver", receiverArray);

    var senderArray = new JsonArray();
    senderArray.add(createSbdhPartner(integrasjonspunktOrgnummer));
    header.add("sender", senderArray);

    var innsynskrav = new JsonObject();
    innsynskrav.addProperty("orgnr", dataOwnerOrgnummer);
    innsynskrav.addProperty("epost", email);
    sbd.add("innsynskrav", innsynskrav);

    return gson.toJson(sbd);
  }

  private JsonObject createSbdhPartner(String orgnummer) {
    var partner = new JsonObject();
    var identifier = new JsonObject();
    identifier.addProperty("authority", ISO6523_AUTHORITY);
    identifier.addProperty("value", ISO6523_PREFIX + orgnummer);
    partner.add("identifier", identifier);
    return partner;
  }

  private static class NamedByteArrayResource extends ByteArrayResource {

    private final String filename;

    NamedByteArrayResource(String content, String filename) {
      super(content.getBytes(StandardCharsets.UTF_8));
      this.filename = filename;
    }

    @Override
    public String getFilename() {
      return filename;
    }
  }
}
