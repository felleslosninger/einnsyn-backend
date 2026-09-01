package no.einnsyn.backend.entities.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

import java.util.List;
import no.einnsyn.backend.EinnsynServiceTestBase;
import no.einnsyn.backend.authentication.AuthenticationService;
import no.einnsyn.backend.authentication.EInnsynAuthentication;
import no.einnsyn.backend.authentication.EInnsynPrincipalEnhet;
import no.einnsyn.backend.common.exceptions.models.BadRequestException;
import no.einnsyn.backend.common.exceptions.models.ConflictException;
import no.einnsyn.backend.common.exceptions.models.NotFoundException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.entities.saksmappe.SaksmappeService;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
public class BaseServiceTest extends EinnsynServiceTestBase {

  @Autowired private AuthenticationService authenticationService;
  @Autowired private SaksmappeService saksmappeService;

  @Mock private EInnsynAuthentication authentication;

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

  /**
   * Test that findOrThrow throws the correct exceptions when the id is not found.
   *
   * @throws Exception
   */
  @Test
  void testFindOrThrow() throws Exception {

    assertThrowsExactly(BadRequestException.class, () -> saksmappeService.findOrThrow("foobar"));
    assertThrowsExactly(
        BadRequestException.class,
        () -> saksmappeService.findOrThrow("foobar", BadRequestException.class));
    assertThrowsExactly(
        Exception.class, () -> saksmappeService.findOrThrow("foobar", Exception.class));
    assertThrowsExactly(
        NotFoundException.class,
        () -> saksmappeService.findOrThrow("foobar", NotFoundException.class));
  }

  @Test
  void testFindOverloads() {
    assertNull(saksmappeService.find((String) null));
    assertNull(saksmappeService.find(new SaksmappeDTO()));
    assertNull(saksmappeService.find(new ExpandableField<>(new SaksmappeDTO())));

    assertThrowsExactly(BadRequestException.class, () -> saksmappeService.findOrThrow("foobar"));
    assertThrowsExactly(
        BadRequestException.class, () -> saksmappeService.findOrThrow(new SaksmappeDTO()));
    assertThrowsExactly(
        BadRequestException.class,
        () -> saksmappeService.findOrThrow(new ExpandableField<>(new SaksmappeDTO())));
  }

  @Test
  @Transactional
  void testCreateOrThrowRejectsDtoWithId() {
    var dto = new SaksmappeDTO();
    dto.setId("sak_foo");
    assertThrowsExactly(BadRequestException.class, () -> saksmappeService.createOrThrow(dto));
  }

  @Test
  @Transactional
  void testCreateOrThrow() throws Exception {
    var dto = getSaksmappeDTO();
    var entity = saksmappeService.createOrThrow(dto);
    assertNotNull(entity);
    assertNotNull(entity.getId());

    // Cleanup
    saksmappeService.delete(entity.getId());
  }

  @Test
  @Transactional
  void testFindOrThrowFindsExistingEntity() throws Exception {
    var created = saksmappeService.createOrThrow(getSaksmappeDTO());
    var id = created.getId();

    // Find by String id
    var found = saksmappeService.findOrThrow(id);
    assertEquals(id, found.getId());

    // Find by DTO with id
    var dto = new SaksmappeDTO();
    dto.setId(id);
    found = saksmappeService.findOrThrow(dto);
    assertEquals(id, found.getId());

    // Find by ExpandableField with id
    found = saksmappeService.findOrThrow(new ExpandableField<>(id));
    assertEquals(id, found.getId());

    // Cleanup
    saksmappeService.delete(id);
  }

  @Test
  @Transactional
  void testFindOrCreateFindsExistingEntity() throws Exception {
    var created = saksmappeService.createOrThrow(getSaksmappeDTO());
    var id = created.getId();

    // Should find the existing entity, not create a new one
    var found = saksmappeService.findOrCreate(new ExpandableField<>(id));
    assertEquals(id, found.getId());

    // Cleanup
    saksmappeService.delete(id);
  }

  @Test
  @Transactional
  void testFindOrCreateCreatesNewEntity() throws Exception {
    var dto = getSaksmappeDTO();
    var entity = saksmappeService.findOrCreate(new ExpandableField<>(dto));
    assertNotNull(entity);
    assertNotNull(entity.getId());

    // Cleanup
    saksmappeService.delete(entity.getId());
  }

  @Test
  @Transactional
  void testFindOrThrowByDtoAndExpandableField() throws Exception {
    var dto = getSaksmappeDTO();
    dto.setExternalId("findOrThrowByDto");
    var created = saksmappeService.createOrThrow(dto);

    // Lookup by a DTO matching on unique fields
    var lookupDTO = new SaksmappeDTO();
    lookupDTO.setExternalId("findOrThrowByDto");
    assertEquals(created.getId(), saksmappeService.findOrThrow(lookupDTO).getId());

    // Lookup by an expandable field without an ID matches on the expanded object
    assertEquals(
        created.getId(), saksmappeService.findOrThrow(new ExpandableField<>(lookupDTO)).getId());

    // A DTO matching nothing throws
    var unknownDTO = new SaksmappeDTO();
    unknownDTO.setExternalId("noSuchExternalId");
    assertThrowsExactly(BadRequestException.class, () -> saksmappeService.findOrThrow(unknownDTO));
    assertThrowsExactly(
        NotFoundException.class,
        () -> saksmappeService.findOrThrow(unknownDTO, NotFoundException.class));

    // A null expandable field throws
    assertThrowsExactly(
        BadRequestException.class,
        () -> saksmappeService.findOrThrow((ExpandableField<SaksmappeDTO>) null));

    // Cleanup
    saksmappeService.delete(created.getId());
  }

  @Test
  @Transactional
  void testFindForUpdateOrThrow() throws Exception {
    var dto = getSaksmappeDTO();
    dto.setExternalId("findForUpdate");
    var created = saksmappeService.createOrThrow(dto);

    // Lookup by ID
    assertEquals(created.getId(), saksmappeService.findForUpdateOrThrow(created.getId()).getId());

    // Lookup by an expandable field with an ID, and by one with only an expanded object
    var lookupDTO = new SaksmappeDTO();
    lookupDTO.setExternalId("findForUpdate");
    assertEquals(
        created.getId(),
        saksmappeService.findForUpdateOrThrow(new ExpandableField<>(created.getId())).getId());
    assertEquals(
        created.getId(),
        saksmappeService.findForUpdateOrThrow(new ExpandableField<>(lookupDTO)).getId());

    // A null expandable field throws
    assertThrowsExactly(
        BadRequestException.class,
        () -> saksmappeService.findForUpdateOrThrow((ExpandableField<SaksmappeDTO>) null));

    // Cleanup
    saksmappeService.delete(created.getId());
  }

  @Test
  @Transactional
  void testCreateOrThrowRejectsExistingId() throws Exception {
    var created = saksmappeService.createOrThrow(getSaksmappeDTO());

    // A DTO with an ID cannot be created again
    var dtoWithId = getSaksmappeDTO();
    dtoWithId.setId(created.getId());
    assertThrowsExactly(BadRequestException.class, () -> saksmappeService.createOrThrow(dtoWithId));

    // Neither can an expandable field carrying an ID
    assertThrowsExactly(
        BadRequestException.class,
        () -> saksmappeService.createOrThrow(new ExpandableField<>(created.getId(), dtoWithId)));

    // Cleanup
    saksmappeService.delete(created.getId());
  }

  @Test
  @Transactional
  void testCreateOrThrowConflict() throws Exception {
    var dto = getSaksmappeDTO();
    dto.setExternalId("duplicateExternalId");
    var created = saksmappeService.createOrThrow(dto);

    // Creating another Saksmappe with the same unique externalId conflicts
    var duplicateDTO = getSaksmappeDTO();
    duplicateDTO.setExternalId("duplicateExternalId");
    assertThrowsExactly(
        ConflictException.class, () -> saksmappeService.createOrThrow(duplicateDTO));

    // Cleanup
    saksmappeService.delete(created.getId());
  }

  @Test
  void testListWithPaginationPivots() throws Exception {
    var first = saksmappeService.add(getSaksmappeDTO());
    var second = saksmappeService.add(getSaksmappeDTO());

    // startingAfter returns only entities after the pivot
    var params = new ListParameters();
    params.setStartingAfter(first.getId());
    params.setSortOrder("asc");
    var page = saksmappeService.list(params);
    assertTrue(page.getItems().stream().anyMatch(s -> s.getId().equals(second.getId())));
    assertTrue(page.getItems().stream().noneMatch(s -> s.getId().equals(first.getId())));

    // endingBefore returns only entities before the pivot
    params = new ListParameters();
    params.setEndingBefore(second.getId());
    params.setSortOrder("asc");
    page = saksmappeService.list(params);
    assertTrue(page.getItems().stream().anyMatch(s -> s.getId().equals(first.getId())));
    assertTrue(page.getItems().stream().noneMatch(s -> s.getId().equals(second.getId())));

    // Cleanup
    saksmappeService.delete(first.getId());
    saksmappeService.delete(second.getId());
  }
}
