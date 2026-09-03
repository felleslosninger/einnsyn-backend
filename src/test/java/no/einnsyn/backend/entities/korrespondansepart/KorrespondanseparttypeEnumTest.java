package no.einnsyn.backend.entities.korrespondansepart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import no.einnsyn.backend.entities.korrespondansepart.models.KorrespondanseparttypeEnum;
import org.junit.jupiter.api.Test;

class KorrespondanseparttypeEnumTest {

  @Test
  void testFromValue() {
    assertEquals(
        KorrespondanseparttypeEnum.AVSENDER, KorrespondanseparttypeEnum.fromValue("avsender"));
    assertEquals(
        KorrespondanseparttypeEnum.MOTTAKER, KorrespondanseparttypeEnum.fromValue("mottaker"));

    // Lookup is case insensitive
    assertEquals(
        KorrespondanseparttypeEnum.AVSENDER, KorrespondanseparttypeEnum.fromValue("AVSENDER"));

    // Legacy Noark URIs map to their own constants
    assertEquals(
        KorrespondanseparttypeEnum
            .HTTP___WWW_ARKIVVERKET_NO_STANDARDER_NOARK5_ARKIVSTRUKTUR_AVSENDER,
        KorrespondanseparttypeEnum.fromValue(
            "http://www.arkivverket.no/standarder/noark5/arkivstruktur/avsender"));
  }

  @Test
  void testFromValueRoundTrip() {
    // Every constant can be recovered from its own JSON value
    for (var value : KorrespondanseparttypeEnum.values()) {
      assertEquals(value, KorrespondanseparttypeEnum.fromValue(value.toJson()));
      assertEquals(value.toJson(), value.toString());
    }
  }

  @Test
  void testFromValueUnknown() {
    assertThrows(
        IllegalArgumentException.class, () -> KorrespondanseparttypeEnum.fromValue("ukjent"));
  }
}
