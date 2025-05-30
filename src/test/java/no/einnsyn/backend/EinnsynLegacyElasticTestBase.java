package no.einnsyn.backend;

import static no.einnsyn.backend.testutils.Assertions.assertEqualInstants;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.entities.base.models.BaseES;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseES;
import no.einnsyn.backend.entities.dokumentobjekt.models.DokumentobjektDTO;
import no.einnsyn.backend.entities.dokumentobjekt.models.DokumentobjektES;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import no.einnsyn.backend.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.backend.entities.innsynskrav.models.InnsynskravES;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostES;
import no.einnsyn.backend.entities.korrespondansepart.models.KorrespondansepartDTO;
import no.einnsyn.backend.entities.korrespondansepart.models.KorrespondansepartES;
import no.einnsyn.backend.entities.korrespondansepart.models.KorrespondanseparttypeResolver;
import no.einnsyn.backend.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.backend.entities.moetedokument.models.MoetedokumentES;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeES;
import no.einnsyn.backend.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.backend.entities.moetesak.models.MoetesakES;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeES;
import no.einnsyn.backend.entities.skjerming.models.SkjermingDTO;
import no.einnsyn.backend.entities.skjerming.models.SkjermingES;
import org.awaitility.Awaitility;
import org.mockito.ArgumentCaptor;

@SuppressWarnings({"unchecked"})
public class EinnsynLegacyElasticTestBase extends EinnsynControllerTestBase {

  protected Map<String, BaseES> captureIndexedDocuments(int times) throws Exception {
    Awaitility.await()
        .atMost(30, TimeUnit.SECONDS)
        .untilAsserted(() -> verify(esClient, times(times)).index(any(Function.class)));

    var builderCaptor = ArgumentCaptor.forClass(Function.class);
    verify(esClient, times(times)).index(builderCaptor.capture());
    var builders = builderCaptor.getAllValues();
    var map = new HashMap<String, BaseES>();

    for (var builder : builders) {
      var indexBuilderMock = mock(IndexRequest.Builder.class);
      when(indexBuilderMock.index(anyString())).thenReturn(indexBuilderMock);
      when(indexBuilderMock.id(anyString())).thenReturn(indexBuilderMock);
      when(indexBuilderMock.document(any())).thenReturn(indexBuilderMock);
      var documentCaptor = ArgumentCaptor.forClass(BaseES.class);
      var idCaptor = ArgumentCaptor.forClass(String.class);
      builder.apply(indexBuilderMock);
      verify(indexBuilderMock).id(idCaptor.capture());
      verify(indexBuilderMock).document(documentCaptor.capture());
      var id = idCaptor.getValue();
      var document = documentCaptor.getValue();
      map.put(id, document);
    }

    return map;
  }

  protected Map<String, BaseES> captureBulkIndexedDocuments(int batches, int total)
      throws Exception {
    Awaitility.await()
        .untilAsserted(() -> verify(esClient, times(batches)).bulk(any(BulkRequest.class)));

    var requestCaptor = ArgumentCaptor.forClass(BulkRequest.class);
    verify(esClient, times(batches)).bulk(requestCaptor.capture());
    var builders = requestCaptor.getAllValues();
    var map = new HashMap<String, BaseES>();

    for (var builder : builders) {
      builder
          .operations()
          .forEach(
              operation -> {
                map.put(operation.index().id(), (BaseES) operation.index().document());
              });
    }

    assertEquals(total, map.size());

    return map;
  }

  protected Set<String> captureDeletedDocuments(int times) throws Exception {
    Awaitility.await()
        .atMost(30, TimeUnit.SECONDS)
        .untilAsserted(() -> verify(esClient, times(times)).delete(any(Function.class)));

    var builderCaptor = ArgumentCaptor.forClass(Function.class);
    verify(esClient, times(times)).delete(builderCaptor.capture());
    var builders = builderCaptor.getAllValues();
    var set = new HashSet<String>();

    for (var builder : builders) {
      var deleteBuilderMock = mock(DeleteRequest.Builder.class);
      when(deleteBuilderMock.index(anyString())).thenReturn(deleteBuilderMock);
      when(deleteBuilderMock.id(anyString())).thenReturn(deleteBuilderMock);
      var idCaptor = ArgumentCaptor.forClass(String.class);
      builder.apply(deleteBuilderMock);
      verify(deleteBuilderMock).id(idCaptor.capture());
      var id = idCaptor.getValue();
      set.add(id);
    }

    return set;
  }

  protected Set<String> captureBulkDeletedDocuments(int batches, int total) throws Exception {
    Awaitility.await()
        .untilAsserted(() -> verify(esClient, times(batches)).bulk(any(BulkRequest.class)));

    var requestCaptor = ArgumentCaptor.forClass(BulkRequest.class);
    verify(esClient, times(batches)).bulk(requestCaptor.capture());
    var builders = requestCaptor.getAllValues();
    var set = new HashSet<String>();

    for (var builder : builders) {
      builder.operations().forEach(operation -> set.add(operation.delete().id()));
    }

    assertEquals(total, set.size());

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
      assertEquals(
          List.of("KommerTilBehandlingMøtesaksregistrering", "Moetesak"), moetesakES.getType());
    } else {
      assertEquals(List.of("Møtesaksregistrering", "Moetesak"), moetesakES.getType());
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

  protected void compareInnsynskrav(
      InnsynskravDTO innsynskravDTO,
      InnsynskravBestillingDTO innsynskravBestillingDTO,
      InnsynskravES innsynskravES)
      throws Exception {
    // BaseES
    assertEquals(innsynskravDTO.getId(), innsynskravES.getId());
    assertEquals(innsynskravDTO.getExternalId(), innsynskravES.getExternalId());
    assertEquals(List.of("Innsynskrav"), innsynskravES.getType());

    // InnsynskravES
    assertNotNull(innsynskravES.getCreated());

    assertEqualInstants(innsynskravDTO.getSent(), innsynskravES.getSent());
    assertEquals(innsynskravBestillingDTO.getVerified(), innsynskravES.getVerified());
    assertEquals(innsynskravBestillingDTO.getBruker(), innsynskravES.getBruker());
    assertEquals(innsynskravDTO.getEnhet().getId(), innsynskravES.getJournalenhet());

    var innsynskravStatRelation = innsynskravES.getStatRelation();
    assertEquals("innsynskrav", innsynskravStatRelation.getName());
    assertEquals(innsynskravDTO.getJournalpost().getId(), innsynskravStatRelation.getParent());
  }
}
