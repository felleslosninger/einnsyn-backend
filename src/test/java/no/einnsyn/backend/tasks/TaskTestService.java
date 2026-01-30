package no.einnsyn.backend.tasks;

import jakarta.transaction.Transactional;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.NoSuchElementException;
import no.einnsyn.backend.entities.innsynskravbestilling.InnsynskravBestillingRepository;
import no.einnsyn.backend.entities.lagretsak.LagretSakRepository;
import no.einnsyn.backend.entities.lagretsoek.LagretSoekRepository;
import no.einnsyn.backend.testutils.SideEffectService;
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
  @Autowired private LagretSakRepository lagretSakRepository;
  @Autowired private LagretSoekRepository lagretSoekRepository;
  @Autowired private InnsynskravBestillingRepository innsynskravBestillingRepository;
  @Autowired private SideEffectService sideEffectService;

  public void notifyLagretSak() {
    var url = "http://localhost:" + port + "/lagretSakTest/notifyLagretSak";
    var request = new HttpEntity<>("");
    restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    sideEffectService.awaitSideEffects();
  }

  public void notifyLagretSoek() {
    var url = "http://localhost:" + port + "/lagretSoekTest/notifyLagretSoek";
    var request = new HttpEntity<>("");
    restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    sideEffectService.awaitSideEffects();
  }

  public void updateOutdatedDocuments() {
    var url = "http://localhost:" + port + "/updateOutdatedDocuments";
    var request = new HttpEntity<>("");
    restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    sideEffectService.awaitSideEffects();
  }

  public void removeStaleDocuments() {
    var url = "http://localhost:" + port + "/removeStaleDocuments";
    var request = new HttpEntity<>("");
    restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    sideEffectService.awaitSideEffects();
  }

  @Transactional
  public int getLagretSakHitCount(String id) throws NoSuchElementException {
    var lagretSak = lagretSakRepository.findById(id).orElseThrow();
    return lagretSak.getHitCount();
  }

  @Transactional
  public int getLagretSoekHitCount(String id) throws NoSuchElementException {
    var lagretSoek = lagretSoekRepository.findById(id).orElseThrow();
    return lagretSoek.getHitCount();
  }

  @Transactional
  public List<String> getLagretSoekHitIds(String id) throws NoSuchElementException {
    var lagretSoek = lagretSoekRepository.findById(id).orElseThrow();
    return lagretSoek.getHitList().stream().map(hit -> hit.getId()).toList();
  }

  @Transactional
  public void modifyInnsynskravBestillingCreatedDate(
      String ikbId, int change, TemporalUnit timeUnit) throws NoSuchElementException {
    var innsynskravBestilling = innsynskravBestillingRepository.findById(ikbId).orElseThrow();
    innsynskravBestilling.setCreated(innsynskravBestilling.getCreated().plus(change, timeUnit));
    innsynskravBestillingRepository.save(innsynskravBestilling);
  }

  public void cleanOldInnsynskravBestillings() {
    var url = "http://localhost:" + port + "/cleanOldInnsynskravBestillings";
    var request = new HttpEntity<>("");
    restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    sideEffectService.awaitSideEffects();
  }
}
