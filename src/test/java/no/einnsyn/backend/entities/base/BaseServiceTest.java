package no.einnsyn.backend.entities.base;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import no.einnsyn.backend.EinnsynServiceTestBase;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.exceptions.models.BadRequestException;
import no.einnsyn.backend.common.exceptions.models.NotFoundException;
import no.einnsyn.backend.entities.saksmappe.SaksmappeService;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class BaseServiceTest extends EinnsynServiceTestBase {

  @Autowired private SaksmappeService saksmappeService;

  /**
   * Test that findOrThrow throws the correct exceptions when the id is not found.
   *
   * @throws Exception
   */
  @Test
  void testFindOrThrow() throws Exception {

    assertThrowsExactly(
        BadRequestException.class, () -> saksmappeService.findOrThrow("foobar"));
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
    assertThrowsExactly(BadRequestException.class, () -> saksmappeService.findOrThrow(new SaksmappeDTO()));
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
}
