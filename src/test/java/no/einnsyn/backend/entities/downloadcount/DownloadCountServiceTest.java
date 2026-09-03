package no.einnsyn.backend.entities.downloadcount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.RefreshRequest;
import co.elastic.clients.elasticsearch.indices.RefreshResponse;
import co.elastic.clients.util.ObjectBuilder;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import no.einnsyn.backend.entities.dokumentbeskrivelse.DokumentbeskrivelseRepository;
import no.einnsyn.backend.entities.downloadcount.models.DownloadCount;
import no.einnsyn.backend.entities.downloadcount.models.DownloadCountES;
import no.einnsyn.backend.entities.journalpost.JournalpostRepository;
import no.einnsyn.backend.entities.moetedokument.MoetedokumentRepository;
import no.einnsyn.backend.entities.moetemappe.MoetemappeRepository;
import no.einnsyn.backend.entities.moetesak.MoetesakRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.test.util.ReflectionTestUtils;

class DownloadCountServiceTest {

  private DownloadCountRepository repository;
  private DokumentbeskrivelseRepository dokumentbeskrivelseRepository;
  private JournalpostRepository journalpostRepository;
  private MoetedokumentRepository moetedokumentRepository;
  private MoetemappeRepository moetemappeRepository;
  private MoetesakRepository moetesakRepository;
  private ElasticsearchClient esClient;
  private DownloadCountService downloadCountService;

  @BeforeEach
  void setUp() {
    repository = mock(DownloadCountRepository.class);
    dokumentbeskrivelseRepository = mock(DokumentbeskrivelseRepository.class);
    journalpostRepository = mock(JournalpostRepository.class);
    moetedokumentRepository = mock(MoetedokumentRepository.class);
    moetemappeRepository = mock(MoetemappeRepository.class);
    moetesakRepository = mock(MoetesakRepository.class);
    esClient = mock(ElasticsearchClient.class);
    downloadCountService =
        new DownloadCountService(
            repository,
            dokumentbeskrivelseRepository,
            journalpostRepository,
            moetedokumentRepository,
            moetemappeRepository,
            moetesakRepository);
    ReflectionTestUtils.setField(downloadCountService, "esClient", esClient);
    ReflectionTestUtils.setField(downloadCountService, "elasticsearchIndex", "test-index");
  }

  @Test
  void shouldReturnJournalpostWhenDokumentbeskrivelseBelongsToJournalpost() {
    var dokumentobjektId = "do_01jxyz123456789abcdefghij";
    var dokumentbeskrivelseId = "db_01jxyz123456789abcdefghij";
    var journalpostId = "jp_01jxyz123456789abcdefghij";
    var downloadCount = new DownloadCount();
    downloadCount.setDokumentobjektId(dokumentobjektId);

    when(dokumentbeskrivelseRepository.findIdByDokumentobjektId(dokumentobjektId))
        .thenReturn(dokumentbeskrivelseId);
    when(journalpostRepository.streamIdByDokumentbeskrivelseId(dokumentbeskrivelseId))
        .thenReturn(Stream.of(journalpostId));

    assertEquals(journalpostId, downloadCountService.getESParent(downloadCount, "dc_1"));
  }

  @Test
  void shouldReturnMoetemappeWhenDokumentbeskrivelseBelongsToMoetedokument() {
    var dokumentobjektId = "do_01jxyz123456789abcdefghij";
    var dokumentbeskrivelseId = "db_01jxyz123456789abcdefghij";
    var moetedokumentId = "md_01jxyz123456789abcdefghij";
    var moetemappeId = "mm_01jxyz123456789abcdefghij";
    var downloadCount = new DownloadCount();
    downloadCount.setDokumentobjektId(dokumentobjektId);

    when(dokumentbeskrivelseRepository.findIdByDokumentobjektId(dokumentobjektId))
        .thenReturn(dokumentbeskrivelseId);
    when(journalpostRepository.streamIdByDokumentbeskrivelseId(dokumentbeskrivelseId))
        .thenReturn(Stream.empty());
    when(moetesakRepository.streamIdByDokumentbeskrivelseId(dokumentbeskrivelseId))
        .thenReturn(Stream.empty());
    when(moetesakRepository.streamIdByUtredningsdokumentId(dokumentbeskrivelseId))
        .thenReturn(Stream.empty());
    when(moetesakRepository.streamIdByVedtaksdokumentId(dokumentbeskrivelseId))
        .thenReturn(Stream.empty());
    when(moetedokumentRepository.streamIdByDokumentbeskrivelseId(dokumentbeskrivelseId))
        .thenReturn(Stream.of(moetedokumentId));
    when(moetemappeRepository.findIdByMoetedokumentId(moetedokumentId)).thenReturn(moetemappeId);

    assertEquals(moetemappeId, downloadCountService.getESParent(downloadCount, "dc_1"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldReturnParentFromElasticsearchWhenDatabaseLookupReturnsNull() throws Exception {
    var dokumentobjektId = "do_01jxyz123456789abcdefghij";
    var downloadCountId = "dc_01jxyz123456789abcdefghij";
    var esParentId = "jp_01jxyz123456789fallback123";
    var downloadCount = new DownloadCount();
    var indicesClient = mock(ElasticsearchIndicesClient.class);
    var searchResponse = (SearchResponse<Void>) mock(SearchResponse.class);
    var hitsMetadata = (HitsMetadata<Void>) mock(HitsMetadata.class);
    var hit = (Hit<Void>) mock(Hit.class);

    downloadCount.setDokumentobjektId(dokumentobjektId);

    when(dokumentbeskrivelseRepository.findIdByDokumentobjektId(dokumentobjektId)).thenReturn(null);
    when(esClient.indices()).thenReturn(indicesClient);
    when(indicesClient.refresh(
            ArgumentMatchers
                .<Function<RefreshRequest.Builder, ObjectBuilder<RefreshRequest>>>any()))
        .thenReturn(mock(RefreshResponse.class));
    when(esClient.search(
            ArgumentMatchers.<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>>any(),
            eq(Void.class)))
        .thenReturn(searchResponse);
    when(searchResponse.hits()).thenReturn(hitsMetadata);
    when(hitsMetadata.hits()).thenReturn(List.of(hit));
    when(hit.routing()).thenReturn(esParentId);

    assertEquals(esParentId, downloadCountService.getESParent(downloadCount, downloadCountId));
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldNotRefreshWhenEsDocumentIsAlreadySearchable() throws Exception {
    var dokumentobjektId = "do_01jxyz123456789abcdefghij";
    var downloadCountId = "dc_01jxyz123456789abcdefghij";
    var esParentId = "jp_01jxyz123456789fallback123";
    var downloadCount = new DownloadCount();
    var indicesClient = mock(ElasticsearchIndicesClient.class);
    var searchResponse = (SearchResponse<Void>) mock(SearchResponse.class);
    var hitsMetadata = (HitsMetadata<Void>) mock(HitsMetadata.class);
    var hit = (Hit<Void>) mock(Hit.class);

    downloadCount.setDokumentobjektId(dokumentobjektId);

    when(dokumentbeskrivelseRepository.findIdByDokumentobjektId(dokumentobjektId)).thenReturn(null);
    when(esClient.indices()).thenReturn(indicesClient);
    when(esClient.search(
            ArgumentMatchers.<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>>any(),
            eq(Void.class)))
        .thenReturn(searchResponse);
    when(searchResponse.hits()).thenReturn(hitsMetadata);
    when(hitsMetadata.hits()).thenReturn(List.of(hit));
    when(hit.routing()).thenReturn(esParentId);

    assertEquals(esParentId, downloadCountService.getESParent(downloadCount, downloadCountId));

    // Refreshing the whole shared index per document is what made deletes expensive.
    verify(indicesClient, never())
        .refresh(
            ArgumentMatchers
                .<Function<RefreshRequest.Builder, ObjectBuilder<RefreshRequest>>>any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldRefreshAndRetryWhenEsDocumentIsNotYetSearchable() throws Exception {
    var dokumentobjektId = "do_01jxyz123456789abcdefghij";
    var downloadCountId = "dc_01jxyz123456789abcdefghij";
    var esParentId = "jp_01jxyz123456789fallback123";
    var downloadCount = new DownloadCount();
    var indicesClient = mock(ElasticsearchIndicesClient.class);
    var emptyResponse = (SearchResponse<Void>) mock(SearchResponse.class);
    var emptyHits = (HitsMetadata<Void>) mock(HitsMetadata.class);
    var foundResponse = (SearchResponse<Void>) mock(SearchResponse.class);
    var foundHits = (HitsMetadata<Void>) mock(HitsMetadata.class);
    var hit = (Hit<Void>) mock(Hit.class);

    downloadCount.setDokumentobjektId(dokumentobjektId);

    when(dokumentbeskrivelseRepository.findIdByDokumentobjektId(dokumentobjektId)).thenReturn(null);
    when(esClient.indices()).thenReturn(indicesClient);
    when(indicesClient.refresh(
            ArgumentMatchers
                .<Function<RefreshRequest.Builder, ObjectBuilder<RefreshRequest>>>any()))
        .thenReturn(mock(RefreshResponse.class));
    when(esClient.search(
            ArgumentMatchers.<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>>any(),
            eq(Void.class)))
        .thenReturn(emptyResponse, foundResponse);
    when(emptyResponse.hits()).thenReturn(emptyHits);
    when(emptyHits.hits()).thenReturn(List.of());
    when(foundResponse.hits()).thenReturn(foundHits);
    when(foundHits.hits()).thenReturn(List.of(hit));
    when(hit.routing()).thenReturn(esParentId);

    assertEquals(esParentId, downloadCountService.getESParent(downloadCount, downloadCountId));

    verify(indicesClient)
        .refresh(
            ArgumentMatchers
                .<Function<RefreshRequest.Builder, ObjectBuilder<RefreshRequest>>>any());
  }

  @Test
  void toLegacyESShouldUseResolvedParentWithoutResolvingAgain() {
    var now = Instant.now();
    var esParentId = "jp_01jxyz123456789abcdefghij";
    var downloadCount = new DownloadCount();
    downloadCount.setId("dc_01jxyz123456789abcdefghij");
    downloadCount.setDokumentobjektId("do_01jxyz123456789abcdefghij");
    downloadCount.setCount(7);
    downloadCount.setCreated(now);
    downloadCount.setUpdated(now);

    var es = (DownloadCountES) downloadCountService.toLegacyES(downloadCount, esParentId);

    assertEquals(7, es.getCount());
    assertEquals(esParentId, es.getStatRelation().getParent());
    assertEquals("download", es.getStatRelation().getName());

    // index() has already resolved the parent. Resolving it again here is what doubled the
    // repository round trips for every indexed download.
    verifyNoInteractions(
        dokumentbeskrivelseRepository,
        journalpostRepository,
        moetesakRepository,
        moetedokumentRepository,
        moetemappeRepository);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldReturnNullWithoutThrowingWhenNoEsDocumentExists() throws Exception {
    var downloadCountId = "dc_01jxyz123456789abcdefghij";
    var indicesClient = mock(ElasticsearchIndicesClient.class);
    var emptyResponse = (SearchResponse<Void>) mock(SearchResponse.class);
    var emptyHits = (HitsMetadata<Void>) mock(HitsMetadata.class);

    when(esClient.indices()).thenReturn(indicesClient);
    when(indicesClient.refresh(
            ArgumentMatchers
                .<Function<RefreshRequest.Builder, ObjectBuilder<RefreshRequest>>>any()))
        .thenReturn(mock(RefreshResponse.class));
    when(esClient.search(
            ArgumentMatchers.<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>>any(),
            eq(Void.class)))
        .thenReturn(emptyResponse);
    when(emptyResponse.hits()).thenReturn(emptyHits);
    when(emptyHits.hits()).thenReturn(List.of());

    // A count deleted before it was ever indexed has no document and no parent. This used to raise
    // NoSuchElementException and log an error with a stack trace.
    assertNull(downloadCountService.getESParent(null, downloadCountId));
  }
}
