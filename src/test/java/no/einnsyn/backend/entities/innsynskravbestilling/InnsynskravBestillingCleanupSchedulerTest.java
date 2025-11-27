package no.einnsyn.backend.entities.innsynskravbestilling;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import no.einnsyn.backend.EinnsynLegacyElasticTestBase;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.tasks.TaskTestService;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
      "application.innsynskrav.cleanSchedule=* * * * * *",
      "application.innsynskrav.anonymousMaxAge=1"
    })
@EnableAutoConfiguration
@ActiveProfiles("test")
@Import(LocalSchedulingConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class InnsynskravBestillingCleanupSchedulerTest extends EinnsynLegacyElasticTestBase {

  @Autowired private InnsynskravBestillingTestService innsynskravTestService;
  @Autowired private TaskTestService taskTestService;

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

    // Set the created date back in time (yesterday)
    taskTestService.modifyInnsynskravBestillingCreatedDate(bestillingId, -1, ChronoUnit.DAYS);

    // Wait for cron job to run and clean up old bestillings, and verify that it has been deleted
    await()
        .atMost(60, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              var deletedBestillingResponse = getAdmin("/innsynskravBestilling/" + bestillingId);
              assertEquals(HttpStatus.NOT_FOUND, deletedBestillingResponse.getStatusCode());
            });

    // Cleanup - delete orphaned innsynskrav
    deleteInnsynskravFromBestilling(bestillingDTO);

    // Cleanup - delete saksmappe
    assertEquals(HttpStatus.OK, deleteAdmin("/saksmappe/" + saksmappeDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, deleteAdmin("/arkivdel/" + arkivdelDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, deleteAdmin("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }
}
