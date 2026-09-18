package no.einnsyn.backend.entities.downloadcount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import no.einnsyn.backend.entities.dokumentbeskrivelse.DokumentbeskrivelseRepository;
import no.einnsyn.backend.entities.downloadcount.models.DownloadCount;
import no.einnsyn.backend.entities.downloadcount.models.DownloadCountES;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import no.einnsyn.backend.entities.journalpost.JournalpostRepository;
import no.einnsyn.backend.entities.journalpost.models.Journalpost;
import no.einnsyn.backend.entities.moetedokument.MoetedokumentRepository;
import no.einnsyn.backend.entities.moetemappe.MoetemappeRepository;
import no.einnsyn.backend.entities.moetemappe.models.Moetemappe;
import no.einnsyn.backend.entities.moetesak.MoetesakRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DownloadCountServiceTest {

  private static final String DOKUMENTOBJEKT_ID = "do_01jxyz123456789abcdefghij";
  private static final String DOKUMENTBESKRIVELSE_ID = "db_01jxyz123456789abcdefghij";

  private DownloadCountRepository repository;
  private DokumentbeskrivelseRepository dokumentbeskrivelseRepository;
  private JournalpostRepository journalpostRepository;
  private MoetedokumentRepository moetedokumentRepository;
  private MoetemappeRepository moetemappeRepository;
  private MoetesakRepository moetesakRepository;
  private EnhetService enhetService;
  private DownloadCountService downloadCountService;

  @BeforeEach
  void setUp() {
    repository = mock(DownloadCountRepository.class);
    dokumentbeskrivelseRepository = mock(DokumentbeskrivelseRepository.class);
    journalpostRepository = mock(JournalpostRepository.class);
    moetedokumentRepository = mock(MoetedokumentRepository.class);
    moetemappeRepository = mock(MoetemappeRepository.class);
    moetesakRepository = mock(MoetesakRepository.class);
    enhetService = mock(EnhetService.class);
    downloadCountService =
        new DownloadCountService(
            repository,
            dokumentbeskrivelseRepository,
            journalpostRepository,
            moetedokumentRepository,
            moetemappeRepository,
            moetesakRepository);
    ReflectionTestUtils.setField(downloadCountService, "enhetService", enhetService);
  }

  private static Enhet enhet(String id) {
    var enhet = new Enhet();
    enhet.setId(id);
    return enhet;
  }

  @Test
  void recordDownloadShouldOnlyIncrementWhenBucketExists() {
    when(repository.incrementCount(eq(DOKUMENTOBJEKT_ID), any())).thenReturn(1);

    downloadCountService.recordDownload(DOKUMENTOBJEKT_ID);

    // The common case must stay a single statement, without resolving the parent again.
    verify(repository, never())
        .insertOrIncrementCount(anyString(), anyString(), any(), any(), any());
    verifyNoInteractions(
        dokumentbeskrivelseRepository,
        journalpostRepository,
        moetesakRepository,
        moetedokumentRepository,
        moetemappeRepository);
  }

  @Test
  void recordDownloadShouldAttributeNewBucketToJournalpostAndItsEnhet() {
    var journalpostId = "jp_01jxyz123456789abcdefghij";
    var enhetId = "enh_01jxyz123456789abcdefghij";
    var journalpost = new Journalpost();
    journalpost.setId(journalpostId);
    journalpost.setAdministrativEnhetObjekt(enhet(enhetId));

    when(repository.incrementCount(eq(DOKUMENTOBJEKT_ID), any())).thenReturn(0);
    when(dokumentbeskrivelseRepository.findIdByDokumentobjektId(DOKUMENTOBJEKT_ID))
        .thenReturn(DOKUMENTBESKRIVELSE_ID);
    when(journalpostRepository.streamIdByDokumentbeskrivelseId(DOKUMENTBESKRIVELSE_ID))
        .thenReturn(Stream.of(journalpostId));
    when(journalpostRepository.findById(journalpostId)).thenReturn(Optional.of(journalpost));

    downloadCountService.recordDownload(DOKUMENTOBJEKT_ID);

    verify(repository)
        .insertOrIncrementCount(
            anyString(), eq(DOKUMENTOBJEKT_ID), any(), eq(journalpostId), eq(enhetId));
  }

  @Test
  void recordDownloadShouldAttributeMoetedokumentFileToMoetemappeAndItsUtvalg() {
    var moetedokumentId = "md_01jxyz123456789abcdefghij";
    var moetemappeId = "mm_01jxyz123456789abcdefghij";
    var utvalgId = "enh_01jxyz123456789utvalg000";
    var moetemappe = new Moetemappe();
    moetemappe.setId(moetemappeId);
    moetemappe.setUtvalgObjekt(enhet(utvalgId));

    when(repository.incrementCount(eq(DOKUMENTOBJEKT_ID), any())).thenReturn(0);
    when(dokumentbeskrivelseRepository.findIdByDokumentobjektId(DOKUMENTOBJEKT_ID))
        .thenReturn(DOKUMENTBESKRIVELSE_ID);
    when(journalpostRepository.streamIdByDokumentbeskrivelseId(DOKUMENTBESKRIVELSE_ID))
        .thenReturn(Stream.empty());
    when(moetesakRepository.streamIdByDokumentbeskrivelseId(DOKUMENTBESKRIVELSE_ID))
        .thenReturn(Stream.empty());
    when(moetesakRepository.streamIdByUtredningsdokumentId(DOKUMENTBESKRIVELSE_ID))
        .thenReturn(Stream.empty());
    when(moetesakRepository.streamIdByVedtaksdokumentId(DOKUMENTBESKRIVELSE_ID))
        .thenReturn(Stream.empty());
    when(moetedokumentRepository.streamIdByDokumentbeskrivelseId(DOKUMENTBESKRIVELSE_ID))
        .thenReturn(Stream.of(moetedokumentId));
    when(moetemappeRepository.findIdByMoetedokumentId(moetedokumentId)).thenReturn(moetemappeId);
    when(moetemappeRepository.findById(moetemappeId)).thenReturn(Optional.of(moetemappe));

    downloadCountService.recordDownload(DOKUMENTOBJEKT_ID);

    verify(repository)
        .insertOrIncrementCount(
            anyString(), eq(DOKUMENTOBJEKT_ID), any(), eq(moetemappeId), eq(utvalgId));
  }

  @Test
  void recordDownloadShouldStillCountWhenNoParentCanBeResolved() {
    when(repository.incrementCount(eq(DOKUMENTOBJEKT_ID), any())).thenReturn(0);
    when(dokumentbeskrivelseRepository.findIdByDokumentobjektId(DOKUMENTOBJEKT_ID))
        .thenReturn(null);

    downloadCountService.recordDownload(DOKUMENTOBJEKT_ID);

    verify(repository)
        .insertOrIncrementCount(anyString(), eq(DOKUMENTOBJEKT_ID), any(), isNull(), isNull());
  }

  @Test
  void getESParentShouldUseTheStoredParent() {
    var downloadCount = new DownloadCount();
    downloadCount.setParentId("jp_01jxyz123456789abcdefghij");

    assertEquals(
        "jp_01jxyz123456789abcdefghij", downloadCountService.getESParent(downloadCount, "dc_1"));
    // No stored parent, or no row at all, means no routing. Nothing is looked up.
    assertNull(downloadCountService.getESParent(new DownloadCount(), "dc_1"));
    assertNull(downloadCountService.getESParent(null, "dc_1"));
    verifyNoInteractions(
        dokumentbeskrivelseRepository,
        journalpostRepository,
        moetesakRepository,
        moetedokumentRepository,
        moetemappeRepository);
  }

  @Test
  void toLegacyESShouldCarryParentAndEnhet() {
    var now = Instant.now();
    var esParentId = "jp_01jxyz123456789abcdefghij";
    var enhet = enhet("enh_01jxyz123456789abcdefghij");
    var parentEnhet = enhet("enh_01jxyz123456789parent000");
    var downloadCount = new DownloadCount();
    downloadCount.setId("dc_01jxyz123456789abcdefghij");
    downloadCount.setDokumentobjektId(DOKUMENTOBJEKT_ID);
    downloadCount.setCount(7);
    downloadCount.setCreated(now);
    downloadCount.setUpdated(now);
    downloadCount.setParentId(esParentId);
    downloadCount.setEnhetId(enhet.getId());
    when(enhetService.find(enhet.getId())).thenReturn(enhet);
    when(enhetService.getTransitiveEnhets(enhet)).thenReturn(List.of(enhet, parentEnhet));

    var es = (DownloadCountES) downloadCountService.toLegacyES(downloadCount);

    assertEquals(7, es.getCount());
    assertEquals(esParentId, es.getStatRelation().getParent());
    assertEquals("download", es.getStatRelation().getName());
    assertEquals(enhet.getId(), es.getAdministrativEnhet());
    assertEquals(List.of(enhet.getId(), parentEnhet.getId()), es.getAdministrativEnhetTransitive());
  }

  @Test
  void toLegacyESShouldKeepAttributionWhenEnhetIsDeleted() {
    var enhetId = "enh_01jxyz123456789deleted00";
    var downloadCount = new DownloadCount();
    downloadCount.setId("dc_01jxyz123456789abcdefghij");
    downloadCount.setDokumentobjektId(DOKUMENTOBJEKT_ID);
    downloadCount.setCount(1);
    downloadCount.setCreated(Instant.now());
    downloadCount.setUpdated(Instant.now());
    downloadCount.setEnhetId(enhetId);
    when(enhetService.find(enhetId)).thenReturn(null);

    var es = (DownloadCountES) downloadCountService.toLegacyES(downloadCount);

    // The bucket records what the attribution was; a deleted Enhet must not erase it.
    assertEquals(enhetId, es.getAdministrativEnhet());
    assertEquals(List.of(enhetId), es.getAdministrativEnhetTransitive());
  }
}
