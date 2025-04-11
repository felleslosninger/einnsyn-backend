package no.einnsyn.backend.entities.dokumentbeskrivelse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.dokumentobjekt.models.DokumentobjektDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class DokumentbeskrivelseControllerTest extends EinnsynControllerTestBase {

  ArkivDTO arkivDTO;
  SaksmappeDTO saksmappeDTO;
  JournalpostDTO journalpostDTO;

  @BeforeEach
  void setUp() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkivDTO.getId());

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdelDTO.getId());

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappeDTO.getId());

    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", getJournalpostJSON());
    journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertNotNull(journalpostDTO.getId());
  }

  @AfterEach
  void tearDown() throws Exception {
    assertEquals(HttpStatus.OK, delete("/saksmappe/" + saksmappeDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/saksmappe/" + saksmappeDTO.getId()).getStatusCode());

    // Delete arkiv
    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }

  @Test
  void testDokumentbeskrivelseLifecycle() throws Exception {
    var dokumentbeskrivelseJSON = getDokumentbeskrivelseJSON();
    var response =
        post(
            "/journalpost/" + journalpostDTO.getId() + "/dokumentbeskrivelse",
            dokumentbeskrivelseJSON);
    var dokumentbeskrivelseDTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    assertNotNull(dokumentbeskrivelseDTO.getId());
    assertEquals(
        dokumentbeskrivelseJSON.getString("tilknyttetRegistreringSom"),
        dokumentbeskrivelseDTO.getTilknyttetRegistreringSom());
    assertEquals(dokumentbeskrivelseJSON.getString("tittel"), dokumentbeskrivelseDTO.getTittel());
    assertEquals(
        dokumentbeskrivelseJSON.getString("tittelSensitiv"),
        dokumentbeskrivelseDTO.getTittelSensitiv());
    assertEquals(1, dokumentbeskrivelseDTO.getDokumentnummer());

    // Update dokumentbeskrivelse
    dokumentbeskrivelseJSON.put("tilknyttetRegistreringSom", "vedlegg");
    dokumentbeskrivelseJSON.put("tittel", "updatedTitle");
    dokumentbeskrivelseJSON.put("tittelSensitiv", "updatedSensitiveTitle");
    dokumentbeskrivelseJSON.put("dokumentnummer", "2");
    response =
        patch("/dokumentbeskrivelse/" + dokumentbeskrivelseDTO.getId(), dokumentbeskrivelseJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    dokumentbeskrivelseDTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    assertEquals("vedlegg", dokumentbeskrivelseDTO.getTilknyttetRegistreringSom());
    assertEquals("updatedTitle", dokumentbeskrivelseDTO.getTittel());
    assertEquals("updatedSensitiveTitle", dokumentbeskrivelseDTO.getTittelSensitiv());
    assertEquals(2, dokumentbeskrivelseDTO.getDokumentnummer());

    // Add Dokumentobjekt
    var dokumentobjektJSON = getDokumentobjektJSON();
    dokumentbeskrivelseJSON.put("dokumentobjekt", new JSONArray(List.of(dokumentobjektJSON)));
    response =
        patch("/dokumentbeskrivelse/" + dokumentbeskrivelseDTO.getId(), dokumentbeskrivelseJSON);
    dokumentbeskrivelseDTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    assertEquals(1, dokumentbeskrivelseDTO.getDokumentobjekt().size());
    var dokumentobjektDTO = dokumentbeskrivelseDTO.getDokumentobjekt().get(0);
    assertNotNull(dokumentobjektDTO.getId());

    // Check that Dokumentbeskrivelse has one Dokumentobjekt
    response = get("/dokumentbeskrivelse/" + dokumentbeskrivelseDTO.getId());
    dokumentbeskrivelseDTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    assertEquals(1, dokumentbeskrivelseDTO.getDokumentobjekt().size());
    assertEquals(
        dokumentobjektDTO.getId(), dokumentbeskrivelseDTO.getDokumentobjekt().get(0).getId());

    // Delete
    response = delete("/dokumentbeskrivelse/" + dokumentbeskrivelseDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  // Test moving an existing Dokumentobjekt to another Dokumentbeskrivelse
  @Test
  void testMoveDokumentobjekt() throws Exception {
    var dokumentbeskrivelseJSON = getDokumentbeskrivelseJSON();
    var dokumentobjektJSON = getDokumentobjektJSON();
    dokumentobjektJSON.put("systemId", "123456789");
    dokumentbeskrivelseJSON.put("dokumentobjekt", new JSONArray(List.of(dokumentobjektJSON)));
    var response =
        post(
            "/journalpost/" + journalpostDTO.getId() + "/dokumentbeskrivelse",
            dokumentbeskrivelseJSON);
    var dokumentbeskrivelseDTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    assertNotNull(dokumentbeskrivelseDTO.getId());
    var dokumentobjektDTO = dokumentbeskrivelseDTO.getDokumentobjekt().get(0).getExpandedObject();
    assertNotNull(dokumentobjektDTO.getId());

    // Create new Dokumentbeskrivelse with the same dokumentobjekt
    var newDokumentbeskrivelseJSON = getDokumentbeskrivelseJSON();
    newDokumentbeskrivelseJSON.put("dokumentobjekt", new JSONArray(List.of(dokumentobjektJSON)));
    response =
        post(
            "/journalpost/" + journalpostDTO.getId() + "/dokumentbeskrivelse",
            newDokumentbeskrivelseJSON);
    var newDokumentbeskrivelseDTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    assertNotNull(newDokumentbeskrivelseDTO.getId());
    var newDokumentobjektDTO =
        newDokumentbeskrivelseDTO.getDokumentobjekt().get(0).getExpandedObject();
    assertNotNull(newDokumentobjektDTO.getId());
    assertEquals(dokumentobjektDTO.getId(), newDokumentobjektDTO.getId());

    // Check that the Dokumentobjekt is moved
    response = get("/dokumentobjekt/" + dokumentobjektDTO.getId());
    dokumentobjektDTO = gson.fromJson(response.getBody(), DokumentobjektDTO.class);
    assertEquals(
        newDokumentbeskrivelseDTO.getId(), dokumentobjektDTO.getDokumentbeskrivelse().getId());

    // Delete
    response = delete("/dokumentbeskrivelse/" + newDokumentbeskrivelseDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}
