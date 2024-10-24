package no.einnsyn.apiv3.entities.innsynskrav;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Lazy
public class InnsynskravTestService {

  @LocalServerPort private int port;
  @Autowired private RestTemplate restTemplate;

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

  public void assertSent(String id, Integer delNo) throws Exception {
    var url = "http://localhost:" + port + "/innsynskravTest/isSent/" + id + "/" + delNo;
    var request = new HttpEntity<>("");
    var response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
    assertTrue(response.getBody() != null, "Sent timestamp was null");
  }

  public void assertNotSent(String id) throws Exception {
    assertNotSent(id, 0);
  }

  public void assertNotSent(String id, Integer delNo) throws Exception {
    var url = "http://localhost:" + port + "/innsynskravTest/isSent/" + id + "/" + delNo;
    var request = new HttpEntity<>("");
    var response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
    assertTrue(
        response.getBody() == null, "Sent timestamp should be null, was " + response.getBody());
  }
}
