package no.einnsyn.backend.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Lazy
public class TaskTestService {

  @LocalServerPort private int port;
  @Autowired private RestTemplate restTemplate;

  public void notifyLagretSak() {
    var url = "http://localhost:" + port + "/lagretSakTest/notifyLagretSak";
    var request = new HttpEntity<>("");
    restTemplate.exchange(url, HttpMethod.POST, request, String.class);
  }

  public void notifyLagretSoek() {
    var url = "http://localhost:" + port + "/lagretSakTest/notifyLagretSoek";
    var request = new HttpEntity<>("");
    restTemplate.exchange(url, HttpMethod.POST, request, String.class);
  }

  public void updateOutdatedDocuments() {
    var url = "http://localhost:" + port + "/updateOutdatedDocuments";
    var request = new HttpEntity<>("");
    restTemplate.exchange(url, HttpMethod.POST, request, String.class);
  }

  public void removeStaleDocuments() {
    var url = "http://localhost:" + port + "/removeStaleDocuments";
    var request = new HttpEntity<>("");
    restTemplate.exchange(url, HttpMethod.POST, request, String.class);
  }
}
