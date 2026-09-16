package no.einnsyn.backend.utils.id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import no.einnsyn.backend.entities.downloadcount.models.DownloadCount;
import org.junit.jupiter.api.Test;

class IdUtilsTest {

  @Test
  void shouldResolveApiSpecEntities() {
    assertEquals("jp", IdUtils.getPrefix("Journalpost"));
    assertEquals("Journalpost", IdUtils.resolveEntity("jp_01jxyz123456789abcdefghij"));
  }

  @Test
  void shouldResolveInternalEntities() {
    // Internal entities are not in the generated IdPrefix map, but must still round-trip so the
    // indexing pipeline can route their ids.
    var id = IdGenerator.generateId(DownloadCount.class);
    assertTrue(id.startsWith("dc_"), id);
    assertEquals("DownloadCount", IdUtils.resolveEntity(id));
    assertEquals("dc", IdUtils.getPrefix("DownloadCount"));
  }

  @Test
  void internalPrefixesShouldNotBeValidApiIds() {
    // IdValidator guards the API boundary, where internal entities never appear.
    assertFalse(IdValidator.isValid(IdGenerator.generateId(DownloadCount.class)));
    assertTrue(IdValidator.isValid(IdGenerator.generateId("Journalpost")));
  }
}
