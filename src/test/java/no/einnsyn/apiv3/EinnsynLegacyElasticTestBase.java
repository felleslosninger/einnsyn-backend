package no.einnsyn.apiv3;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseES;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.DokumentobjektDTO;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.DokumentobjektES;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostES;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartES;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeES;
import no.einnsyn.apiv3.entities.skjerming.models.SkjermingDTO;
import no.einnsyn.apiv3.entities.skjerming.models.SkjermingES;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;

public class EinnsynLegacyElasticTestBase extends EinnsynServiceTestBase {

  protected void compareJournalpost(JournalpostDTO journalpostDTO, JournalpostES journalpostES)
      throws EInnsynException {

    // BaseES
    if (journalpostDTO.getExternalId() != null) {
      assertEquals(journalpostDTO.getExternalId(), journalpostES.getId());
    } else {
      assertEquals(journalpostDTO.getId(), journalpostES.getId());
    }
    assertEquals(List.of("Journalpost"), journalpostES.getType());
    assertEquals(journalpostDTO.getJournalposttype(), journalpostES.getSorteringstype());

    // ArkivBaseES
    var saksmappe = saksmappeService.findById(journalpostDTO.getSaksmappe().getId());
    var saksmappeDTO = saksmappeService.get(saksmappe.getId());
    var administrativEnhetId = saksmappe.getAdministrativEnhetObjekt().getId();
    var administrativEnhetDTO = enhetService.findById(administrativEnhetId);
    var transitive = enhetService.getTransitiveEnhets(administrativEnhetId);
    assertEquals(administrativEnhetDTO.getIri(), journalpostES.getArkivskaper());
    assertEquals(administrativEnhetDTO.getNavn(), journalpostES.getArkivskaperSorteringNavn());
    assertEquals(
        transitive.stream().map(e -> e.getNavn()).toList(), journalpostES.getArkivskaperNavn());
    assertEquals(
        transitive.stream().map(e -> e.getIri()).toList(),
        journalpostES.getArkivskaperTransitive());

    // RegistreringES
    assertEquals(journalpostDTO.getOffentligTittel(), journalpostES.getOffentligTittel());
    assertEquals(
        journalpostDTO.getOffentligTittelSensitiv(), journalpostES.getOffentligTittel_SENSITIV());
    assertEquals(journalpostDTO.getPublisertDato(), journalpostES.getPublisertDato());

    // JournalpostES
    assertEquals(List.of("Journalpost"), journalpostES.getType());
    assertEquals(journalpostDTO.getJournaldato(), journalpostES.getJournaldato());
    assertEquals(journalpostDTO.getDokumentetsDato(), journalpostES.getDokumentetsDato());
    assertEquals(journalpostDTO.getJournalpostnummer() + "", journalpostES.getJournalpostnummer());
    assertEquals(journalpostDTO.getJournalposttype(), journalpostES.getJournalposttype());
    assertEquals(journalpostDTO.getJournalaar() + "", journalpostES.getJournalaar());
    assertEquals(
        journalpostDTO.getJournalsekvensnummer() + "", journalpostES.getJournalsekvensnummer());
    assertEquals(journalpostDTO.getJournalposttype(), journalpostES.getSorteringstype());
    var saksaar = saksmappe.getSaksaar() + "";
    var saksaarShort = saksaar.substring(2);
    var sakssekvensnummer = saksmappe.getSakssekvensnummer() + "";
    assertEquals(
        List.of(
            saksaar + "/" + sakssekvensnummer,
            saksaarShort + "/" + sakssekvensnummer,
            sakssekvensnummer + "/" + saksaar,
            sakssekvensnummer + "/" + saksaarShort),
        journalpostES.getSaksnummerGenerert());

    // Journalpost.Korrespondansepart
    if (journalpostDTO.getKorrespondansepart() != null) {
      assertEquals(
          journalpostDTO.getKorrespondansepart().size(),
          journalpostES.getKorrespondansepart().size());
      for (int i = 0; i < journalpostDTO.getKorrespondansepart().size(); i++) {
        var korrespondansepartDTO =
            journalpostDTO.getKorrespondansepart().get(i).getExpandedObject();
        var korrespondansepartES = journalpostES.getKorrespondansepart().get(i);
        compareKorrespondansepart(korrespondansepartDTO, korrespondansepartES);
      }
    }

    // Journalpost.Skjerming
    if (journalpostDTO.getSkjerming() != null) {
      var skjermingDTO = journalpostDTO.getSkjerming().getExpandedObject();
      var skjermingES = journalpostES.getSkjerming();
      compareSkjerming(skjermingDTO, skjermingES);
    }

    // Journalpost.parent
    compareSaksmappe(saksmappeDTO, journalpostES.getParent());

    // Journalpost.dokumentbeskrivelse
    if (journalpostDTO.getDokumentbeskrivelse() != null
        && journalpostDTO.getDokumentbeskrivelse().size() > 0) {
      assertEquals(
          journalpostDTO.getDokumentbeskrivelse().size(),
          journalpostES.getDokumentbeskrivelse().size());
      for (int i = 0; i < journalpostDTO.getDokumentbeskrivelse().size(); i++) {
        var dokumentbeskrivelseDTO =
            journalpostDTO.getDokumentbeskrivelse().get(i).getExpandedObject();
        var dokumentbeskrivelseES = journalpostES.getDokumentbeskrivelse().get(i);
        compareDokumentbeskrivelse(dokumentbeskrivelseDTO, dokumentbeskrivelseES);
      }
    }
  }

  protected void compareSaksmappe(SaksmappeDTO saksmappeDTO, SaksmappeES saksmappeES)
      throws EInnsynException {
    // BaseES
    if (saksmappeDTO.getExternalId() != null) {
      assertEquals(saksmappeDTO.getExternalId(), saksmappeES.getId());
    } else {
      assertEquals(saksmappeDTO.getId(), saksmappeES.getId());
    }
    assertEquals(List.of("Saksmappe"), saksmappeES.getType());
    assertEquals("sak", saksmappeES.getSorteringstype());

    // ArkivBaseES
    var saksmappe = saksmappeService.findById(saksmappeDTO.getId());
    var administrativEnhetId = saksmappe.getAdministrativEnhetObjekt().getId();
    var administrativEnhetDTO = enhetService.findById(administrativEnhetId);
    var transitive = enhetService.getTransitiveEnhets(administrativEnhetId);
    assertEquals(administrativEnhetDTO.getIri(), saksmappeES.getArkivskaper());
    assertEquals(administrativEnhetDTO.getNavn(), saksmappeES.getArkivskaperSorteringNavn());
    assertEquals(
        transitive.stream().map(e -> e.getNavn()).toList(), saksmappeES.getArkivskaperNavn());
    assertEquals(
        transitive.stream().map(e -> e.getIri()).toList(), saksmappeES.getArkivskaperTransitive());

    // MappeES
    assertEquals(saksmappeDTO.getOffentligTittel(), saksmappeES.getOffentligTittel());
    assertEquals(
        saksmappeDTO.getOffentligTittelSensitiv(), saksmappeES.getOffentligTittel_SENSITIV());
    assertEquals(saksmappeDTO.getPublisertDato(), saksmappeES.getPublisertDato());

    // SaksmappeES
    assertEquals(saksmappeDTO.getSaksaar() + "", saksmappeES.getSaksaar());
    assertEquals(saksmappeDTO.getSakssekvensnummer() + "", saksmappeES.getSakssekvensnummer());
    assertEquals(saksmappeDTO.getSaksnummer(), saksmappeES.getSaksnummer());
    var saksaar = saksmappe.getSaksaar() + "";
    var saksaarShort = saksaar.substring(2);
    var sakssekvensnummer = saksmappe.getSakssekvensnummer() + "";
    assertEquals(
        List.of(
            saksaar + "/" + sakssekvensnummer,
            saksaarShort + "/" + sakssekvensnummer,
            sakssekvensnummer + "/" + saksaar,
            sakssekvensnummer + "/" + saksaarShort),
        saksmappeES.getSaksnummerGenerert());

    // SaksmappeES.child (this can be null in saksmappeES, when the object is gotten as
    // journalpost.parent)
    if (saksmappeDTO.getJournalpost() != null && saksmappeES.getChild() != null) {
      assertEquals(saksmappeDTO.getJournalpost().size(), saksmappeES.getChild().size());
      for (int i = 0; i < saksmappeDTO.getJournalpost().size(); i++) {
        var journalpostDTO =
            (JournalpostDTO) saksmappeDTO.getJournalpost().get(i).getExpandedObject();
        var journalpostES = (JournalpostES) saksmappeES.getChild().get(i);
        compareJournalpost(journalpostDTO, journalpostES);
      }
    }
  }

  protected void compareSkjerming(SkjermingDTO skjermingDTO, SkjermingES skjermingES) {
    assertEquals(List.of("Skjerming"), skjermingES.getType());
    assertEquals(skjermingDTO.getSkjermingshjemmel(), skjermingES.getSkjermingshjemmel());
    assertEquals(skjermingDTO.getTilgangsrestriksjon(), skjermingES.getTilgangsrestriksjon());
  }

  protected void compareKorrespondansepart(
      KorrespondansepartDTO korrespondansepartDTO, KorrespondansepartES korrespondansepartES) {
    assertEquals(List.of("Korrespondansepart"), korrespondansepartES.getType());
    assertEquals(
        korrespondansepartDTO.getKorrespondansepartNavn(),
        korrespondansepartES.getKorrespondansepartNavn());
    assertEquals(
        korrespondansepartDTO.getKorrespondansepartNavnSensitiv(),
        korrespondansepartES.getKorrespondansepartNavn_SENSITIV());
    assertEquals(
        korrespondansepartDTO.getKorrespondanseparttype(),
        korrespondansepartES.getKorrespondanseparttype());
    assertEquals(
        korrespondansepartDTO.getAdministrativEnhet(),
        korrespondansepartES.getAdministrativEnhet());
    assertEquals(
        korrespondansepartDTO.getErBehandlingsansvarlig(),
        korrespondansepartES.isErBehandlingsansvarlig());
  }

  protected void compareDokumentbeskrivelse(
      DokumentbeskrivelseDTO dokumentbeskrivelseDTO, DokumentbeskrivelseES dokumentbeskrivelseES) {
    assertEquals(List.of("Dokumentbeskrivelse"), dokumentbeskrivelseES.getType());
    assertEquals(dokumentbeskrivelseDTO.getTittel(), dokumentbeskrivelseES.getTittel());
    assertEquals(
        dokumentbeskrivelseDTO.getTittelSensitiv(), dokumentbeskrivelseES.getTittel_SENSITIV());
    assertEquals(
        dokumentbeskrivelseDTO.getTilknyttetRegistreringSom(),
        dokumentbeskrivelseES.getTilknyttetRegistreringSom());
    assertEquals(dokumentbeskrivelseDTO.getDokumenttype(), dokumentbeskrivelseES.getDokumenttype());
    if (dokumentbeskrivelseDTO.getDokumentobjekt() != null) {
      for (int i = 0; i < dokumentbeskrivelseDTO.getDokumentobjekt().size(); i++) {
        var dokumentobjektDTO =
            dokumentbeskrivelseDTO.getDokumentobjekt().get(i).getExpandedObject();
        var dokumentobjektES = dokumentbeskrivelseES.getDokumentobjekt().get(i);
        compareDokumentobjekt(dokumentobjektDTO, dokumentobjektES);
      }
    }
  }

  protected void compareDokumentobjekt(
      DokumentobjektDTO dokumentobjektDTO, DokumentobjektES dokumentobjektES) {
    assertEquals(List.of("Dokumentobjekt"), dokumentobjektES.getType());
    assertEquals(dokumentobjektDTO.getFormat(), dokumentobjektES.getFormat());
    assertEquals(
        dokumentobjektDTO.getReferanseDokumentfil(), dokumentobjektES.getReferanseDokumentfil());
  }
}
