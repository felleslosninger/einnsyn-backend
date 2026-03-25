package no.einnsyn.backend.entities.downloadcount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import no.einnsyn.backend.entities.dokumentbeskrivelse.DokumentbeskrivelseRepository;
import no.einnsyn.backend.entities.downloadcount.models.DownloadCount;
import no.einnsyn.backend.entities.journalpost.JournalpostRepository;
import no.einnsyn.backend.entities.moetedokument.MoetedokumentRepository;
import no.einnsyn.backend.entities.moetemappe.MoetemappeRepository;
import no.einnsyn.backend.entities.moetesak.MoetesakRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DownloadCountServiceTest {

  private DownloadCountRepository repository;
  private DokumentbeskrivelseRepository dokumentbeskrivelseRepository;
  private JournalpostRepository journalpostRepository;
  private MoetedokumentRepository moetedokumentRepository;
  private MoetemappeRepository moetemappeRepository;
  private MoetesakRepository moetesakRepository;
  private DownloadCountService downloadCountService;

  @BeforeEach
  void setUp() {
    repository = mock(DownloadCountRepository.class);
    dokumentbeskrivelseRepository = mock(DokumentbeskrivelseRepository.class);
    journalpostRepository = mock(JournalpostRepository.class);
    moetedokumentRepository = mock(MoetedokumentRepository.class);
    moetemappeRepository = mock(MoetemappeRepository.class);
    moetesakRepository = mock(MoetesakRepository.class);
    downloadCountService =
        new DownloadCountService(
            repository,
            dokumentbeskrivelseRepository,
            journalpostRepository,
            moetedokumentRepository,
            moetemappeRepository,
            moetesakRepository);
  }

  @Test
  void getESParentReturnsJournalpostWhenDokumentbeskrivelseBelongsToJournalpost() {
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
  void getESParentReturnsMoetemappeWhenDokumentbeskrivelseBelongsToMoetedokument() {
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
}
