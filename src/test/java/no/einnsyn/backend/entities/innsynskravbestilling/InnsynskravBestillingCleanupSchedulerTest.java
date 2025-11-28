package no.einnsyn.backend.entities.innsynskravbestilling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.temporal.ChronoUnit;
import no.einnsyn.backend.EinnsynLegacyElasticTestBase;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.tasks.TaskTestService;
import no.einnsyn.backend.tasks.handlers.innsynskrav.InnsynskravScheduler;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.request.RequestContextHolder;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InnsynskravBestillingCleanupSchedulerTest extends EinnsynLegacyElasticTestBase {
  @Autowired private InnsynskravBestillingTestService innsynskravTestService;
  @Autowired private InnsynskravScheduler innsynskravScheduler;
  @Autowired private TaskTestService taskTestService;

  @Value("${application.innsynskrav.anonymousMaxAge}")
  private Integer anonymousMaxAgeDays;

  @Test
  void schedulerShouldCleanupOldInnsynskravBestillings() throws Exception {
    // Create arkiv / arkivdel
    var arkivResponse = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(arkivResponse.getBody(), ArkivDTO.class);
    var arkivdelResponse = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdelDTO = gson.fromJson(arkivdelResponse.getBody(), ArkivdelDTO.class);

    // Insert Saksmappe
    var saksmappeJSON = getSaksmappeJSON();
    var saksmappeResponse = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappeDTO = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappeDTO);

    // Insert Journalpost to Saksmappe
    var jp = getJournalpostJSON();
    var journalpostResponse = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", jp);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var journalpostDTO = gson.fromJson(journalpostResponse.getBody(), JournalpostDTO.class);
    assertNotNull(journalpostDTO);

    // Create InnsynskravBestilling as guest
    var bestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", journalpostDTO.getId());
    bestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));

    var bestillingResponse = post("/innsynskravBestilling", bestillingJSON);
    assertEquals(HttpStatus.CREATED, bestillingResponse.getStatusCode());
    var bestillingDTO = gson.fromJson(bestillingResponse.getBody(), InnsynskravBestillingDTO.class);
    assertNotNull(bestillingDTO);
    var bestillingId = bestillingDTO.getId();

    // Verify the InnsynskravBestilling
    var verificationSecret = innsynskravTestService.getVerificationSecret(bestillingId);
    bestillingResponse =
        patch("/innsynskravBestilling/" + bestillingId + "/verify/" + verificationSecret, null);
    assertEquals(HttpStatus.OK, bestillingResponse.getStatusCode());
    bestillingDTO = gson.fromJson(bestillingResponse.getBody(), InnsynskravBestillingDTO.class);
    assertNotNull(bestillingDTO);
    assertEquals(true, bestillingDTO.getVerified());

    // Set the created date back in time
    taskTestService.modifyInnsynskravBestillingCreatedDate(
        bestillingId, -anonymousMaxAgeDays, ChronoUnit.DAYS);

    // // Run cleanup in a separate thread without request context, like a scheduled task
    var thread =
        new Thread(
            () -> {
              // Clear contexts to simulate scheduled task environment
              RequestContextHolder.resetRequestAttributes();
              SecurityContextHolder.clearContext();

              // Run the scheduler method
              innsynskravScheduler.deleteOldInnsynskravBestilling();
            });
    thread.start();
    thread.join(5000);
    awaitSideEffects();

    // Verify the record was deleted
    var deletedResponse = getAdmin("/innsynskravBestilling/" + bestillingId);
    assertEquals(HttpStatus.NOT_FOUND, deletedResponse.getStatusCode());

    // Cleanup - delete orphaned innsynskrav
    deleteInnsynskravFromBestilling(bestillingDTO);

    // Cleanup - delete saksmappe
    assertEquals(HttpStatus.OK, deleteAdmin("/saksmappe/" + saksmappeDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, deleteAdmin("/arkivdel/" + arkivdelDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, deleteAdmin("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }
}
