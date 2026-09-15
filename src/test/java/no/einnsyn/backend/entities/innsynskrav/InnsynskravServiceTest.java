package no.einnsyn.backend.entities.innsynskrav;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import no.einnsyn.backend.entities.enhet.models.Enhet;
import no.einnsyn.backend.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.backend.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestilling;
import no.einnsyn.backend.entities.journalpost.models.Journalpost;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for InnsynskravService#fromDTO. The InnsynskravBestilling and Journalpost references
 * are pre-set on the Innsynskrav, so the lookups in fromDTO are skipped and no database is needed.
 */
class InnsynskravServiceTest {

  private Innsynskrav newInnsynskravWithJournalpost(Journalpost journalpost) {
    var innsynskrav = new Innsynskrav();
    innsynskrav.setInnsynskravBestilling(new InnsynskravBestilling());
    innsynskrav.setJournalpost(journalpost);
    return innsynskrav;
  }

  /** A Journalpost without an Enhet shouldn't make the whole order fail. */
  @Test
  void testJournalpostWithoutEnhet() {
    var service = new InnsynskravService(null);
    var innsynskrav = newInnsynskravWithJournalpost(new Journalpost());

    assertDoesNotThrow(() -> service.fromDTO(new InnsynskravDTO(), innsynskrav));
    assertNull(innsynskrav.getEnhet());
    assertEquals(1, innsynskrav.getLegacyStatus().size());
  }

  @Test
  void testJournalpostWithAdministrativEnhet() throws Exception {
    var service = new InnsynskravService(null);
    var administrativEnhet = new Enhet();
    var journalpost = new Journalpost();
    journalpost.setAdministrativEnhetObjekt(administrativEnhet);
    var innsynskrav = newInnsynskravWithJournalpost(journalpost);

    service.fromDTO(new InnsynskravDTO(), innsynskrav);
    assertSame(administrativEnhet, innsynskrav.getEnhet());
  }

  /** "avhendetTil" takes precedence over the administrativEnhet. */
  @Test
  void testJournalpostWithAvhendetTil() throws Exception {
    var service = new InnsynskravService(null);
    var avhendetTil = new Enhet();
    var journalpost = new Journalpost();
    journalpost.setAvhendetTil(avhendetTil);
    journalpost.setAdministrativEnhetObjekt(new Enhet());
    var innsynskrav = newInnsynskravWithJournalpost(journalpost);

    service.fromDTO(new InnsynskravDTO(), innsynskrav);
    assertSame(avhendetTil, innsynskrav.getEnhet());
  }
}
