package no.einnsyn.backend;

import java.time.LocalDate;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.korrespondansepart.models.KorrespondansepartDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;

public abstract class EinnsynServiceTestBase extends EinnsynTestBase {

  protected SaksmappeDTO getSaksmappeDTO() {
    var saksmappeDTO = new SaksmappeDTO();
    saksmappeDTO.setOffentligTittel("test 1");
    saksmappeDTO.setOffentligTittelSensitiv("test 1 sensitiv");
    saksmappeDTO.setBeskrivelse("test 1 beskrivelse");
    saksmappeDTO.setSaksaar(2023);
    saksmappeDTO.setSakssekvensnummer(1);
    return saksmappeDTO;
  }

  protected JournalpostDTO getJournalpostDTO() {
    var journalpostDTO = new JournalpostDTO();
    journalpostDTO.setOffentligTittel("test 1");
    journalpostDTO.setOffentligTittelSensitiv("test 1 sensitiv");
    journalpostDTO.setJournalaar(2023);
    journalpostDTO.setJournalsekvensnummer(1);
    journalpostDTO.setJournalposttype("innkommende_dokument");
    journalpostDTO.setJournaldato(LocalDate.of(2023, 1, 2).toString());
    journalpostDTO.setJournalpostnummer(1);
    journalpostDTO.setDokumentetsDato("2023-01-02");
    return journalpostDTO;
  }

  protected KorrespondansepartDTO getKorrespondansepartDTO() {
    var korrespondansepartDTO = new KorrespondansepartDTO();
    korrespondansepartDTO.setKorrespondansepartNavn("test 1");
    korrespondansepartDTO.setKorrespondansepartNavnSensitiv("test 1 sensitiv");
    korrespondansepartDTO.setKorrespondanseparttype("mottaker");
    korrespondansepartDTO.setEpostadresse("epost1@example.com");
    korrespondansepartDTO.setAdministrativEnhet("https://testAdmEnhet1");
    return korrespondansepartDTO;
  }

  protected DokumentbeskrivelseDTO getDokumentbeskrivelseDTO() {
    var dokumentbeskrivelseDTO = new DokumentbeskrivelseDTO();
    dokumentbeskrivelseDTO.setTittel("dokumentbeskrivelsetest 1");
    dokumentbeskrivelseDTO.setTittelSensitiv("dokumentbeskrivelsetest 1 sensitiv");
    dokumentbeskrivelseDTO.setTilknyttetRegistreringSom("vedlegg");
    return dokumentbeskrivelseDTO;
  }
}
