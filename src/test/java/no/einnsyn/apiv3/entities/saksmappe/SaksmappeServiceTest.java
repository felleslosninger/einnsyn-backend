package no.einnsyn.apiv3.entities.saksmappe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.EinnsynServiceTestBase;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SaksmappeServiceTest extends EinnsynServiceTestBase {

  @Autowired private SaksmappeService saksmappeService;

  /** Add a new saksmappe */
  @Test
  void addNewSaksmappe() throws Exception {
    var saksmappeDTO = getSaksmappeDTO();
    System.err.println("....");

    // Insert the saksmappe, and verify returned content
    var insertedSaksmappe = saksmappeService.update(null, saksmappeDTO);
    assertNotNull(insertedSaksmappe.getId());
    assertEquals(insertedSaksmappe.getOffentligTittel(), saksmappeDTO.getOffentligTittel());
    assertEquals(
        insertedSaksmappe.getOffentligTittelSensitiv(), saksmappeDTO.getOffentligTittelSensitiv());
    assertEquals(insertedSaksmappe.getBeskrivelse(), saksmappeDTO.getBeskrivelse());
    assertEquals(insertedSaksmappe.getSaksaar(), saksmappeDTO.getSaksaar());
    System.err.println("Fooo");

    // Verify that journalenhet was inserted
    var journalenhetField = insertedSaksmappe.getJournalenhet();
    assertNotNull(journalenhetField, "Inserted saksmappe should have a journalenhet field");
    assertNotNull(journalenhetField.getId(), "Inserted saksmappe should have a journalenhet ID");

    // Verify that the saksmappe can be found in the database
    var saksmappe = saksmappeRepository.findById(insertedSaksmappe.getId()).orElse(null);
    assertNotNull(saksmappe);

    // Delete the saksmappe
    var deletedSaksmappe = saksmappeService.delete(insertedSaksmappe.getId());
    assertTrue(deletedSaksmappe.getDeleted());
    assertNull(saksmappeRepository.findById(insertedSaksmappe.getId()).orElse(null));
  }

  /**
   * Add a new saksmappe with administrativEnhet set. Make sure administrativEnhetObjekt is found in
   * the tree below journalenhet.
   */
  @Test
  void addNewSaksmappeWithAdministrativEnhet() throws Exception {
    var saksmappeDTOWithAdmEnhet = getSaksmappeDTO();
    saksmappeDTOWithAdmEnhet.setAdministrativEnhet("UNDER");
    var insertedSaksmappeWithAdmEnhet = saksmappeService.update(null, saksmappeDTOWithAdmEnhet);
    assertNotNull(insertedSaksmappeWithAdmEnhet.getId());
    assertEquals("UNDER", insertedSaksmappeWithAdmEnhet.getAdministrativEnhet());

    var saksmappeDTOWithoutAdmEnhet = getSaksmappeDTO();
    var insertedSaksmappeWithoutAdmEnhet =
        saksmappeService.update(null, saksmappeDTOWithoutAdmEnhet);
    assertNotNull(insertedSaksmappeWithoutAdmEnhet.getId());

    assertNotEquals(
        insertedSaksmappeWithAdmEnhet.getAdministrativEnhetObjekt().getId(),
        insertedSaksmappeWithoutAdmEnhet.getAdministrativEnhetObjekt().getId());

    // Delete the saksmappe
    var deletedSaksmappeWithAdmEnhet =
        saksmappeService.delete(insertedSaksmappeWithAdmEnhet.getId());
    assertTrue(deletedSaksmappeWithAdmEnhet.getDeleted());
  }

  /** Add a new saksmappe with a journalpost */
  @Test
  void addNewSaksmappeWithJournalpost() throws Exception {
    var saksmappeDTO = getSaksmappeDTO();
    var journalpostJSON = getJournalpostDTO();
    var journalpostField = new ExpandableField<>(journalpostJSON);
    saksmappeDTO.setJournalpost(List.of(journalpostField));

    // Insert saksmappe with journalpost
    var insertedSaksmappeDTO = saksmappeService.update(null, saksmappeDTO);
    assertNotNull(insertedSaksmappeDTO.getId());

    // Verify that there is one journalpost in the returned saksmappe
    var insertedJournalpostFieldList = insertedSaksmappeDTO.getJournalpost();
    assertEquals(1, insertedJournalpostFieldList.size());
    var insertedJournalpostField = insertedJournalpostFieldList.get(0);
    assertNotNull(insertedJournalpostField.getId());

    // Verify that the journalpost can be found in the database
    var journalpost = journalpostRepository.findById(insertedJournalpostField.getId()).orElse(null);
    assertNotNull(journalpost);

    // Delete the saksmappe, and verify that both the saksmappe and journalpost are
    // deleted from
    // the database
    var deletedSaksmappe = saksmappeService.delete(insertedSaksmappeDTO.getId());
    assertTrue(deletedSaksmappe.getDeleted());
    assertNull(saksmappeRepository.findById(insertedSaksmappeDTO.getId()).orElse(null));
    assertNull(journalpostRepository.findById(insertedJournalpostField.getId()).orElse(null));
  }

  /** Add a new saksmappe with a journalpost and a korrespondansepart */
  @Test
  void addNewSaksmappeWithJournalpostAndKorrespondansepart() throws Exception {
    var saksmappeDTO = getSaksmappeDTO();
    var journalpostDTO = getJournalpostDTO();
    var korrespondansepartDTO = getKorrespondanseparJSON();
    var journalpostField = new ExpandableField<>(journalpostDTO);
    var korrespondansepartField = new ExpandableField<>(korrespondansepartDTO);
    journalpostDTO.setKorrespondansepart(List.of(korrespondansepartField));
    saksmappeDTO.setJournalpost(List.of(journalpostField));

    // Insert saksmappe with journalpost and korrespondansepart
    var insertedSaksmappeDTO = saksmappeService.update(null, saksmappeDTO);
    assertNotNull(insertedSaksmappeDTO.getId());

    // Verify that there is one journalpost in the returned saksmappe
    var insertedJournalpostFieldList = insertedSaksmappeDTO.getJournalpost();
    assertEquals(1, insertedJournalpostFieldList.size());
    var insertedJournalpostField = insertedJournalpostFieldList.get(0);
    assertNotNull(insertedJournalpostField.getId());

    // Verify that there is one korrespondansepart in the returned journalpost
    var insertedKorrespondansepartFieldList =
        insertedJournalpostField.getExpandedObject().getKorrespondansepart();
    assertEquals(1, insertedKorrespondansepartFieldList.size());
    var insertedKorrespondansepartField = insertedKorrespondansepartFieldList.get(0);
    assertNotNull(insertedKorrespondansepartField.getId());

    // Verify that the korrespondansepart can be found in the database
    var journalpost = journalpostRepository.findById(insertedJournalpostField.getId()).orElse(null);
    assertNotNull(journalpost);
    var korrespondansepart =
        korrespondansepartRepository.findById(insertedKorrespondansepartField.getId()).orElse(null);
    assertNotNull(korrespondansepart);

    // Delete the saksmappe, and verify that both the saksmappe, journalpost and
    // korrespondansepart are deleted from the database
    var deletedSaksmappe = saksmappeService.delete(insertedSaksmappeDTO.getId());
    assertTrue(deletedSaksmappe.getDeleted());
    assertNull(saksmappeRepository.findById(insertedSaksmappeDTO.getId()).orElse(null));
    assertNull(journalpostRepository.findById(insertedJournalpostField.getId()).orElse(null));
    assertNull(
        korrespondansepartRepository
            .findById(insertedKorrespondansepartField.getId())
            .orElse(null));
  }

  @Test
  void saksmappeJournalpostShouldGetAdmEnhetFromKorrespondansepart() throws Exception {
    var saksmappeDTO = getSaksmappeDTO();
    var journalpostDTO = getJournalpostDTO();
    var korrespondansepartDTO = getKorrespondanseparJSON();
    var journalpostField = new ExpandableField<>(journalpostDTO);
    var korrespondansepartField = new ExpandableField<>(korrespondansepartDTO);
    journalpostDTO.setKorrespondansepart(List.of(korrespondansepartField));
    saksmappeDTO.setJournalpost(List.of(journalpostField));
    korrespondansepartDTO.setAdministrativEnhet("UNDER");

    // Insert saksmappe with journalpost and korrespondansepart where the korrespondansepart's
    // administrativEnhet is set
    var insertedSaksmappeDTO = saksmappeService.update(null, saksmappeDTO);
    assertNotNull(insertedSaksmappeDTO.getId());

    // Verify that there is one journalpost in the returned saksmappe, with the correct
    // administrativEnhet set
    var insertedJournalpostFieldList = insertedSaksmappeDTO.getJournalpost();
    var insertedJournalpostField = insertedJournalpostFieldList.get(0);
    var insertedJournalpostDTO = insertedJournalpostField.getExpandedObject();
    assertEquals(
        korrespondansepartDTO.getAdministrativEnhet(),
        insertedJournalpostDTO.getAdministrativEnhet());

    // Verify that there is one korrespondansepart in the returned journalpost
    var insertedKorrespondansepartFieldList =
        insertedJournalpostField.getExpandedObject().getKorrespondansepart();
    assertEquals(1, insertedKorrespondansepartFieldList.size());
    var insertedKorrespondansepartField = insertedKorrespondansepartFieldList.get(0);
    assertNotNull(insertedKorrespondansepartField.getId());

    // Verify that the administrativEnhet "UNDER" was found. If so, administrativEnhetObjekt is
    // different from the one in saksmappe (which is equal to journalenhet since no code was given)
    assertNotEquals(
        insertedSaksmappeDTO.getAdministrativEnhetObjekt().getId(),
        insertedJournalpostDTO.getAdministrativEnhetObjekt().getId());

    // Delete the saksmappe, and verify that both the saksmappe, journalpost and korrespondansepart
    // are is deleted from the database
    var deletedSaksmappe = saksmappeService.delete(insertedSaksmappeDTO.getId());
    assertTrue(deletedSaksmappe.getDeleted());
    assertNull(saksmappeRepository.findById(insertedSaksmappeDTO.getId()).orElse(null));
    assertNull(journalpostRepository.findById(insertedJournalpostField.getId()).orElse(null));
    assertNull(
        korrespondansepartRepository
            .findById(insertedKorrespondansepartField.getId())
            .orElse(null));
  }

  /**
   * Test inserting a saksmappe with two journalposts sharing the same dokumentbeskrivelse. Delete
   * the journalposts one by one, and make sure the dokumentbeskrivelse isn't removed before the
   * last journalpost is deleted.
   */
  @Test
  void addSaksmappeWithJournalpostAndDokumentbeskrivelse() throws Exception {
    var saksmappeDTO1 = getSaksmappeDTO();
    var journalpostDTO1 = getJournalpostDTO();
    var journalpostField1 = new ExpandableField<>(journalpostDTO1);
    var dokumentbeskrivelseField = new ExpandableField<>(getDokumentbeskrivelseDTO());
    journalpostDTO1.setDokumentbeskrivelse(List.of(dokumentbeskrivelseField));
    saksmappeDTO1.setJournalpost(List.of(journalpostField1));

    // Insert saksmappe with one journalpost and dokumentbeskrivelse
    var insertedSaksmappeDTO = saksmappeService.update(null, saksmappeDTO1);
    assertNotNull(insertedSaksmappeDTO.getId());

    // Verify that there is one journalpost in the returned saksmappe
    var insertedJournalpostFieldList = insertedSaksmappeDTO.getJournalpost();
    assertEquals(1, insertedJournalpostFieldList.size());
    var insertedJournalpostField = insertedJournalpostFieldList.get(0);
    assertNotNull(insertedJournalpostField.getId());

    // Verify that there is one dokumentbeskrivelse in the returned journalpost
    var insertedDokumentbeskrivelseFieldList =
        insertedJournalpostField.getExpandedObject().getDokumentbeskrivelse();
    assertEquals(1, insertedDokumentbeskrivelseFieldList.size());
    var insertedDokumentbeskrivelse =
        insertedDokumentbeskrivelseFieldList.get(0).getExpandedObject();

    // The Dokumentbeskrivelse should be related to one journalpost
    var dokbesk =
        dokumentbeskrivelseRepository.findById(insertedDokumentbeskrivelse.getId()).orElse(null);
    assertEquals(1, journalpostRepository.countByDokumentbeskrivelse(dokbesk));

    // Add another journalpost with the same dokumentbeskrivelse
    var saksmappeDTO2 = getSaksmappeDTO();
    var journalpostJSON2 = getJournalpostDTO();
    var journalpostField2 = new ExpandableField<>(journalpostJSON2);
    var dokumentbeskrivelseField2 =
        new ExpandableField<DokumentbeskrivelseDTO>(insertedDokumentbeskrivelse.getId(), null);
    journalpostJSON2.setDokumentbeskrivelse(List.of(dokumentbeskrivelseField2));
    saksmappeDTO2.setJournalpost(List.of(journalpostField2));
    insertedSaksmappeDTO = saksmappeService.update(insertedSaksmappeDTO.getId(), saksmappeDTO2);
    insertedJournalpostFieldList = insertedSaksmappeDTO.getJournalpost();
    assertEquals(2, insertedJournalpostFieldList.size());

    // Check that the dokumentbeskrivelse is linked to two journalposts
    assertEquals(2, journalpostRepository.countByDokumentbeskrivelse(dokbesk));

    // Delete one journalpost
    var jpFieldToRemove = insertedJournalpostFieldList.get(0);
    journalpostService.delete(jpFieldToRemove.getId());
    assertNull(journalpostRepository.findById(jpFieldToRemove.getId()).orElse(null));
    assertNotNull(
        dokumentbeskrivelseRepository.findById(insertedDokumentbeskrivelse.getId()).orElse(null));

    // Verify that the dokumentbeskrivelse is still linked to one journalpost
    assertEquals(1, journalpostRepository.countByDokumentbeskrivelse(dokbesk));

    // Delete the other journalpost
    jpFieldToRemove = insertedJournalpostFieldList.get(1);
    journalpostService.delete(jpFieldToRemove.getId());
    assertNull(journalpostRepository.findById(jpFieldToRemove.getId()).orElse(null));
    assertNull(
        dokumentbeskrivelseRepository.findById(insertedDokumentbeskrivelse.getId()).orElse(null));
    assertEquals(0, journalpostRepository.countByDokumentbeskrivelse(dokbesk));

    // Verify that the dokumentbeskrivelse is deleted
    assertNull(
        dokumentbeskrivelseRepository.findById(insertedDokumentbeskrivelse.getId()).orElse(null));

    // Delete the saksmappe
    var deletedSaksmappe = saksmappeService.delete(insertedSaksmappeDTO.getId());
    assertTrue(deletedSaksmappe.getDeleted());
    assertNull(saksmappeRepository.findById(insertedSaksmappeDTO.getId()).orElse(null));
  }
}
