package no.einnsyn.backend.entities.innsynskrav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.reflect.TypeToken;
import no.einnsyn.backend.EinnsynLegacyElasticTestBase;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.innsynskrav.models.InnsynskravES;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
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
class InnsynskravESTest extends EinnsynLegacyElasticTestBase {

  ArkivDTO arkivDTO;
  ArkivdelDTO arkivdelDTO;

  @BeforeAll
  void setUp() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
  }

  @AfterAll
  void tearDown() throws Exception {
    var response = delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testIndexInnsynskrav() throws Exception {
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("journalpost", new JSONArray().put(getJournalpostJSON()));
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    response = get("/saksmappe/" + saksmappeDTO.getId() + "/journalpost");
    var resultListType = new TypeToken<PaginatedList<JournalpostDTO>>() {}.getType();
    PaginatedList<JournalpostDTO> journalpostList =
        gson.fromJson(response.getBody(), resultListType);

    // Create InnsynskravBestilling
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", journalpostList.getItems().getFirst().getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));
    response = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    var innsynskravDTO = innsynskravBestillingDTO.getInnsynskrav().getFirst().getExpandedObject();

    // Should have indexed one Saksmappe one Journalpost and one InnysnkravDel
    var capturedDocuments = captureIndexedDocuments(3);
    resetEs();
    assertNotNull(capturedDocuments.get(saksmappeDTO.getId()));
    assertNotNull(capturedDocuments.get(journalpostList.getItems().getFirst().getId()));
    assertNotNull(capturedDocuments.get(innsynskravDTO.getId()));

    var innsynskravES = (InnsynskravES) capturedDocuments.get(innsynskravDTO.getId());
    compareInnsynskrav(innsynskravDTO, innsynskravBestillingDTO, innsynskravES);

    // Delete
    delete("/saksmappe/" + saksmappeDTO.getId());
    deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingDTO.getId());
    deleteInnsynskravFromBestilling(innsynskravBestillingDTO);
  }
}
