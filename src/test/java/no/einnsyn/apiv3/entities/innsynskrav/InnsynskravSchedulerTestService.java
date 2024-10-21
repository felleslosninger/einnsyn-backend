package no.einnsyn.apiv3.entities.innsynskrav;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InnsynskravSchedulerTestService {

  @Autowired private InnsynskravRepository innsynskravRepository;
  @Autowired private EntityManager entityManager;

  @Transactional
  public String getVerificationSecret(String innsynskravId) {
    var innsynskrav = innsynskravRepository.findById(innsynskravId).orElse(null);
    entityManager.refresh(innsynskrav);
    assertNotNull(innsynskrav, "Innsynskrav not found");
    return innsynskrav.getVerificationSecret();
  }

  @Transactional
  public void assertSent(String innsynskravId) throws Exception {
    var innsynskrav = innsynskravRepository.findById(innsynskravId).orElse(null);
    entityManager.refresh(innsynskrav);
    assertNotNull(innsynskrav, "Innsynskrav not found");
    assertNotNull(innsynskrav.getInnsynskravDel().getFirst().getSent(), "Innsynskrav was not sent");
  }

  @Transactional
  public void assertNotSent(String innsynskravId) throws Exception {
    var innsynskrav = innsynskravRepository.findById(innsynskravId).orElse(null);
    entityManager.refresh(innsynskrav);
    assertNotNull(innsynskrav, "Innsynskrav not found");
    assertNull(innsynskrav.getInnsynskravDel().getFirst().getSent(), "Innsynskrav was sent");
  }
}
