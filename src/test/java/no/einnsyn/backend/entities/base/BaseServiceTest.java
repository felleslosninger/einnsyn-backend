package no.einnsyn.backend.entities.base;

import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import no.einnsyn.backend.EinnsynServiceTestBase;
import no.einnsyn.backend.common.exceptions.models.BadRequestException;
import no.einnsyn.backend.common.exceptions.models.NotFoundException;
import no.einnsyn.backend.entities.saksmappe.SaksmappeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class BaseServiceTest extends EinnsynServiceTestBase {

  @Autowired private SaksmappeService saksmappeService;

  /**
   * Test that findByIdOrThrow throws the correct exceptions when the id is not found.
   *
   * @throws Exception
   */
  @Test
  void testFindByIdOrThrow() throws Exception {

    assertThrowsExactly(
        BadRequestException.class, () -> saksmappeService.findByIdOrThrow("foobar"));
    assertThrowsExactly(
        BadRequestException.class,
        () -> saksmappeService.findByIdOrThrow("foobar", BadRequestException.class));
    assertThrowsExactly(
        Exception.class, () -> saksmappeService.findByIdOrThrow("foobar", Exception.class));
    assertThrowsExactly(
        NotFoundException.class,
        () -> saksmappeService.findByIdOrThrow("foobar", NotFoundException.class));
  }
}
