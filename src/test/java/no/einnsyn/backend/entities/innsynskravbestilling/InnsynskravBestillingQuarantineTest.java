package no.einnsyn.backend.entities.innsynskravbestilling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {"application.innsynskrav.verificationQuarantineLimit=1"})
class InnsynskravBestillingQuarantineTest extends EinnsynControllerTestBase {

  @Lazy @Autowired private InnsynskravBestillingTestService innsynskravTestService;

  @Test
  void testVerificationQuarantine() throws Exception {
    // Insert Saksmappe
    var arkivResponse = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(arkivResponse.getBody(), ArkivDTO.class);
    var arkivdelResponse = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdelDTO = gson.fromJson(arkivdelResponse.getBody(), ArkivdelDTO.class);
    var saksmappeResponse =
        post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappeDTO = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);

    // Insert Journalpost to Saksmappe
    var jp = getJournalpostJSON();
    var journalpostResponse = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", jp);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var journalpostDTO = gson.fromJson(journalpostResponse.getBody(), JournalpostDTO.class);

    var testEmail = "quarantine-test@example.com";

    // Create first InnsynskravBestilling (should succeed, limit is 1)
    var innsynskravBestilling1JSON = getInnsynskravBestillingJSON();
    innsynskravBestilling1JSON.put("email", testEmail);
    var innsynskrav1JSON = getInnsynskravJSON();
    innsynskrav1JSON.put("journalpost", journalpostDTO.getId());
    innsynskravBestilling1JSON.put("innsynskrav", new JSONArray().put(innsynskrav1JSON));

    var response1 = post("/innsynskravBestilling", innsynskravBestilling1JSON);
    assertEquals(HttpStatus.CREATED, response1.getStatusCode());
    var innsynskravBestilling1DTO =
        gson.fromJson(response1.getBody(), InnsynskravBestillingDTO.class);
    assertFalse(innsynskravBestilling1DTO.getVerified());

    // Create second InnsynskravBestilling (should fail with TOO_MANY_REQUESTS, limit is 1)
    var innsynskravBestilling2JSON = getInnsynskravBestillingJSON();
    innsynskravBestilling2JSON.put("email", testEmail);
    var innsynskrav2JSON = getInnsynskravJSON();
    innsynskrav2JSON.put("journalpost", journalpostDTO.getId());
    innsynskravBestilling2JSON.put("innsynskrav", new JSONArray().put(innsynskrav2JSON));

    var response2 = post("/innsynskravBestilling", innsynskravBestilling2JSON);
    assertEquals(HttpStatus.TOO_MANY_REQUESTS, response2.getStatusCode());

    // Verify the first InnsynskravBestilling
    var verificationSecret1 =
        innsynskravTestService.getVerificationSecret(innsynskravBestilling1DTO.getId());
    var verifyResponse =
        patch(
            "/innsynskravBestilling/"
                + innsynskravBestilling1DTO.getId()
                + "/verify/"
                + verificationSecret1,
            null);
    assertEquals(HttpStatus.OK, verifyResponse.getStatusCode());
    innsynskravBestilling1DTO =
        gson.fromJson(verifyResponse.getBody(), InnsynskravBestillingDTO.class);
    assertTrue(innsynskravBestilling1DTO.getVerified());

    // Now creating a new order should succeed again (no unverified orders remaining)
    var innsynskravBestilling3JSON = getInnsynskravBestillingJSON();
    innsynskravBestilling3JSON.put("email", testEmail);
    var innsynskrav3JSON = getInnsynskravJSON();
    innsynskrav3JSON.put("journalpost", journalpostDTO.getId());
    innsynskravBestilling3JSON.put("innsynskrav", new JSONArray().put(innsynskrav3JSON));

    var response3 = post("/innsynskravBestilling", innsynskravBestilling3JSON);
    assertEquals(HttpStatus.CREATED, response3.getStatusCode());
    var innsynskravBestilling3DTO =
        gson.fromJson(response3.getBody(), InnsynskravBestillingDTO.class);
    assertFalse(innsynskravBestilling3DTO.getVerified());

    // Cleanup
    var deleteResponse1 =
        deleteAdmin("/innsynskravBestilling/" + innsynskravBestilling1DTO.getId());
    assertEquals(HttpStatus.OK, deleteResponse1.getStatusCode());
    deleteInnsynskravFromBestilling(
        gson.fromJson(deleteResponse1.getBody(), InnsynskravBestillingDTO.class));

    var deleteResponse3 =
        deleteAdmin("/innsynskravBestilling/" + innsynskravBestilling3DTO.getId());
    assertEquals(HttpStatus.OK, deleteResponse3.getStatusCode());
    deleteInnsynskravFromBestilling(
        gson.fromJson(deleteResponse3.getBody(), InnsynskravBestillingDTO.class));

    assertEquals(HttpStatus.OK, delete("/saksmappe/" + saksmappeDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/arkivdel/" + arkivdelDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }
}
