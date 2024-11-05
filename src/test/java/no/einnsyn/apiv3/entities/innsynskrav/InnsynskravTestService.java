package no.einnsyn.apiv3.entities.innsynskrav;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
@Lazy
public class InnsynskravTestService {

  @LocalServerPort private int port;
  @Autowired private RestTemplate restTemplate;
  @Autowired private InnsynskravDelRepository innsynskravDelRepository;

  public void triggerScheduler() {
    var url = "http://localhost:" + port + "/innsynskravTest/trigger";
    var request = new HttpEntity<>("");
    restTemplate.exchange(url, HttpMethod.POST, request, String.class);
  }

  public String getVerificationSecret(String id) {
    var url = "http://localhost:" + port + "/innsynskravTest/getVerificationSecret/" + id;
    var request = new HttpEntity<>("");
    var response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
    return response.getBody();
  }

  public void assertSent(String id) throws Exception {
    assertSent(id, 0);
  }

  @SuppressWarnings("unchecked")
  @Transactional(readOnly = true)
  public void assertSent(String id, Integer delNo) throws Exception {
    var url = "http://localhost:" + port + "/innsynskravTest/isSent/" + id + "/" + delNo;
    var request = new HttpEntity<>("");
    var response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
    assertNotNull(response.getBody(), "Sent timestamp was null");

    url = "http://localhost:" + port + "/innsynskravTest/delLegacyStatus/" + id + "/" + delNo;
    request = new HttpEntity<>("");
    List<String> statuses =
        restTemplate.exchange(url, HttpMethod.GET, request, List.class).getBody();
    assertNotNull(statuses, "Legacy status was null");
    assertTrue(statuses.contains("OPPRETTET"), "Legacy status was not OPPRETTET");
    assertTrue(statuses.contains("SENDT_TIL_VIRKSOMHET"), "Legacy status was not SENDT");
  }

  public void assertNotSent(String id) throws Exception {
    assertNotSent(id, 0);
  }

  @SuppressWarnings("unchecked")
  public void assertNotSent(String id, Integer delNo) throws Exception {
    var url = "http://localhost:" + port + "/innsynskravTest/isSent/" + id + "/" + delNo;
    var request = new HttpEntity<>("");
    var response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
    assertNull(response.getBody(), "Sent timestamp should be null, was " + response.getBody());

    url = "http://localhost:" + port + "/innsynskravTest/delLegacyStatus/" + id + "/" + delNo;
    request = new HttpEntity<>("");
    List<String> statuses =
        restTemplate.exchange(url, HttpMethod.GET, request, List.class).getBody();
    assertNotNull(statuses, "Legacy status was null");
    assertTrue(statuses.contains("OPPRETTET"), "Legacy status was not OPPRETTET");
    assertFalse(statuses.contains("SENDT_TIL_VIRKSOMHET"), "Legacy status was not SENDT");
  }
}
