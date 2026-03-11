package no.einnsyn.backend.entities.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Mockito.doReturn;

import java.util.List;
import no.einnsyn.backend.EinnsynServiceTestBase;
import no.einnsyn.backend.authentication.AuthenticationService;
import no.einnsyn.backend.authentication.EInnsynAuthentication;
import no.einnsyn.backend.authentication.EInnsynPrincipalEnhet;
import no.einnsyn.backend.common.exceptions.models.BadRequestException;
import no.einnsyn.backend.common.exceptions.models.NotFoundException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.saksmappe.SaksmappeService;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

public class BaseServiceTest extends EinnsynServiceTestBase {

  @Autowired private AuthenticationService authenticationService;
  @Autowired private SaksmappeService saksmappeService;

  @Mock EInnsynAuthentication authentication;
  @Mock SecurityContext securityContext;

  @BeforeAll
  void setupMock() {
    var apiKey = apiKeyService.findBySecretKey(adminKey);
    var enhetId = apiKey.getEnhet().getId();
    var enhetOrgno = apiKey.getEnhet().getOrgnummer();
    var principal = new EInnsynPrincipalEnhet("ApiKey", apiKey.getId(), enhetId, enhetOrgno, false);
    doReturn(principal).when(authentication).getPrincipal();

    var authorities =
        authenticationService.getAuthoritiesFromEnhet(List.of(apiKey.getEnhet()), "Write");
    doReturn(authorities).when(authentication).getAuthorities();

    doReturn(authentication).when(securityContext).getAuthentication();
    SecurityContextHolder.setContext(securityContext);
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
}
