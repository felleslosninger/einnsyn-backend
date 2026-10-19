package no.einnsyn.backend.entities.saksmappe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import com.google.gson.Gson;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import no.einnsyn.backend.EinnsynServiceTestBase;
import no.einnsyn.backend.authentication.AuthenticationService;
import no.einnsyn.backend.authentication.EInnsynAuthentication;
import no.einnsyn.backend.authentication.EInnsynPrincipalEnhet;
import no.einnsyn.backend.common.exceptions.models.ConflictException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.journalpost.JournalpostRepository;
import no.einnsyn.backend.entities.journalpost.JournalpostService;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.utils.SlugGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class SaksmappeServiceTest extends EinnsynServiceTestBase {

  @Autowired private AuthenticationService authenticationService;
  @Autowired private SaksmappeService saksmappeService;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private EntityManager entityManager;
  @Autowired private PlatformTransactionManager transactionManager;

  @Mock private EInnsynAuthentication authentication;
  @Autowired protected Gson gson;

  @BeforeEach
  void setupMock() {
    var apiKey = apiKeyService.findBySecretKey(adminKey);
    var enhetId = apiKey.getEnhet().getId();
    var enhetOrgno = apiKey.getEnhet().getOrgnummer();
    var principal = new EInnsynPrincipalEnhet("ApiKey", apiKey.getId(), enhetId, enhetOrgno, false);
    doReturn(principal).when(authentication).getPrincipal();

    var authorities =
        authenticationService.getAuthoritiesFromEnhet(List.of(apiKey.getEnhet()), "Write");
    doReturn(authorities).when(authentication).getAuthorities();

    var securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(authentication);
    SecurityContextHolder.setContext(securityContext);
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
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

    // Verify that slug was generated
    var saksmappe = saksmappeRepository.findById(insertedSaksmappe.getId()).orElse(null);
    assertNotNull(saksmappe);
    assertNotNull(saksmappe.getSlug(), "Slug should be generated for saksmappe");
    var expectedSlug =
        SlugGenerator.generate(
            saksmappeDTO.getSaksaar()
                + "-"
                + saksmappeDTO.getSakssekvensnummer()
                + "-"
                + saksmappeDTO.getOffentligTittel(),
            false);
    assertEquals(expectedSlug, saksmappe.getSlug(), "Slug should be correctly generated");

    // Verify that journalenhet was inserted
    var journalenhetField = insertedSaksmappe.getJournalenhet();
    assertNotNull(journalenhetField, "Inserted saksmappe should have a journalenhet field");
    assertNotNull(journalenhetField.getId(), "Inserted saksmappe should have a journalenhet ID");

    // Verify that we can get by slug
    var fetchedBySlug = saksmappeService.find(expectedSlug);
    assertNotNull(fetchedBySlug, "Should be able to fetch saksmappe by slug");
    assertEquals(
        insertedSaksmappe.getId(), fetchedBySlug.getId(), "Fetched saksmappe ID should match");

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
        journalpostRepository
            .streamIdBySaksmappeId(insertedSaksmappeDTO.getId())
            .toList()
            .getFirst();
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
            .streamIdBySaksmappeId(insertedSaksmappeDTO.getId())
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
            .streamIdBySaksmappeId(insertedSaksmappeDTO.getId())
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
            .streamIdBySaksmappeId(insertedSaksmappeDTO.getId())
            .map(id -> journalpostRepository.findById(id).orElse(null))
            .toList();

    assertEquals(1, insertedJournalpostList.size());

    var insertedJournalpost = insertedJournalpostList.getFirst();
    assertNotNull(insertedJournalpost.getId());

    // Verify that there is one dokumentbeskrivelse in the returned journalpost
    var insertedDokumentbeskrivelseList = insertedJournalpost.getDokumentbeskrivelse();
    assertEquals(1, insertedDokumentbeskrivelseList.size());
    var insertedDokumentbeskrivelse = insertedDokumentbeskrivelseList.iterator().next();

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
            .streamIdBySaksmappeId(insertedSaksmappeDTO.getId())
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

  /**
   * Forces the race the duplicate pre-check cannot see: the first lookup misses, the insert hits
   * the unique index, and the retry's lookup finds the winner.
   */
  @Test
  void addRetriesUniqueViolationAndReportsConflict() throws Exception {
    var externalId = "addRetriesUniqueViolationAndReportsConflict";
    var winner = getSaksmappeDTO();
    winner.setExternalId(externalId);
    var winnerId = saksmappeService.add(winner).getId();

    var target = AopTestUtils.<SaksmappeService>getUltimateTargetObject(saksmappeService);
    var repository =
        mock(
            SaksmappeRepository.class,
            withSettings().defaultAnswer(delegatesTo(saksmappeRepository)));
    doReturn(null)
        .doAnswer(delegatesTo(saksmappeRepository))
        .when(repository)
        .findByExternalId(externalId);
    ReflectionTestUtils.setField(target, "repository", repository);
    try {
      var loser = getSaksmappeDTO();
      loser.setExternalId(externalId);
      assertThrows(ConflictException.class, () -> saksmappeService.add(loser));
      // The stubbed miss, the identifier check and its same-field lookup leaving the winner to the
      // unique index, then the retry seeing the winner
      verify(repository, times(4)).findByExternalId(externalId);
    } finally {
      ReflectionTestUtils.setField(target, "repository", saksmappeRepository);
      saksmappeService.delete(winnerId);
    }
  }

  /**
   * The same forced race for the insert an update performs through findOrCreate: the nested lookup
   * misses, the journalpost insert hits the unique index, and the retry finds the winner's
   * journalpost and reassigns it.
   */
  @Test
  void updateRetriesUniqueViolationAndReassignsNestedObject() throws Exception {
    var externalId = "updateRetriesUniqueViolationAndReassignsNestedObject";
    var winnerId = saksmappeService.add(getSaksmappeDTO()).getId();
    var loserId = saksmappeService.add(getSaksmappeDTO()).getId();
    saksmappeService.update(winnerId, withNestedJournalpost(externalId));

    var target = AopTestUtils.<JournalpostService>getUltimateTargetObject(journalpostService);
    var repository =
        mock(
            JournalpostRepository.class,
            withSettings().defaultAnswer(delegatesTo(journalpostRepository)));
    doReturn(null)
        .doAnswer(delegatesTo(journalpostRepository))
        .when(repository)
        .findByExternalId(externalId);
    ReflectionTestUtils.setField(target, "repository", repository);
    try {
      saksmappeService.update(loserId, withNestedJournalpost(externalId));
      // The stubbed miss and the identifier check with its same-field lookup, then the retry
      // seeing the winner's journalpost and the identifier check finding that same journalpost
      verify(repository, times(5)).findByExternalId(externalId);

      var journalpostId = journalpostRepository.findByExternalId(externalId).getId();
      assertEquals(loserId, journalpostService.get(journalpostId).getSaksmappe().getId());
    } finally {
      ReflectionTestUtils.setField(target, "repository", journalpostRepository);
      saksmappeService.delete(winnerId);
      saksmappeService.delete(loserId);
    }
  }

  /**
   * isIdentifierTaken() must not trust entities loaded earlier in the transaction.
   *
   * <p>The first Saksmappe is loaded with externalId X. Another transaction then moves X to its
   * slug, and the second Saksmappe asks for X as externalId. A duplicate in the same field returns
   * false, since the unique index should handle it. Reading the loaded Saksmappe, the check would
   * see such a duplicate and return false. Reading the database, it sees X as a slug and returns
   * true.
   */
  @Test
  void isIdentifierTakenIgnoresStaleEntity() throws Exception {
    var externalId = "externalId";
    var firstDTO = getSaksmappeDTO();
    firstDTO.setExternalId(externalId);
    var firstId = saksmappeService.add(firstDTO).getId();
    var secondId = saksmappeService.add(getSaksmappeDTO()).getId();

    var newTransaction = new TransactionTemplate(transactionManager);
    newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    try {
      new TransactionTemplate(transactionManager)
          .executeWithoutResult(
              status -> {
                var first = saksmappeRepository.findById(firstId).orElseThrow();
                var second = saksmappeRepository.findById(secondId).orElseThrow();
                assertFalse(
                    saksmappeService.isIdentifierTaken(
                        second, externalId, saksmappeRepository::findByExternalId),
                    "A duplicate in the same field is left to the unique index");

                newTransaction.executeWithoutResult(
                    moveStatus ->
                        jdbcTemplate.update(
                            "UPDATE saksmappe SET _external_id = ?, slug = ? WHERE _id = ?",
                            "moved-" + externalId,
                            externalId,
                            firstId));
                assertEquals(
                    externalId, first.getExternalId(), "The loaded Saksmappe is now stale");

                assertTrue(
                    saksmappeService.isIdentifierTaken(
                        second, externalId, saksmappeRepository::findByExternalId),
                    "The first Saksmappe now has the value as slug");
              });
    } finally {
      saksmappeService.delete(firstId);
      saksmappeService.delete(secondId);
    }
  }

  /**
   * Race two transactions that try to insert the same identifier in different fields. The first
   * should succeed, the second should fail with a ConflictException.
   *
   * @throws Exception
   */
  @Test
  void racingSystemIdAndExternalIdCommitOnlyOne() throws Exception {
    var identifier = "racing-" + UUID.randomUUID();
    var firstDTO = getSaksmappeDTO();
    firstDTO.setSystemId(identifier);
    // Another title, so the slugs don't collide on their unique index instead
    var secondDTO = withTitle("Racing " + identifier);
    secondDTO.setExternalId(identifier);

    var race = raceBehindLock(firstDTO, secondDTO);
    try {
      var thrown =
          assertThrows(ExecutionException.class, () -> race.second().get(30, TimeUnit.SECONDS));
      assertInstanceOf(ConflictException.class, thrown.getCause());
      assertEquals(race.first().getId(), saksmappeService.find(identifier).getId());
      assertNotNull(saksmappeRepository.findBySystemId(identifier));
      assertNull(saksmappeRepository.findByExternalId(identifier));
    } finally {
      deleteBoth(race);
    }
  }

  @Test
  void racingSystemIdLosesToGeneratedSlug() throws Exception {
    var title = "Racing " + UUID.randomUUID();
    var slug = generatedSlug(title);
    var secondDTO = getSaksmappeDTO();
    secondDTO.setSystemId(slug);

    var race = raceBehindLock(withTitle(title), secondDTO);
    try {
      assertEquals(slug, race.first().getSlug());
      var thrown =
          assertThrows(ExecutionException.class, () -> race.second().get(30, TimeUnit.SECONDS));
      assertInstanceOf(ConflictException.class, thrown.getCause());
      assertNotNull(saksmappeRepository.findBySlug(slug));
      assertNull(saksmappeRepository.findBySystemId(slug));
    } finally {
      deleteBoth(race);
    }
  }

  @Test
  void racingGeneratedSlugAvoidsSystemId() throws Exception {
    var title = "Racing " + UUID.randomUUID();
    var slug = generatedSlug(title);
    var firstDTO = getSaksmappeDTO();
    firstDTO.setSystemId(slug);

    var race = raceBehindLock(firstDTO, withTitle(title));
    try {
      var second = race.second().get(30, TimeUnit.SECONDS);
      assertNotEquals(slug, second.getSlug());
      assertTrue(second.getSlug().startsWith(slug + "-"));
      assertEquals(race.first().getId(), saksmappeService.find(slug).getId());
    } finally {
      deleteBoth(race);
    }
  }

  private record Race(SaksmappeDTO first, Future<SaksmappeDTO> second) {}

  /**
   * Adds the first Saksmappe in a transaction that commits once the second, added in another
   * thread, waits for one of its advisory locks. If the second finishes without waiting, the first
   * commits after it.
   */
  private Race raceBehindLock(SaksmappeDTO firstDTO, SaksmappeDTO secondDTO) {
    var securityContext = SecurityContextHolder.getContext();
    var executor = Executors.newSingleThreadExecutor();
    try {
      return new TransactionTemplate(transactionManager)
          .execute(
              status -> {
                try {
                  var first = saksmappeService.add(firstDTO);
                  var pid =
                      entityManager.createNativeQuery("SELECT pg_backend_pid()").getSingleResult();
                  var second =
                      executor.submit(
                          () -> {
                            SecurityContextHolder.setContext(securityContext);
                            return saksmappeService.add(secondDTO);
                          });
                  var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
                  while (!second.isDone() && !isWaitingForAdvisoryLockOf(pid)) {
                    assertTrue(
                        System.nanoTime() < deadline, "Second write neither waited nor ended");
                    Thread.sleep(10);
                  }
                  return new Race(first, second);
                } catch (Exception e) {
                  throw new IllegalStateException(e);
                }
              });
    } finally {
      executor.shutdown();
    }
  }

  /** pg_locks is read live, while pg_stat_activity is snapshotted for the whole transaction. */
  private boolean isWaitingForAdvisoryLockOf(Object pid) {
    return Boolean.TRUE.equals(
        jdbcTemplate.queryForObject(
            """
            SELECT EXISTS (
              SELECT 1 FROM pg_locks
              WHERE locktype = 'advisory' AND NOT granted AND ? = ANY(pg_blocking_pids(pid))
            )
            """,
            Boolean.class,
            pid));
  }

  private void deleteBoth(Race race) throws Exception {
    saksmappeService.delete(race.first().getId());
    try {
      saksmappeService.delete(race.second().get(30, TimeUnit.SECONDS).getId());
    } catch (ExecutionException e) {
      // The second write failed, so there is nothing to delete
    }
  }

  /** The slug a Saksmappe with this title gets, found by adding and deleting one. */
  private String generatedSlug(String title) throws Exception {
    var saksmappe = saksmappeService.add(withTitle(title));
    saksmappeService.delete(saksmappe.getId());
    return saksmappe.getSlug();
  }

  private SaksmappeDTO withTitle(String title) {
    var saksmappeDTO = getSaksmappeDTO();
    saksmappeDTO.setOffentligTittel(title);
    return saksmappeDTO;
  }

  private SaksmappeDTO withNestedJournalpost(String externalId) {
    var journalpostDTO = getJournalpostDTO();
    journalpostDTO.setExternalId(externalId);
    var saksmappeDTO = new SaksmappeDTO();
    saksmappeDTO.setJournalpost(List.of(new ExpandableField<>(journalpostDTO)));
    return saksmappeDTO;
  }
}
