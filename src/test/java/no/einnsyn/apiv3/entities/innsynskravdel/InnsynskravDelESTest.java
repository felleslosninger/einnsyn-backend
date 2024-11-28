package no.einnsyn.apiv3.entities.innsynskravdel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import no.einnsyn.apiv3.EinnsynLegacyElasticTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelES;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class InnsynskravDelESTest extends EinnsynLegacyElasticTestBase {

  ArkivDTO arkivDTO;

  @BeforeAll
  void setUp() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
  }

  @AfterAll
  void tearDown() throws Exception {
    var response = delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testIndexInnsynskravDel() throws Exception {
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("journalpost", new JSONArray().put(getJournalpostJSON()));
    var response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Create InnsynskravBestilling
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", saksmappeDTO.getJournalpost().getFirst().getId());
    innsynskravBestillingJSON.put("innsynskravDel", new JSONArray().put(innsynskravDelJSON));
    response = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    var innsynskravDelDTO =
        innsynskravBestillingDTO.getInnsynskravDel().getFirst().getExpandedObject();

    // Should have indexed one Saksmappe one Journalpost and one InnysnkravDel
    var capturedDocuments = captureIndexedDocuments(3);
    resetEsMock();
    assertNotNull(capturedDocuments.get(saksmappeDTO.getId()));
    assertNotNull(capturedDocuments.get(saksmappeDTO.getJournalpost().getFirst().getId()));
    assertNotNull(capturedDocuments.get(innsynskravDelDTO.getId()));

    var innsynskravDelES = (InnsynskravDelES) capturedDocuments.get(innsynskravDelDTO.getId());
    compareInnsynskravDel(innsynskravDelDTO, innsynskravBestillingDTO, innsynskravDelES);

    // Delete
    delete("/saksmappe/" + saksmappeDTO.getId());
    deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingDTO.getId());
  }
}
