package no.einnsyn.backend.entities.downloadcount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import no.einnsyn.backend.entities.dokumentbeskrivelse.DokumentbeskrivelseRepository;
import no.einnsyn.backend.entities.downloadcount.models.DownloadCount;
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
}
