package no.einnsyn.backend.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EInnsynAuthorityTest {

  @Test
  void testGetAuthority() {
    var authority = new EInnsynAuthority("Enhet", "enhet_123", "Write");
    assertEquals("ENTITY_ENHET_WRITE_enhet_123", authority.getAuthority());
    assertEquals(authority.getAuthority(), authority.toString());
  }

  @Test
  void testEquals() {
    var authority = new EInnsynAuthority("Enhet", "enhet_123", "Write");

    // Same instance
    assertTrue(authority.equals(authority));

    // Equal values
    assertEquals(authority, new EInnsynAuthority("Enhet", "enhet_123", "Write"));

    // Differing entity, id or access
    assertNotEquals(authority, new EInnsynAuthority("Bruker", "enhet_123", "Write"));
    assertNotEquals(authority, new EInnsynAuthority("Enhet", "enhet_456", "Write"));
    assertNotEquals(authority, new EInnsynAuthority("Enhet", "enhet_123", "Read"));

    // Null and other types
    assertNotEquals(authority, null);
    assertNotEquals(authority, "ENTITY_ENHET_WRITE_enhet_123");
  }

  @Test
  void testHashCode() {
    var authority = new EInnsynAuthority("Enhet", "enhet_123", "Write");

    // Equal objects have equal hash codes
    assertEquals(
        authority.hashCode(), new EInnsynAuthority("Enhet", "enhet_123", "Write").hashCode());
  }
}
