package no.einnsyn.apiv3.entities;

import java.time.LocalDate;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;

public abstract class EinnsynServiceTestBase extends EinnsynTestBase {

  protected SaksmappeDTO getSaksmappeDTO() {
    var SaksmappeDTO = new SaksmappeDTO();
    SaksmappeDTO = new SaksmappeDTO();
    SaksmappeDTO.setOffentligTittel("test 1");
    SaksmappeDTO.setOffentligTittelSensitiv("test 1 sensitiv");
    SaksmappeDTO.setBeskrivelse("test 1 beskrivelse");
    SaksmappeDTO.setSaksaar(2023);
    SaksmappeDTO.setSakssekvensnummer(1);
    return SaksmappeDTO;
  }


  protected JournalpostDTO getJournalpostDTO() {
    var journalpostJSON = new JournalpostDTO();
    journalpostJSON.setOffentligTittel("test 1");
    journalpostJSON.setOffentligTittelSensitiv("test 1 sensitiv");
    journalpostJSON.setJournalaar(2023);
    journalpostJSON.setJournalsekvensnummer(1);
    journalpostJSON.setJournalposttype("innkommendeDokument");
    journalpostJSON.setJournaldato(LocalDate.of(2023, 1, 2).toString());
    journalpostJSON.setJournalpostnummer(1);
    return journalpostJSON;
  }


  protected KorrespondansepartDTO getKorrespondanseparJSON() {
    var KorrespondansepartDTO = new KorrespondansepartDTO();
    KorrespondansepartDTO.setKorrespondansepartNavn("test 1");
    KorrespondansepartDTO.setKorrespondansepartNavnSensitiv("test 1 sensitiv");
    KorrespondansepartDTO.setKorrespondanseparttype("mottaker");
    return KorrespondansepartDTO;
  }


  protected DokumentbeskrivelseDTO getDokumentbeskrivelseDTO() {
    var dokumentbeskrivelseJSON = new DokumentbeskrivelseDTO();
    dokumentbeskrivelseJSON.setTittel("dokumentbeskrivelsetest 1");
    dokumentbeskrivelseJSON.setTittelSensitiv("dokumentbeskrivelsetest 1 sensitiv");
    dokumentbeskrivelseJSON.setDokumenttype("utgåendeDokument");
    return dokumentbeskrivelseJSON;
  }

}
