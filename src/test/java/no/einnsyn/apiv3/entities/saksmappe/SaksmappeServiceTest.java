package no.einnsyn.apiv3.entities.saksmappe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import no.einnsyn.apiv3.entities.EinnsynServiceTestBase;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseJSON;
import no.einnsyn.apiv3.entities.enhet.models.EnhetJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostJSON;
import no.einnsyn.apiv3.entities.korrespondansepart.models.Korrespondansepart;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartJSON;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeJSON;

class SaksmappeServiceTest extends EinnsynServiceTestBase {

  @Autowired
  private SaksmappeService saksmappeService;


  /**
   * Add a new saksmappe
   */
  @Test
  void addNewSaksmappe() {
    SaksmappeJSON saksmappeJSON = getSaksmappeJSON();

    // Insert the saksmappe, and verify returned content
    SaksmappeJSON insertedSaksmappe = saksmappeService.update(null, saksmappeJSON);
    assertNotNull(insertedSaksmappe.getId());
    assertEquals(insertedSaksmappe.getOffentligTittel(), saksmappeJSON.getOffentligTittel());
    assertEquals(insertedSaksmappe.getOffentligTittelSensitiv(),
        saksmappeJSON.getOffentligTittelSensitiv());
    assertEquals(insertedSaksmappe.getBeskrivelse(), saksmappeJSON.getBeskrivelse());
    assertEquals(insertedSaksmappe.getSaksaar(), saksmappeJSON.getSaksaar());

    // Verify that journalenhet was inserted
    ExpandableField<EnhetJSON> journalenhetField = insertedSaksmappe.getJournalenhet();
    assertNotNull(journalenhetField, "Inserted saksmappe should have a journalenhet field");
    assertNotNull(journalenhetField.getId(), "Inserted saksmappe should have a journalenhet ID");

    // Verify that the saksmappe can be found in the database
    Saksmappe saksmappe = saksmappeRepository.findById(insertedSaksmappe.getId());
    assertNotNull(saksmappe);

    // Delete the saksmappe
    SaksmappeJSON deletedSaksmappe = saksmappeService.delete(insertedSaksmappe.getId());
    assertTrue(deletedSaksmappe.getDeleted());
    assertNull(saksmappeRepository.findById(insertedSaksmappe.getId()));
  }


  /**
   * Add a new saksmappe with administrativEnhet set. Make sure administrativEnhetObjekt is found in
   * the tree below journalenhet.
   */
  @Test
  void addNewSaksmappeWithAdministrativEnhet() {
    SaksmappeJSON saksmappeJSONWithAdmEnhet = getSaksmappeJSON();
    saksmappeJSONWithAdmEnhet.setAdministrativEnhet("UNDER");
    SaksmappeJSON insertedSaksmappeWithAdmEnhet =
        saksmappeService.update(null, saksmappeJSONWithAdmEnhet);
    assertNotNull(insertedSaksmappeWithAdmEnhet.getId());
    assertEquals("UNDER", insertedSaksmappeWithAdmEnhet.getAdministrativEnhet());

    SaksmappeJSON saksmappeJSONWithoutAdmEnhet = getSaksmappeJSON();
    SaksmappeJSON insertedSaksmappeWithoutAdmEnhet =
        saksmappeService.update(null, saksmappeJSONWithoutAdmEnhet);
    assertNotNull(insertedSaksmappeWithoutAdmEnhet.getId());

    assertNotEquals(insertedSaksmappeWithAdmEnhet.getAdministrativEnhetObjekt().getId(),
        insertedSaksmappeWithoutAdmEnhet.getAdministrativEnhetObjekt().getId());

    // Delete the saksmappe
    SaksmappeJSON deletedSaksmappeWithAdmEnhet =
        saksmappeService.delete(insertedSaksmappeWithAdmEnhet.getId());
    assertTrue(deletedSaksmappeWithAdmEnhet.getDeleted());
  }


  /**
   * Add a new saksmappe with a journalpost
   */
  @Test
  void addNewSaksmappeWithJournalpost() {
    SaksmappeJSON saksmappeJSON = getSaksmappeJSON();
    JournalpostJSON journalpostJSON = getJournalpostJSON();
    ExpandableField<JournalpostJSON> journalpostField = new ExpandableField<>(journalpostJSON);
    saksmappeJSON.setJournalpost(Arrays.asList(journalpostField));

    // Insert saksmappe with journalpost
    SaksmappeJSON insertedSaksmappeJSON = saksmappeService.update(null, saksmappeJSON);
    assertNotNull(insertedSaksmappeJSON.getId());

    // Verify that there is one journalpost in the returned saksmappe
    List<ExpandableField<JournalpostJSON>> insertedJournalpostFieldList =
        insertedSaksmappeJSON.getJournalpost();
    assertEquals(1, insertedJournalpostFieldList.size());
    ExpandableField<JournalpostJSON> insertedJournalpostField = insertedJournalpostFieldList.get(0);
    assertNotNull(insertedJournalpostField.getId());

    // Verify that the journalpost can be found in the database
    Journalpost journalpost = journalpostRepository.findById(insertedJournalpostField.getId());
    assertNotNull(journalpost);

    // Delete the saksmappe, and verify that both the saksmappe and journalpost are deleted from
    // the database
    SaksmappeJSON deletedSaksmappe = saksmappeService.delete(insertedSaksmappeJSON.getId());
    assertTrue(deletedSaksmappe.getDeleted());
    assertNull(saksmappeRepository.findById(insertedSaksmappeJSON.getId()));
    assertNull(journalpostRepository.findById(insertedJournalpostField.getId()));
  }


  /**
   * Add a new saksmappe with a journalpost and a korrespondansepart
   */
  @Test
  void addNewSaksmappeWithJournalpostAndKorrespondansepart() {
    SaksmappeJSON saksmappeJSON = getSaksmappeJSON();
    JournalpostJSON journalpostJSON = getJournalpostJSON();
    KorrespondansepartJSON korrespondansepartJSON = getKorrespondanseparJSON();
    ExpandableField<JournalpostJSON> journalpostField = new ExpandableField<>(journalpostJSON);
    ExpandableField<KorrespondansepartJSON> korrespondansepartField =
        new ExpandableField<>(korrespondansepartJSON);
    journalpostJSON.setKorrespondansepart(Arrays.asList(korrespondansepartField));
    saksmappeJSON.setJournalpost(Arrays.asList(journalpostField));

    // Insert saksmappe with journalpost and korrespondansepart
    SaksmappeJSON insertedSaksmappeJSON = saksmappeService.update(null, saksmappeJSON);
    assertNotNull(insertedSaksmappeJSON.getId());

    // Verify that there is one journalpost in the returned saksmappe
    List<ExpandableField<JournalpostJSON>> insertedJournalpostFieldList =
        insertedSaksmappeJSON.getJournalpost();
    assertEquals(1, insertedJournalpostFieldList.size());
    ExpandableField<JournalpostJSON> insertedJournalpostField = insertedJournalpostFieldList.get(0);
    assertNotNull(insertedJournalpostField.getId());

    // Verify that there is one korrespondansepart in the returned journalpost
    List<ExpandableField<KorrespondansepartJSON>> insertedKorrespondansepartFieldList =
        insertedJournalpostField.getExpandedObject().getKorrespondansepart();
    assertEquals(1, insertedKorrespondansepartFieldList.size());
    ExpandableField<KorrespondansepartJSON> insertedKorrespondansepartField =
        insertedKorrespondansepartFieldList.get(0);
    assertNotNull(insertedKorrespondansepartField.getId());

    // Verify that the korrespondansepart can be found in the database
    Journalpost journalpost = journalpostRepository.findById(insertedJournalpostField.getId());
    assertNotNull(journalpost);
    Korrespondansepart korrespondansepart =
        korrespondansepartRepository.findById(insertedKorrespondansepartField.getId());
    assertNotNull(korrespondansepart);

    // Delete the saksmappe, and verify that both the saksmappe, journalpost and
    // korrespondansepart are deleted from the database
    SaksmappeJSON deletedSaksmappe = saksmappeService.delete(insertedSaksmappeJSON.getId());
    assertTrue(deletedSaksmappe.getDeleted());
    assertNull(saksmappeRepository.findById(insertedSaksmappeJSON.getId()));
    assertNull(journalpostRepository.findById(insertedJournalpostField.getId()));
    assertNull(korrespondansepartRepository.findById(insertedKorrespondansepartField.getId()));
  }


  @Test
  void saksmappeJournalpostShouldGetAdmEnhetFromKorrespondansepart() {
    SaksmappeJSON saksmappeJSON = getSaksmappeJSON();
    JournalpostJSON journalpostJSON = getJournalpostJSON();
    KorrespondansepartJSON korrespondansepartJSON = getKorrespondanseparJSON();
    ExpandableField<JournalpostJSON> journalpostField = new ExpandableField<>(journalpostJSON);
    ExpandableField<KorrespondansepartJSON> korrespondansepartField =
        new ExpandableField<>(korrespondansepartJSON);
    journalpostJSON.setKorrespondansepart(Arrays.asList(korrespondansepartField));
    saksmappeJSON.setJournalpost(Arrays.asList(journalpostField));
    korrespondansepartJSON.setAdministrativEnhet("UNDER");

    // Insert saksmappe with journalpost and korrespondansepart where the korrespondansepart's
    // administrativEnhet is set
    SaksmappeJSON insertedSaksmappeJSON = saksmappeService.update(null, saksmappeJSON);
    assertNotNull(insertedSaksmappeJSON.getId());

    // Verify that there is one journalpost in the returned saksmappe, with the correct
    // administrativEnhet set
    List<ExpandableField<JournalpostJSON>> insertedJournalpostFieldList =
        insertedSaksmappeJSON.getJournalpost();
    ExpandableField<JournalpostJSON> insertedJournalpostField = insertedJournalpostFieldList.get(0);
    JournalpostJSON insertedJournalpostJSON = insertedJournalpostField.getExpandedObject();
    assertEquals(korrespondansepartJSON.getAdministrativEnhet(),
        insertedJournalpostJSON.getAdministrativEnhet());

    // Verify that the administrativEnhet "UNDER" was found. If so, administrativEnhetObjekt is
    // different from the one in saksmappe (which is equal to journalenhet since no code was
    // given)
    assertNotEquals(insertedSaksmappeJSON.getAdministrativEnhetObjekt().getId(),
        insertedJournalpostJSON.getAdministrativEnhetObjekt().getId());

    // Delete the saksmappe, and verify that both the saksmappe, journalpost and
    // korrespondansepart are is deleted from the database
    SaksmappeJSON deletedSaksmappe = saksmappeService.delete(insertedSaksmappeJSON.getId());
    assertTrue(deletedSaksmappe.getDeleted());
    assertNull(saksmappeRepository.findById(insertedSaksmappeJSON.getId()));
    assertNull(journalpostRepository.findById(insertedJournalpostField.getId()));
    assertNull(korrespondansepartRepository.findById(korrespondansepartField.getId()));
  }

  /**
   * Test inserting a saksmappe with two journalposts sharing the same dokumentbeskrivelse. Delete
   * the journalposts one by one, and make sure the dokumentbeskrivelse isn't removed before the
   * last journalpost is deleted.
   */
  @Test
  void addSaksmappeWithJournalpostAndDokumentbeskrivelse() {
    SaksmappeJSON saksmappeJSON1 = getSaksmappeJSON();
    JournalpostJSON journalpostJSON1 = getJournalpostJSON();
    ExpandableField<JournalpostJSON> journalpostField1 = new ExpandableField<>(journalpostJSON1);
    ExpandableField<DokumentbeskrivelseJSON> dokumentbeskrivelseField =
        new ExpandableField<>(getDokumentbeskrivelseJSON());
    journalpostJSON1.setDokumentbeskrivelse(Arrays.asList(dokumentbeskrivelseField));
    saksmappeJSON1.setJournalpost(Arrays.asList(journalpostField1));

    // Insert saksmappe with one journalpost and dokumentbeskrivelse
    SaksmappeJSON insertedSaksmappeJSON = saksmappeService.update(null, saksmappeJSON1);
    assertNotNull(insertedSaksmappeJSON.getId());

    // Verify that there is one journalpost in the returned saksmappe
    List<ExpandableField<JournalpostJSON>> insertedJournalpostFieldList =
        insertedSaksmappeJSON.getJournalpost();
    assertEquals(1, insertedJournalpostFieldList.size());
    ExpandableField<JournalpostJSON> insertedJournalpostField = insertedJournalpostFieldList.get(0);
    assertNotNull(insertedJournalpostField.getId());

    // Verify that there is one dokumentbeskrivelse in the returned journalpost
    List<ExpandableField<DokumentbeskrivelseJSON>> insertedDokumentbeskrivelseFieldList =
        insertedJournalpostField.getExpandedObject().getDokumentbeskrivelse();
    assertEquals(1, insertedDokumentbeskrivelseFieldList.size());
    DokumentbeskrivelseJSON insertedDokumentbeskrivelse =
        insertedDokumentbeskrivelseFieldList.get(0).getExpandedObject();

    // The Dokumentbeskrivelse should be related to one journalpost
    Dokumentbeskrivelse dokbesk =
        dokumentbeskrivelseRepository.findById(insertedDokumentbeskrivelse.getId());
    assertEquals(1, journalpostRepository.countByDokumentbeskrivelse(dokbesk));

    // Add another journalpost with the same dokumentbeskrivelse
    SaksmappeJSON saksmappeJSON2 = getSaksmappeJSON();
    JournalpostJSON journalpostJSON2 = getJournalpostJSON();
    ExpandableField<JournalpostJSON> journalpostField2 = new ExpandableField<>(journalpostJSON2);
    ExpandableField<DokumentbeskrivelseJSON> dokumentbeskrivelseField2 =
        new ExpandableField<>(insertedDokumentbeskrivelse.getId(), null);
    journalpostJSON2.setDokumentbeskrivelse(Arrays.asList(dokumentbeskrivelseField2));
    saksmappeJSON2.setJournalpost(Arrays.asList(journalpostField2));
    insertedSaksmappeJSON = saksmappeService.update(insertedSaksmappeJSON.getId(), saksmappeJSON2);
    insertedJournalpostFieldList = insertedSaksmappeJSON.getJournalpost();
    assertEquals(2, insertedJournalpostFieldList.size());

    // Check that the dokumentbeskrivelse is linked to two journalposts
    assertEquals(2, journalpostRepository.countByDokumentbeskrivelse(dokbesk));

    // Delete one journalpost
    ExpandableField<JournalpostJSON> jpFieldToRemove = insertedJournalpostFieldList.get(0);
    journalpostService.delete(jpFieldToRemove.getId());
    assertNull(journalpostRepository.findById(jpFieldToRemove.getId()));
    assertNotNull(dokumentbeskrivelseRepository.findById(insertedDokumentbeskrivelse.getId()));
    assertEquals(1, journalpostRepository.countByDokumentbeskrivelse(dokbesk));

    // Delete the other journalpost
    jpFieldToRemove = insertedJournalpostFieldList.get(1);
    journalpostService.delete(jpFieldToRemove.getId());
    assertNull(journalpostRepository.findById(jpFieldToRemove.getId()));
    assertNull(dokumentbeskrivelseRepository.findById(insertedDokumentbeskrivelse.getId()));
    assertEquals(0, journalpostRepository.countByDokumentbeskrivelse(dokbesk));
  }

}
