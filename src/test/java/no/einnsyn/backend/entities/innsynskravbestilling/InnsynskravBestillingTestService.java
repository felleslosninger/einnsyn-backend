package no.einnsyn.backend.entities.innsynskravbestilling;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import no.einnsyn.backend.testutils.SideEffectService;
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
public class InnsynskravBestillingTestService {

  @LocalServerPort private int port;
  @Autowired private RestTemplate restTemplate;
  @Autowired private SideEffectService sideEffectService;

  public void triggerScheduler() {
    var url = "http://localhost:" + port + "/innsynskravTest/trigger";
    var request = new HttpEntity<>("");
    restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    sideEffectService.awaitSideEffects();
  }

  public String getVerificationSecret(String id) {
    var url = "http://localhost:" + port + "/innsynskravTest/getVerificationSecret/" + id;
    var request = new HttpEntity<>("");
    var response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
    sideEffectService.awaitSideEffects();
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
    sideEffectService.awaitSideEffects();
    assertNotNull(response.getBody(), "Sent timestamp was null");

    url = "http://localhost:" + port + "/innsynskravTest/delLegacyStatus/" + id + "/" + delNo;
    request = new HttpEntity<>("");
    List<String> statuses =
        restTemplate.exchange(url, HttpMethod.GET, request, List.class).getBody();
    sideEffectService.awaitSideEffects();
    assertNotNull(statuses, "Legacy status was null");
    assertTrue(statuses.contains("OPPRETTET"), "Legacy status did not contain OPPRETTET");
    assertTrue(
        statuses.contains("SENDT_TIL_VIRKSOMHET"),
        "Legacy status did not contain SENDT_TIL_VIRKSOMHET");
  }

  public void assertNotSent(String id) throws Exception {
    assertNotSent(id, 0);
  }

  @SuppressWarnings("unchecked")
  public void assertNotSent(String id, Integer delNo) throws Exception {
    var url = "http://localhost:" + port + "/innsynskravTest/isSent/" + id + "/" + delNo;
    var request = new HttpEntity<>("");
    var response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
    sideEffectService.awaitSideEffects();
    assertNull(response.getBody(), "Sent timestamp should be null, was " + response.getBody());

    url = "http://localhost:" + port + "/innsynskravTest/delLegacyStatus/" + id + "/" + delNo;
    request = new HttpEntity<>("");
    List<String> statuses =
        restTemplate.exchange(url, HttpMethod.GET, request, List.class).getBody();
    sideEffectService.awaitSideEffects();
    assertNotNull(statuses, "Legacy status was null");
    assertTrue(statuses.contains("OPPRETTET"), "Legacy status did not contain OPPRETTET");
    assertFalse(
        statuses.contains("SENDT_TIL_VIRKSOMHET"), "Legacy status contained SENDT_TIL_VIRKSOMHET");
  }

  public void assertRetryCount(String id, Integer delNo, Integer expectedRetryCount) {
    var url = "http://localhost:" + port + "/innsynskravTest/retryCount/" + id + "/" + delNo;
    var request = new HttpEntity<>("");
    var response = restTemplate.exchange(url, HttpMethod.GET, request, Integer.class);
    sideEffectService.awaitSideEffects();
    assertEquals(expectedRetryCount, response.getBody());
  }

  @SuppressWarnings("unchecked")
  public void assertRetryCounts(String id, List<Integer> expectedRetryCounts) {
    var url = "http://localhost:" + port + "/innsynskravTest/retryCounts/" + id;
    var request = new HttpEntity<>("");
    List<?> retryCounts =
        restTemplate.exchange(url, HttpMethod.GET, request, List.class).getBody();
    sideEffectService.awaitSideEffects();
    var expectedSorted = new ArrayList<>(expectedRetryCounts);
    var actualSorted =
        retryCounts.stream()
            .map(value -> ((Number) value).intValue())
            .sorted()
            .toList();
    expectedSorted.sort(Comparator.naturalOrder());
    assertEquals(expectedSorted, actualSorted);
  }

  @SuppressWarnings("unchecked")
  public void assertSentStates(String id, List<Boolean> expectedSentStates) {
    var sentStates = getSentStates(id);
    var expectedSorted = new ArrayList<>(expectedSentStates);
    var actualSorted = new ArrayList<>(sentStates);
    expectedSorted.sort(Comparator.naturalOrder());
    actualSorted.sort(Comparator.naturalOrder());
    assertEquals(expectedSorted, actualSorted);
  }

  @SuppressWarnings("unchecked")
  public List<Boolean> getSentStates(String id) {
    var url = "http://localhost:" + port + "/innsynskravTest/sentStates/" + id;
    var request = new HttpEntity<>("");
    List<Boolean> sentStates =
        restTemplate.exchange(url, HttpMethod.GET, request, List.class).getBody();
    sideEffectService.awaitSideEffects();
    return sentStates;
  }
}
