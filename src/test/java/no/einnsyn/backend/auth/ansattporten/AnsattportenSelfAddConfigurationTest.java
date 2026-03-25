package no.einnsyn.backend.auth.ansattporten;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import no.einnsyn.backend.EInnsynApplication;
import no.einnsyn.backend.EinnsynControllerTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {"application.ansattporten.allowSelfRegistration=false"},
    classes = {EInnsynApplication.class, AnsattportenTestJwtConfiguration.class})
@ActiveProfiles("test")
class AnsattportenSelfAddConfigurationTest extends EinnsynControllerTestBase {

  @Value("${application.ansattporten.issuerUri}")
  private String ansattportenIssuerUri;

  @Test
  void shouldRejectSelfAddWhenDisabled() throws Exception {
    var orgnummer = "623456789";
    var jwt =
        AnsattportenAuthenticationTest.generateMockAltinn3Jwt(orgnummer, ansattportenIssuerUri);
    var enhetJSON = getEnhetJSON();
    enhetJSON.put("orgnummer", orgnummer);
    enhetJSON.put("parent", rootEnhetId);

    var response = post("/enhet", enhetJSON, jwt);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNull(enhetRepository.findByOrgnummer(orgnummer));
  }
}
