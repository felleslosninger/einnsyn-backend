package no.einnsyn.apiv3.entities;

import java.time.LocalDate;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseJSON;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostJSON;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartJSON;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeJSON;

public abstract class EinnsynServiceTestBase extends EinnsynTestBase {

  protected SaksmappeJSON getSaksmappeJSON() {
    SaksmappeJSON saksmappeJSON = new SaksmappeJSON();
    saksmappeJSON = new SaksmappeJSON();
    saksmappeJSON.setOffentligTittel("test 1");
    saksmappeJSON.setOffentligTittelSensitiv("test 1 sensitiv");
    saksmappeJSON.setBeskrivelse("test 1 beskrivelse");
    saksmappeJSON.setSaksaar(2023);
    saksmappeJSON.setSakssekvensnummer(1);
    return saksmappeJSON;
  }

  protected JournalpostJSON getJournalpostJSON() {
    JournalpostJSON journalpostJSON = new JournalpostJSON();
    journalpostJSON.setOffentligTittel("test 1");
    journalpostJSON.setOffentligTittelSensitiv("test 1 sensitiv");
    journalpostJSON.setJournalaar(2023);
    journalpostJSON.setJournalsekvensnummer(1);
    journalpostJSON.setJournalposttype("innkommendeDokument");
    journalpostJSON.setJournaldato(LocalDate.of(2023, 1, 2));
    journalpostJSON.setJournalpostnummer(1);
    return journalpostJSON;
  }

  protected KorrespondansepartJSON getKorrespondanseparJSON() {
    KorrespondansepartJSON korrespondansepartJSON = new KorrespondansepartJSON();
    korrespondansepartJSON.setKorrespondansepartNavn("test 1");
    korrespondansepartJSON.setKorrespondansepartNavnSensitiv("test 1 sensitiv");
    korrespondansepartJSON.setKorrespondanseparttype("mottaker");
    return korrespondansepartJSON;
  }

  protected DokumentbeskrivelseJSON getDokumentbeskrivelseJSON() {
    DokumentbeskrivelseJSON dokumentbeskrivelseJSON = new DokumentbeskrivelseJSON();
    dokumentbeskrivelseJSON.setTittel("dokumentbeskrivelsetest 1");
    dokumentbeskrivelseJSON.setTittelSensitiv("dokumentbeskrivelsetest 1 sensitiv");
    dokumentbeskrivelseJSON.setDokumenttype("utgåendeDokument");
    return dokumentbeskrivelseJSON;
  }
}
