package no.einnsyn.apiv3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import no.einnsyn.apiv3.entities.base.models.BaseES;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseES;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.DokumentobjektDTO;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.DokumentobjektES;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostES;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartES;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondanseparttypeResolver;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentES;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeES;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakES;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeES;
import no.einnsyn.apiv3.entities.skjerming.models.SkjermingDTO;
import no.einnsyn.apiv3.entities.skjerming.models.SkjermingES;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@SuppressWarnings({"unchecked"})
public class EinnsynLegacyElasticTestBase extends EinnsynControllerTestBase {

  @Mock IndexRequest.Builder<BaseES> indexBuilderMock;
  @Mock DeleteRequest.Builder deleteBuilderMock;

  protected void resetEs() throws Exception {
    // Wait for unfinished Async calls
    Thread.sleep(50);
    reset(esClient);
  }

  private void resetIndexBuilderMock() {
    reset(indexBuilderMock);
    when(indexBuilderMock.index(anyString())).thenReturn(indexBuilderMock);
    when(indexBuilderMock.id(anyString())).thenReturn(indexBuilderMock);
    when(indexBuilderMock.document(any())).thenReturn(indexBuilderMock);
  }

  private void resetDeleteBuilderMock() {
    reset(deleteBuilderMock);
    when(deleteBuilderMock.index(anyString())).thenReturn(deleteBuilderMock);
    when(deleteBuilderMock.id(anyString())).thenReturn(deleteBuilderMock);
  }

  protected Map<String, BaseES> captureIndexedDocuments(int times) throws Exception {
    // Indexing is done in `@Async`, so we have to delay the capture
    Thread.sleep(50);

    var builderCaptor = ArgumentCaptor.forClass(Function.class);
    verify(esClient, times(times)).index(builderCaptor.capture());
    var builders = builderCaptor.getAllValues();
    var map = new HashMap<String, BaseES>();

    for (int i = 0; i < times; i++) {
      resetIndexBuilderMock();
      var documentCaptor = ArgumentCaptor.forClass(BaseES.class);
      var idCaptor = ArgumentCaptor.forClass(String.class);
      var builder = builders.get(i);
      builder.apply(indexBuilderMock);
      verify(indexBuilderMock).id(idCaptor.capture());
      verify(indexBuilderMock).document(documentCaptor.capture());
      var id = idCaptor.getValue();
      var document = documentCaptor.getValue();
      map.put(id, document);
    }

    return map;
  }

  protected Set<String> captureDeletedDocuments(int times) throws Exception {
    // Deleting is done in `@Async`, so we have to delay the capture
    Thread.sleep(50);

    var builderCaptor = ArgumentCaptor.forClass(Function.class);
    verify(esClient, times(times)).delete(builderCaptor.capture());
    var builders = builderCaptor.getAllValues();
    var set = new HashSet<String>();

    for (int i = 0; i < times; i++) {
      resetDeleteBuilderMock();
      var idCaptor = ArgumentCaptor.forClass(String.class);
      var builder = builders.get(i);
      builder.apply(deleteBuilderMock);
      verify(deleteBuilderMock).id(idCaptor.capture());
      var id = idCaptor.getValue();
      set.add(id);
    }

    return set;
  }

  protected void compareJournalpost(JournalpostDTO journalpostDTO, JournalpostES journalpostES)
      throws EInnsynException {

    // BaseES
    assertEquals(journalpostDTO.getId(), journalpostES.getId());
    assertEquals(journalpostDTO.getExternalId(), journalpostES.getExternalId());
    assertEquals(List.of("Journalpost"), journalpostES.getType());
    assertEquals(journalpostDTO.getJournalposttype(), journalpostES.getSorteringstype());

    // ArkivBaseES
    var saksmappe = saksmappeService.findById(journalpostDTO.getSaksmappe().getId());
    var saksmappeDTO = saksmappeService.get(saksmappe.getId());
    var administrativEnhetId = journalpostDTO.getAdministrativEnhetObjekt().getId();
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
    assertNotNull(journalpostDTO.getPublisertDato());
    assertEqualInstants(journalpostDTO.getPublisertDato(), journalpostES.getPublisertDato());
    assertNotNull(journalpostDTO.getOppdatertDato());
    assertEqualInstants(journalpostDTO.getOppdatertDato(), journalpostES.getOppdatertDato());

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
            saksaar + "/" + sakssekvensnummer + "-" + journalpostDTO.getJournalpostnummer(),
            saksaarShort + "/" + sakssekvensnummer + "-" + journalpostDTO.getJournalpostnummer(),
            sakssekvensnummer + "/" + saksaar + "-" + journalpostDTO.getJournalpostnummer(),
            sakssekvensnummer + "/" + saksaarShort + "-" + journalpostDTO.getJournalpostnummer()),
        journalpostES.getSaksnummerGenerert());

    // Journalpost.Korrespondansepart
    if (journalpostDTO.getKorrespondansepart() != null
        && journalpostDTO.getKorrespondansepart().size() > 0) {
      assertEquals(
          journalpostDTO.getKorrespondansepart().size(),
          journalpostES.getKorrespondansepart().size());
      for (int i = 0; i < journalpostDTO.getKorrespondansepart().size(); i++) {
        var korrespondansepartField = journalpostDTO.getKorrespondansepart().get(i);
        var korrespondansepartDTO = korrespondansepartField.getExpandedObject();
        var korrespondansepartES = journalpostES.getKorrespondansepart().get(i);
        if (korrespondansepartDTO == null) {
          korrespondansepartDTO = korrespondansepartService.get(korrespondansepartField.getId());
        }
        compareKorrespondansepart(korrespondansepartDTO, korrespondansepartES);
      }
    }

    // Journalpost.Skjerming
    if (journalpostDTO.getSkjerming() != null) {
      var skjermingField = journalpostDTO.getSkjerming();
      var skjermingDTO = skjermingField.getExpandedObject();
      var skjermingES = journalpostES.getSkjerming();
      if (skjermingDTO == null) {
        skjermingDTO = skjermingService.get(skjermingField.getId());
      }
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
        var dokumentbeskrivelseField = journalpostDTO.getDokumentbeskrivelse().get(i);
        var dokumentbeskrivelseDTO = dokumentbeskrivelseField.getExpandedObject();
        var dokumentbeskrivelseES = journalpostES.getDokumentbeskrivelse().get(i);
        if (dokumentbeskrivelseDTO == null) {
          dokumentbeskrivelseDTO = dokumentbeskrivelseService.get(dokumentbeskrivelseField.getId());
        }
        compareDokumentbeskrivelse(dokumentbeskrivelseDTO, dokumentbeskrivelseES);
      }
    }
  }

  protected void compareSaksmappe(SaksmappeDTO saksmappeDTO, SaksmappeES saksmappeES)
      throws EInnsynException {
    // BaseES
    assertEquals(saksmappeDTO.getId(), saksmappeES.getId());
    assertEquals(saksmappeDTO.getExternalId(), saksmappeES.getExternalId());
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
    assertNotNull(saksmappeDTO.getPublisertDato());
    assertEqualInstants(saksmappeDTO.getPublisertDato(), saksmappeES.getPublisertDato());
    assertNotNull(saksmappeDTO.getOppdatertDato());
    assertEqualInstants(saksmappeDTO.getOppdatertDato(), saksmappeES.getOppdatertDato());

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
  }

  protected void compareSkjerming(SkjermingDTO skjermingDTO, SkjermingES skjermingES) {
    assertEquals(skjermingDTO.getId(), skjermingES.getId());
    assertEquals(skjermingDTO.getExternalId(), skjermingES.getExternalId());
    assertEquals(List.of("Skjerming"), skjermingES.getType());
    assertEquals(skjermingDTO.getSkjermingshjemmel(), skjermingES.getSkjermingshjemmel());
    assertEquals(skjermingDTO.getTilgangsrestriksjon(), skjermingES.getTilgangsrestriksjon());
  }

  protected void compareKorrespondansepart(
      KorrespondansepartDTO korrespondansepartDTO, KorrespondansepartES korrespondansepartES) {
    assertEquals(korrespondansepartDTO.getId(), korrespondansepartES.getId());
    assertEquals(korrespondansepartDTO.getExternalId(), korrespondansepartES.getExternalId());
    assertEquals(List.of("Korrespondansepart"), korrespondansepartES.getType());

    assertEquals(
        korrespondansepartDTO.getKorrespondansepartNavn(),
        korrespondansepartES.getKorrespondansepartNavn());
    assertEquals(
        korrespondansepartDTO.getKorrespondansepartNavnSensitiv(),
        korrespondansepartES.getKorrespondansepartNavn_SENSITIV());
    assertEquals(
        KorrespondanseparttypeResolver.BASE + korrespondansepartDTO.getKorrespondanseparttype(),
        korrespondansepartES.getKorrespondanseparttype());
    assertEquals(
        korrespondansepartDTO.getAdministrativEnhet(),
        korrespondansepartES.getAdministrativEnhet());
    assertEquals(
        korrespondansepartDTO.getErBehandlingsansvarlig(),
        korrespondansepartES.isErBehandlingsansvarlig());
  }

  protected void compareDokumentbeskrivelse(
      DokumentbeskrivelseDTO dokumentbeskrivelseDTO, DokumentbeskrivelseES dokumentbeskrivelseES)
      throws EInnsynException {
    assertEquals(dokumentbeskrivelseDTO.getId(), dokumentbeskrivelseES.getId());
    assertEquals(dokumentbeskrivelseDTO.getExternalId(), dokumentbeskrivelseES.getExternalId());
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
        var dokumentobjektField = dokumentbeskrivelseDTO.getDokumentobjekt().get(i);
        var dokumentobjektDTO = dokumentobjektField.getExpandedObject();
        var dokumentobjektES = dokumentbeskrivelseES.getDokumentobjekt().get(i);
        if (dokumentobjektDTO == null) {
          dokumentobjektDTO = dokumentobjektService.get(dokumentobjektField.getId());
        }
        compareDokumentobjekt(dokumentobjektDTO, dokumentobjektES);
      }
    }
  }

  protected void compareDokumentobjekt(
      DokumentobjektDTO dokumentobjektDTO, DokumentobjektES dokumentobjektES) {
    assertEquals(dokumentobjektDTO.getId(), dokumentobjektES.getId());
    assertEquals(dokumentobjektDTO.getExternalId(), dokumentobjektES.getExternalId());
    assertEquals(List.of("Dokumentobjekt"), dokumentobjektES.getType());
    assertEquals(dokumentobjektDTO.getFormat(), dokumentobjektES.getFormat());
    assertEquals(
        dokumentobjektDTO.getReferanseDokumentfil(), dokumentobjektES.getReferanseDokumentfil());
  }

  protected void compareMoetemappe(MoetemappeDTO moetemappeDTO, MoetemappeES moetemappeES)
      throws Exception {
    // BaseES
    assertEquals(moetemappeDTO.getId(), moetemappeES.getId());
    assertEquals(moetemappeDTO.getExternalId(), moetemappeES.getExternalId());
    assertEquals(List.of("Moetemappe"), moetemappeES.getType());
    assertEquals("politisk møte", moetemappeES.getSorteringstype());

    // ArkivBaseES
    var moetemappe = moetemappeService.findById(moetemappeDTO.getId());
    var administrativEnhetId = moetemappe.getUtvalgObjekt().getId();
    var administrativEnhetDTO = enhetService.findById(administrativEnhetId);
    var transitive = enhetService.getTransitiveEnhets(administrativEnhetId);
    assertEquals(administrativEnhetDTO.getIri(), moetemappeES.getArkivskaper());
    assertEquals(administrativEnhetDTO.getNavn(), moetemappeES.getArkivskaperSorteringNavn());
    assertEquals(
        transitive.stream().map(e -> e.getNavn()).toList(), moetemappeES.getArkivskaperNavn());
    assertEquals(
        transitive.stream().map(e -> e.getIri()).toList(), moetemappeES.getArkivskaperTransitive());

    // MappeES
    assertEquals(moetemappeDTO.getOffentligTittel(), moetemappeES.getOffentligTittel());
    assertEquals(
        moetemappeDTO.getOffentligTittelSensitiv(), moetemappeES.getOffentligTittel_SENSITIV());
    assertNotNull(moetemappeDTO.getPublisertDato());
    assertEqualInstants(moetemappeDTO.getPublisertDato(), moetemappeES.getPublisertDato());
    assertNotNull(moetemappeDTO.getOppdatertDato());
    assertEqualInstants(moetemappeDTO.getOppdatertDato(), moetemappeES.getOppdatertDato());

    // MoetemappeES
    assertEquals(moetemappeDTO.getUtvalg(), moetemappeES.getUtvalg());
    assertEquals(moetemappeDTO.getMoetested(), moetemappeES.getMoetested());
    assertEquals(moetemappeDTO.getMoetedato(), moetemappeES.getMoetedato());
    assertNotNull(moetemappeES.getStandardDato());

    // MoetemappeES.child
    if (moetemappeDTO.getMoetedokument() != null && moetemappeES.getChild() != null) {
      assertEquals(moetemappeDTO.getMoetedokument().size(), moetemappeES.getChild().size());
      for (int i = 0; i < moetemappeDTO.getMoetedokument().size(); i++) {
        var moetedokumentField = moetemappeDTO.getMoetedokument().get(i);
        var moetedokumentDTO = moetedokumentField.getExpandedObject();
        var moetedokumentES = (MoetedokumentES) moetemappeES.getChild().get(i);
        if (moetedokumentDTO == null) {
          moetedokumentDTO = moetedokumentService.get(moetedokumentField.getId());
        }
        compareMoetedokument(moetedokumentDTO, moetedokumentES);
      }
    }
  }

  protected void compareMoetedokument(
      MoetedokumentDTO moetedokumentDTO, MoetedokumentES moetedokumentES) throws Exception {
    // BaseES
    assertEquals(moetedokumentDTO.getId(), moetedokumentES.getId());
    assertEquals(moetedokumentDTO.getExternalId(), moetedokumentES.getExternalId());
    assertEquals(List.of("Møtedokumentregistrering"), moetedokumentES.getType());

    // ArkivBaseES
    var moetemappe = moetemappeService.findById(moetedokumentDTO.getMoetemappe().getId());
    var administrativEnhetId = moetemappe.getUtvalgObjekt().getId();
    var administrativEnhetDTO = enhetService.findById(administrativEnhetId);
    var transitive = enhetService.getTransitiveEnhets(administrativEnhetId);
    assertEquals(administrativEnhetDTO.getIri(), moetedokumentES.getArkivskaper());
    assertEquals(administrativEnhetDTO.getNavn(), moetedokumentES.getArkivskaperSorteringNavn());
    assertEquals(
        transitive.stream().map(Enhet::getNavn).toList(), moetedokumentES.getArkivskaperNavn());
    assertEquals(
        transitive.stream().map(Enhet::getIri).toList(),
        moetedokumentES.getArkivskaperTransitive());

    // RegistreringES
    assertEquals(moetedokumentDTO.getOffentligTittel(), moetedokumentES.getOffentligTittel());
    assertEquals(
        moetedokumentDTO.getOffentligTittelSensitiv(),
        moetedokumentES.getOffentligTittel_SENSITIV());
    assertNotNull(moetedokumentDTO.getPublisertDato());
    assertEqualInstants(moetedokumentDTO.getPublisertDato(), moetedokumentES.getPublisertDato());
    assertNotNull(moetedokumentDTO.getOppdatertDato());
    assertEqualInstants(moetedokumentDTO.getOppdatertDato(), moetedokumentES.getOppdatertDato());

    // MoetedokumentES
    assertEquals(
        moetedokumentDTO.getMoetedokumenttype(),
        moetedokumentES.getMøtedokumentregistreringstype());

    // MoetedokumentES.dokumentbeskrivelse
    if (moetedokumentDTO.getDokumentbeskrivelse() != null
        && moetedokumentDTO.getDokumentbeskrivelse().size() > 0) {
      assertEquals(
          moetedokumentDTO.getDokumentbeskrivelse().size(),
          moetedokumentES.getDokumentbeskrivelse().size());
      for (int i = 0; i < moetedokumentDTO.getDokumentbeskrivelse().size(); i++) {
        var dokumentbeskrivelseField = moetedokumentDTO.getDokumentbeskrivelse().get(i);
        var dokumentbeskrivelseDTO = dokumentbeskrivelseField.getExpandedObject();
        var dokumentbeskrivelseES = moetedokumentES.getDokumentbeskrivelse().get(i);
        if (dokumentbeskrivelseDTO == null) {
          dokumentbeskrivelseDTO = dokumentbeskrivelseService.get(dokumentbeskrivelseField.getId());
        }
        compareDokumentbeskrivelse(dokumentbeskrivelseDTO, dokumentbeskrivelseES);
      }
    }
  }

  protected void compareMoetesak(MoetesakDTO moetesakDTO, MoetesakES moetesakES) throws Exception {
    // BaseES
    assertEquals(moetesakDTO.getId(), moetesakES.getId());
    assertEquals(moetesakDTO.getExternalId(), moetesakES.getExternalId());

    var moetemappeField = moetesakDTO.getMoetemappe();
    var isOrphan = true;
    if (moetemappeField != null) {
      var moetemappeDTO = moetemappeField.getExpandedObject();
      if (moetemappeDTO == null) {
        moetemappeDTO = moetemappeService.get(moetemappeField.getId());
      }
      if (moetemappeDTO.getMoetedato() != null) {
        isOrphan = false;
      }
    }
    if (isOrphan) {
      assertEquals(List.of("KommerTilBehandlingMøtesaksregistrering"), moetesakES.getType());
    } else {
      assertEquals(List.of("Møtesaksregistrering"), moetesakES.getType());
    }

    // ArkivBaseES
    assertNotNull(moetesakDTO.getUtvalgObjekt());
    var administrativEnhetId = moetesakDTO.getUtvalgObjekt().getId();
    var administrativEnhetDTO = enhetService.findById(administrativEnhetId);
    var transitive = enhetService.getTransitiveEnhets(administrativEnhetId);
    assertEquals(administrativEnhetDTO.getIri(), moetesakES.getArkivskaper());
    assertEquals(administrativEnhetDTO.getNavn(), moetesakES.getArkivskaperSorteringNavn());
    assertEquals(
        transitive.stream().map(e -> e.getNavn()).toList(), moetesakES.getArkivskaperNavn());
    assertEquals(
        transitive.stream().map(e -> e.getIri()).toList(), moetesakES.getArkivskaperTransitive());

    // RegistreringES
    assertEquals(moetesakDTO.getOffentligTittel(), moetesakES.getOffentligTittel());
    assertEquals(
        moetesakDTO.getOffentligTittelSensitiv(), moetesakES.getOffentligTittel_SENSITIV());
    assertNotNull(moetesakDTO.getPublisertDato());
    assertEqualInstants(moetesakDTO.getPublisertDato(), moetesakES.getPublisertDato());
    assertNotNull(moetesakDTO.getOppdatertDato());
    assertEqualInstants(moetesakDTO.getOppdatertDato(), moetesakES.getOppdatertDato());

    // MoetesakES
    assertEquals("politisk sak", moetesakES.getSorteringstype());
    assertEquals(moetesakDTO.getUtvalg(), moetesakES.getUtvalg());
    if (!isOrphan) {
      var moetesaksaar = String.valueOf(moetesakDTO.getMoetesaksaar());
      var moetesaksaarShort = moetesaksaar.substring(2);
      var moetesakssekvensnummer = String.valueOf(moetesakDTO.getMoetesakssekvensnummer());
      assertEquals(moetesaksaar, moetesakES.getMøtesaksår());
      assertEquals(moetesakssekvensnummer, moetesakES.getMøtesakssekvensnummer());
      assertEquals(moetesaksaar + "/" + moetesakssekvensnummer, moetesakES.getSaksnummer());
      assertEquals(
          List.of(
              moetesaksaar + "/" + moetesakssekvensnummer,
              moetesaksaarShort + "/" + moetesakssekvensnummer,
              moetesakssekvensnummer + "/" + moetesaksaar,
              moetesakssekvensnummer + "/" + moetesaksaarShort),
          moetesakES.getSaksnummerGenerert());
    }

    // MoetesakES.parent
    if (moetemappeField != null) {
      var moetemappeDTO = moetemappeField.getExpandedObject();
      var moetemappeES = moetesakES.getParent();
      if (moetemappeDTO == null) {
        moetemappeDTO = moetemappeService.get(moetemappeField.getId());
      }
      compareMoetemappe(moetemappeDTO, moetemappeES);
    }

    // MoetesakES.dokumentbeskrivelse
    // Note: we also skip if moetesakES.dokumentbeskrivelse is null, since "parent" objects doesn't
    // have this field
    if (moetesakDTO.getDokumentbeskrivelse() != null
        && moetesakES.getDokumentbeskrivelse() != null) {
      assertEquals(
          moetesakDTO.getDokumentbeskrivelse().size(), moetesakES.getDokumentbeskrivelse().size());
      for (int i = 0; i < moetesakDTO.getDokumentbeskrivelse().size(); i++) {
        var dokumentbeskrivelseField = moetesakDTO.getDokumentbeskrivelse().get(i);
        var dokumentbeskrivelseDTO = dokumentbeskrivelseField.getExpandedObject();
        var dokumentbeskrivelseES = moetesakES.getDokumentbeskrivelse().get(i);
        if (dokumentbeskrivelseDTO == null) {
          dokumentbeskrivelseDTO = dokumentbeskrivelseService.get(dokumentbeskrivelseField.getId());
        }
        compareDokumentbeskrivelse(dokumentbeskrivelseDTO, dokumentbeskrivelseES);
      }
    }
  }

  public static void assertEqualInstants(String expected, String actual) {
    var expectedInstant = Instant.parse(expected);
    var actualInstant = Instant.parse(actual);
    var roundedExpected = roundToMilliseconds(expectedInstant);
    var roundedActual = roundToMilliseconds(actualInstant);
    assertEquals(roundedExpected, roundedActual, "Expected: " + expected + " but was: " + actual);
  }

  public static Instant roundToMilliseconds(Instant instant) {
    return instant.plusNanos(500_000).truncatedTo(ChronoUnit.MILLIS);
  }
}
