package no.einnsyn.backend.entities.innsynskravbestilling;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import no.einnsyn.backend.common.exceptions.models.NetworkException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

  public IntegrasjonspunktInnsynskravClient(
      @Value("${spring.application.name:}") String applicationName,
      @Value("${application.integrasjonspunkt.moveUrl}") String moveUrl,
      @Value("${application.integrasjonspunkt.expectedResponseTimeoutDays:30}")
          int expectedResponseTimeoutDays,
      @Value("${application.integrasjonspunkt.orgnummer:000000000}")
          String integrasjonspunktOrgnummer) {
    this.restTemplate = new RestTemplate();
    this.gson = new GsonBuilder().serializeNulls().create();
    this.applicationName = applicationName;
    this.moveUrl = moveUrl.replaceFirst("/+$", "");
    this.expectedResponseTimeoutDays = expectedResponseTimeoutDays;
    this.integrasjonspunktOrgnummer = integrasjonspunktOrgnummer;
  }

  public String sendInnsynskrav(
      String orderXml,
      String handteresAvOrgnummer,
      String dataOwnerOrgnummer,
      String email,
      String emailText)
      throws NetworkException {
    var now = OffsetDateTime.now();
    var formattedNow = formatDateTime(now);
    var messageId = UUID.randomUUID().toString();
    var transactionId = UUID.randomUUID().toString();
    var sbd =
        new StandardBusinessDocumentEnvelope(
            new StandardBusinessDocumentHeader(
                MESSAGE_TYPE_VERSION,
                List.of(partner(integrasjonspunktOrgnummer)),
                List.of(partner(handteresAvOrgnummer)),
                new DocumentIdentification(
                    SBD_STANDARD,
                    MESSAGE_TYPE_VERSION,
                    messageId,
                    MESSAGE_TYPE,
                    null,
                    formattedNow),
                null,
                new BusinessScope(
                    List.of(
                        new Scope(
                            CONVERSATION_ID_SCOPE_TYPE,
                            transactionId,
                            PROFILE_IDENTIFIER,
                            List.of(
                                new CorrelationInformation(
                                    null,
                                    null,
                                    formatDateTime(now.plusDays(expectedResponseTimeoutDays)))))))),
            new InnsynskravMessage(dataOwnerOrgnummer, email, null, null));

    var body = new LinkedMultiValueMap<String, Object>();
    try {
      body.add("sbd", gson.toJson(sbd));
    } catch (RuntimeException e) {
      throw new NetworkException("Could not serialize innsynskrav request", e, moveUrl);
    }
    body.add("order.xml", new NamedByteArrayResource(orderXml, "order.xml"));
    body.add("emailtext", new NamedByteArrayResource(emailText, "emailtext"));

    var headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    var userAgent = buildUserAgent();
    if (userAgent != null) {
      headers.set(HttpHeaders.USER_AGENT, userAgent);
    }

    try {
      restTemplate.postForEntity(uploadUrl(), new HttpEntity<>(body, headers), String.class);
    } catch (RestClientException e) {
      throw new NetworkException("Could not send innsynskrav to integrasjonspunkt", e, moveUrl);
    }

    return messageId;
  }

  private Partner partner(String orgnummer) {
    return new Partner(
        new PartnerIdentification(ISO6523_PREFIX + orgnummer, ISO6523_AUTHORITY), List.of());
  }

  private String buildUserAgent() {
    if (!StringUtils.hasText(applicationName)) {
      return null;
    }
    return applicationName;
  }

  private String uploadUrl() {
    return moveUrl + UPLOAD_PATH;
  }

  private String formatDateTime(OffsetDateTime dateTime) {
    return DATE_TIME_FORMATTER.format(dateTime);
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

  private record StandardBusinessDocumentEnvelope(
      StandardBusinessDocumentHeader standardBusinessDocumentHeader,
      InnsynskravMessage innsynskrav) {}

  private record StandardBusinessDocumentHeader(
      String headerVersion,
      List<Partner> sender,
      List<Partner> receiver,
      DocumentIdentification documentIdentification,
      Object manifest,
      BusinessScope businessScope) {}

  private record Partner(PartnerIdentification identifier, List<Object> contactInformation) {}

  private record PartnerIdentification(String value, String authority) {}

  private record DocumentIdentification(
      String standard,
      String typeVersion,
      String instanceIdentifier,
      String type,
      Boolean multipleType,
      String creationDateAndTime) {}

  private record BusinessScope(List<Scope> scope) {}

  private record Scope(
      String type,
      String instanceIdentifier,
      String identifier,
      List<CorrelationInformation> scopeInformation) {}

  private record CorrelationInformation(
      String requestingDocumentCreationDateTime,
      String requestingDocumentInstanceIdentifier,
      String expectedResponseDateTime) {}

  private record InnsynskravMessage(
      String orgnr, String epost, Object sikkerhetsnivaa, Object hoveddokument) {}
}
