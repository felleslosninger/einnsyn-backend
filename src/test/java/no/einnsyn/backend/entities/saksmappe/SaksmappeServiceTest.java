package no.einnsyn.backend.entities.saksmappe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import java.util.List;
import no.einnsyn.backend.EinnsynServiceTestBase;
import no.einnsyn.backend.authentication.apikey.models.ApiKeyUserDetails;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

class SaksmappeServiceTest extends EinnsynServiceTestBase {

  @Autowired private SaksmappeService saksmappeService;

  @Mock Authentication authentication;
  @Mock SecurityContext securityContext;
  @Autowired protected Gson gson;

  @BeforeAll
  void setupMock() {
    var apiKey = apiKeyService.findBySecretKey(adminKey);
    var enhetId = apiKey.getEnhet().getId();
    var subtreeList = enhetService.getSubtreeIdList(enhetId);
    var apiKeyUserDetails = new ApiKeyUserDetails(apiKey, enhetId, subtreeList);
    when(authentication.getPrincipal()).thenReturn(apiKeyUserDetails);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
  }

  /** Add a new saksmappe */
  @Test
  void addNewSaksmappe() throws Exception {
    var saksmappeDTO = getSaksmappeDTO();

    // Insert the saksmappe, and verify returned content
    var insertedSaksmappe = saksmappeService.add(saksmappeDTO);
    assertNotNull(insertedSaksmappe.getId());
    assertEquals(insertedSaksmappe.getOffentligTittel(), saksmappeDTO.getOffentligTittel());
    assertEquals(
        insertedSaksmappe.getOffentligTittelSensitiv(), saksmappeDTO.getOffentligTittelSensitiv());
    assertEquals(insertedSaksmappe.getBeskrivelse(), saksmappeDTO.getBeskrivelse());
    assertEquals(insertedSaksmappe.getSaksaar(), saksmappeDTO.getSaksaar());

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
    var insertedSaksmappeWithAdmEnhet = saksmappeService.add(saksmappeDTOWithAdmEnhet);
    assertNotNull(insertedSaksmappeWithAdmEnhet.getId());
    assertEquals("UNDER", insertedSaksmappeWithAdmEnhet.getAdministrativEnhet());

    var saksmappeDTOWithoutAdmEnhet = getSaksmappeDTO();
    var insertedSaksmappeWithoutAdmEnhet = saksmappeService.add(saksmappeDTOWithoutAdmEnhet);
    assertNotNull(insertedSaksmappeWithoutAdmEnhet.getId());

    assertNotEquals(
        insertedSaksmappeWithAdmEnhet.getAdministrativEnhetObjekt().getId(),
        insertedSaksmappeWithoutAdmEnhet.getAdministrativEnhetObjekt().getId());

    // Delete the saksmappes
    var deletedSaksmappeWithAdmEnhet =
        saksmappeService.delete(insertedSaksmappeWithAdmEnhet.getId());
    assertTrue(deletedSaksmappeWithAdmEnhet.getDeleted());
    var deletedSaksmappeWithoutAdmEnhet =
        saksmappeService.delete(insertedSaksmappeWithoutAdmEnhet.getId());
    assertTrue(deletedSaksmappeWithoutAdmEnhet.getDeleted());
  }

  /** Add a new saksmappe with a journalpost */
  @Test
  @Transactional
  void addNewSaksmappeWithJournalpost() throws Exception {
    var saksmappeDTO = getSaksmappeDTO();
    var journalpostJSON = getJournalpostDTO();
    var journalpostField = new ExpandableField<>(journalpostJSON);
    saksmappeDTO.setJournalpost(List.of(journalpostField));

    // Insert saksmappe with journalpost
    var insertedSaksmappeDTO = saksmappeService.add(saksmappeDTO);
    assertNotNull(insertedSaksmappeDTO.getId());

    // Verify that the journalpost can be found in the database
    var journalpostId =
        journalpostRepository.findIdsBySaksmappe(insertedSaksmappeDTO.getId()).toList().getFirst();
    var journalpost = journalpostRepository.findById(journalpostId).orElse(null);
    assertNotNull(journalpost);

    // Delete the saksmappe, and verify that both the saksmappe and journalpost are
    // deleted from
    // the database
    var deletedSaksmappe = saksmappeService.delete(insertedSaksmappeDTO.getId());
    assertTrue(deletedSaksmappe.getDeleted());
    assertNull(saksmappeRepository.findById(insertedSaksmappeDTO.getId()).orElse(null));
    assertNull(journalpostRepository.findById(journalpostId).orElse(null));
  }

  /** Add a new saksmappe with a journalpost and a korrespondansepart */
  @Test
  @Transactional
  void addNewSaksmappeWithJournalpostAndKorrespondansepart() throws Exception {
    var saksmappeDTO = getSaksmappeDTO();
    var journalpostDTO = getJournalpostDTO();
    var korrespondansepartDTO = getKorrespondansepartDTO();
    var journalpostField = new ExpandableField<>(journalpostDTO);
    var korrespondansepartField = new ExpandableField<>(korrespondansepartDTO);
    journalpostDTO.setKorrespondansepart(List.of(korrespondansepartField));
    saksmappeDTO.setJournalpost(List.of(journalpostField));

    // Insert saksmappe with journalpost and korrespondansepart
    var insertedSaksmappeDTO = saksmappeService.add(saksmappeDTO);
    assertNotNull(insertedSaksmappeDTO.getId());

    // Find journalposts
    var journalpostList =
        journalpostRepository
            .findIdsBySaksmappe(insertedSaksmappeDTO.getId())
            .map(id -> journalpostRepository.findById(id).orElse(null))
            .toList();

    // Verify that there is one journalpost in the returned saksmappe
    assertEquals(1, journalpostList.size());
    var insertedJournalpost = journalpostList.get(0);
    assertNotNull(insertedJournalpost.getId());

    // Verify that there is one korrespondansepart in the returned journalpost
    var insertedKorrespondansepartList = insertedJournalpost.getKorrespondansepart();
    assertEquals(1, insertedKorrespondansepartList.size());
    var insertedKorrespondansepartField = insertedKorrespondansepartList.get(0);
    assertNotNull(insertedKorrespondansepartField.getId());

    // Delete the saksmappe, and verify that both the saksmappe, journalpost and
    // korrespondansepart are deleted from the database
    var deletedSaksmappe = saksmappeService.delete(insertedSaksmappeDTO.getId());
    assertTrue(deletedSaksmappe.getDeleted());
    assertNull(saksmappeRepository.findById(insertedSaksmappeDTO.getId()).orElse(null));
    assertNull(journalpostRepository.findById(insertedJournalpost.getId()).orElse(null));
    assertNull(
        korrespondansepartRepository
            .findById(insertedKorrespondansepartField.getId())
            .orElse(null));
  }

  @Test
  @Transactional
  void saksmappeJournalpostShouldGetAdmEnhetFromKorrespondansepart() throws Exception {
    var saksmappeDTO = getSaksmappeDTO();
    var journalpostDTO = getJournalpostDTO();
    var korrespondansepartDTO = getKorrespondansepartDTO();
    var journalpostField = new ExpandableField<>(journalpostDTO);
    var korrespondansepartField = new ExpandableField<>(korrespondansepartDTO);
    journalpostDTO.setKorrespondansepart(List.of(korrespondansepartField));
    saksmappeDTO.setJournalpost(List.of(journalpostField));
    korrespondansepartDTO.setErBehandlingsansvarlig(true);
    korrespondansepartDTO.setAdministrativEnhet("UNDER");

    // Insert saksmappe with journalpost and korrespondansepart where the korrespondansepart's
    // administrativEnhet is set
    var insertedSaksmappeDTO = saksmappeService.add(saksmappeDTO);
    assertNotNull(insertedSaksmappeDTO.getId());

    // Get journalpost
    var insertedJournalpost =
        journalpostRepository
            .findIdsBySaksmappe(insertedSaksmappeDTO.getId())
            .map(id -> journalpostRepository.findById(id).orElse(null))
            .toList()
            .getFirst();

    // Verify that there is one journalpost in the returned saksmappe, with the correct
    // administrativEnhet set
    var administrativEnhetKode =
        journalpostService.getAdministrativEnhetKode(insertedJournalpost.getId());
    assertEquals(korrespondansepartDTO.getAdministrativEnhet(), administrativEnhetKode);

    // Verify that there is one korrespondansepart in the returned journalpost
    var insertedKorrespondansepartList = insertedJournalpost.getKorrespondansepart();
    assertEquals(1, insertedKorrespondansepartList.size());
    var insertedKorrespondansepartField = insertedKorrespondansepartList.get(0);
    assertNotNull(insertedKorrespondansepartField.getId());

    // Verify that the administrativEnhet "UNDER" was found. If so, administrativEnhetObjekt is
    // different from the one in saksmappe (which is equal to journalenhet since no code was given)
    assertNotEquals(
        insertedSaksmappeDTO.getAdministrativEnhetObjekt().getId(),
        insertedJournalpost.getAdministrativEnhetObjekt().getId());

    // Delete the saksmappe, and verify that both the saksmappe, journalpost and korrespondansepart
    // are is deleted from the database
    var deletedSaksmappe = saksmappeService.delete(insertedSaksmappeDTO.getId());
    assertTrue(deletedSaksmappe.getDeleted());
    assertNull(saksmappeRepository.findById(insertedSaksmappeDTO.getId()).orElse(null));
    assertNull(journalpostRepository.findById(insertedJournalpost.getId()).orElse(null));
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
  @Transactional
  void addSaksmappeWithJournalpostAndDokumentbeskrivelse() throws Exception {
    var saksmappeDTO1 = getSaksmappeDTO();
    var journalpostDTO1 = getJournalpostDTO();
    var journalpostField1 = new ExpandableField<>(journalpostDTO1);
    var dokumentbeskrivelseField = new ExpandableField<>(getDokumentbeskrivelseDTO());
    journalpostDTO1.setDokumentbeskrivelse(List.of(dokumentbeskrivelseField));
    saksmappeDTO1.setJournalpost(List.of(journalpostField1));

    // Insert saksmappe with one journalpost and dokumentbeskrivelse
    var insertedSaksmappeDTO = saksmappeService.add(saksmappeDTO1);
    assertNotNull(insertedSaksmappeDTO.getId());

    // Verify that there is one journalpost in the returned saksmappe
    var insertedJournalpostList =
        journalpostRepository
            .findIdsBySaksmappe(insertedSaksmappeDTO.getId())
            .map(id -> journalpostRepository.findById(id).orElse(null))
            .toList();

    assertEquals(1, insertedJournalpostList.size());

    var insertedJournalpost = insertedJournalpostList.getFirst();
    assertNotNull(insertedJournalpost.getId());

    // Verify that there is one dokumentbeskrivelse in the returned journalpost
    var insertedDokumentbeskrivelseList = insertedJournalpost.getDokumentbeskrivelse();
    assertEquals(1, insertedDokumentbeskrivelseList.size());
    var insertedDokumentbeskrivelse = insertedDokumentbeskrivelseList.getFirst();

    // The Dokumentbeskrivelse should be related to one journalpost
    assertEquals(1, journalpostRepository.countByDokumentbeskrivelse(insertedDokumentbeskrivelse));

    // Add another journalpost with the same dokumentbeskrivelse
    var saksmappeDTO2 = getSaksmappeDTO();
    var journalpostJSON2 = getJournalpostDTO();
    var journalpostField2 = new ExpandableField<>(journalpostJSON2);
    var dokumentbeskrivelseField2 =
        new ExpandableField<DokumentbeskrivelseDTO>(insertedDokumentbeskrivelse.getId(), null);
    journalpostJSON2.setDokumentbeskrivelse(List.of(dokumentbeskrivelseField2));
    saksmappeDTO2.setJournalpost(List.of(journalpostField2));
    insertedSaksmappeDTO = saksmappeService.update(insertedSaksmappeDTO.getId(), saksmappeDTO2);

    insertedJournalpostList =
        journalpostRepository
            .findIdsBySaksmappe(insertedSaksmappeDTO.getId())
            .map(id -> journalpostRepository.findById(id).orElse(null))
            .toList();
    assertEquals(2, insertedJournalpostList.size());

    // Check that the dokumentbeskrivelse is linked to two journalposts
    assertEquals(2, journalpostRepository.countByDokumentbeskrivelse(insertedDokumentbeskrivelse));

    // Delete one journalpost
    var jpFieldToRemove = insertedJournalpostList.getFirst();
    journalpostService.delete(jpFieldToRemove.getId());
    assertNull(journalpostRepository.findById(jpFieldToRemove.getId()).orElse(null));
    assertNotNull(
        dokumentbeskrivelseRepository.findById(insertedDokumentbeskrivelse.getId()).orElse(null));

    // Verify that the dokumentbeskrivelse is still linked to one journalpost
    assertEquals(1, journalpostRepository.countByDokumentbeskrivelse(insertedDokumentbeskrivelse));

    // Delete the other journalpost
    jpFieldToRemove = insertedJournalpostList.get(1);
    journalpostService.delete(jpFieldToRemove.getId());
    assertNull(journalpostRepository.findById(jpFieldToRemove.getId()).orElse(null));
    assertNull(
        dokumentbeskrivelseRepository.findById(insertedDokumentbeskrivelse.getId()).orElse(null));
    assertEquals(0, journalpostRepository.countByDokumentbeskrivelse(insertedDokumentbeskrivelse));

    // Verify that the dokumentbeskrivelse is deleted
    assertNull(
        dokumentbeskrivelseRepository.findById(insertedDokumentbeskrivelse.getId()).orElse(null));

    // Delete the saksmappe
    var deletedSaksmappe = saksmappeService.delete(insertedSaksmappeDTO.getId());
    assertTrue(deletedSaksmappe.getDeleted());
    assertNull(saksmappeRepository.findById(insertedSaksmappeDTO.getId()).orElse(null));
  }
}
