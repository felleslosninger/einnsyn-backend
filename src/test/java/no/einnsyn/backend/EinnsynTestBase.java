package no.einnsyn.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import no.einnsyn.backend.authentication.bruker.BrukerUserDetailsService;
import no.einnsyn.backend.authentication.bruker.JwtService;
import no.einnsyn.backend.entities.apikey.ApiKeyRepository;
import no.einnsyn.backend.entities.apikey.ApiKeyService;
import no.einnsyn.backend.entities.apikey.models.ApiKey;
import no.einnsyn.backend.entities.arkiv.ArkivRepository;
import no.einnsyn.backend.entities.arkiv.ArkivService;
import no.einnsyn.backend.entities.arkivdel.ArkivdelRepository;
import no.einnsyn.backend.entities.arkivdel.ArkivdelService;
import no.einnsyn.backend.entities.behandlingsprotokoll.BehandlingsprotokollRepository;
import no.einnsyn.backend.entities.behandlingsprotokoll.BehandlingsprotokollService;
import no.einnsyn.backend.entities.bruker.BrukerRepository;
import no.einnsyn.backend.entities.bruker.BrukerService;
import no.einnsyn.backend.entities.dokumentbeskrivelse.DokumentbeskrivelseRepository;
import no.einnsyn.backend.entities.dokumentbeskrivelse.DokumentbeskrivelseService;
import no.einnsyn.backend.entities.dokumentobjekt.DokumentobjektRepository;
import no.einnsyn.backend.entities.dokumentobjekt.DokumentobjektService;
import no.einnsyn.backend.entities.enhet.EnhetRepository;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import no.einnsyn.backend.entities.enhet.models.EnhetstypeEnum;
import no.einnsyn.backend.entities.identifikator.IdentifikatorRepository;
import no.einnsyn.backend.entities.identifikator.IdentifikatorService;
import no.einnsyn.backend.entities.innsynskrav.InnsynskravRepository;
import no.einnsyn.backend.entities.innsynskrav.InnsynskravService;
import no.einnsyn.backend.entities.innsynskravbestilling.InnsynskravBestillingRepository;
import no.einnsyn.backend.entities.innsynskravbestilling.InnsynskravBestillingService;
import no.einnsyn.backend.entities.innsynskravbestilling.InnsynskravSenderService;
import no.einnsyn.backend.entities.journalpost.JournalpostRepository;
import no.einnsyn.backend.entities.journalpost.JournalpostService;
import no.einnsyn.backend.entities.klasse.KlasseRepository;
import no.einnsyn.backend.entities.klasse.KlasseService;
import no.einnsyn.backend.entities.klassifikasjonssystem.KlassifikasjonssystemRepository;
import no.einnsyn.backend.entities.klassifikasjonssystem.KlassifikasjonssystemService;
import no.einnsyn.backend.entities.korrespondansepart.KorrespondansepartRepository;
import no.einnsyn.backend.entities.korrespondansepart.KorrespondansepartService;
import no.einnsyn.backend.entities.lagretsak.LagretSakRepository;
import no.einnsyn.backend.entities.lagretsak.LagretSakService;
import no.einnsyn.backend.entities.lagretsoek.LagretSoekRepository;
import no.einnsyn.backend.entities.lagretsoek.LagretSoekService;
import no.einnsyn.backend.entities.moetedeltaker.MoetedeltakerRepository;
import no.einnsyn.backend.entities.moetedeltaker.MoetedeltakerService;
import no.einnsyn.backend.entities.moetedokument.MoetedokumentRepository;
import no.einnsyn.backend.entities.moetedokument.MoetedokumentService;
import no.einnsyn.backend.entities.moetemappe.MoetemappeRepository;
import no.einnsyn.backend.entities.moetemappe.MoetemappeService;
import no.einnsyn.backend.entities.moetesak.MoetesakRepository;
import no.einnsyn.backend.entities.moetesak.MoetesakService;
import no.einnsyn.backend.entities.moetesaksbeskrivelse.MoetesaksbeskrivelseRepository;
import no.einnsyn.backend.entities.moetesaksbeskrivelse.MoetesaksbeskrivelseService;
import no.einnsyn.backend.entities.saksmappe.SaksmappeRepository;
import no.einnsyn.backend.entities.saksmappe.SaksmappeService;
import no.einnsyn.backend.entities.search.SearchService;
import no.einnsyn.backend.entities.skjerming.SkjermingRepository;
import no.einnsyn.backend.entities.skjerming.SkjermingService;
import no.einnsyn.backend.entities.tilbakemelding.TilbakemeldingRepository;
import no.einnsyn.backend.entities.tilbakemelding.TilbakemeldingService;
import no.einnsyn.backend.entities.utredning.UtredningRepository;
import no.einnsyn.backend.entities.utredning.UtredningService;
import no.einnsyn.backend.entities.vedtak.VedtakRepository;
import no.einnsyn.backend.entities.vedtak.VedtakService;
import no.einnsyn.backend.entities.votering.VoteringRepository;
import no.einnsyn.backend.entities.votering.VoteringService;
import no.einnsyn.backend.testutils.ElasticsearchMocks;
import org.apache.commons.codec.digest.DigestUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(Lifecycle.PER_CLASS)
public abstract class EinnsynTestBase {

  protected static int idSequence = 0;

  @Autowired protected ApiKeyRepository apiKeyRepository;
  @Autowired protected ArkivRepository arkivRepository;
  @Autowired protected ArkivdelRepository arkivdelRepository;
  @Autowired protected BehandlingsprotokollRepository behandlingsprotokollRepository;
  @Autowired protected BrukerRepository brukerRepository;
  @Autowired protected DokumentbeskrivelseRepository dokumentbeskrivelseRepository;
  @Autowired protected DokumentobjektRepository dokumentobjektRepository;
  @Autowired protected EnhetRepository enhetRepository;
  @Autowired protected IdentifikatorRepository identifikatorRepository;
  @Autowired protected InnsynskravBestillingRepository innsynskravBestillingRepository;
  @Autowired protected InnsynskravRepository innsynskravRepository;
  @Autowired protected JournalpostRepository journalpostRepository;
  @Autowired protected KlasseRepository klasseRepository;
  @Autowired protected KlassifikasjonssystemRepository klassifikasjonssystemRepository;
  @Autowired protected KorrespondansepartRepository korrespondansepartRepository;
  @Autowired protected LagretSakRepository lagretSakRepository;
  @Autowired protected LagretSoekRepository lagretSoekRepository;
  @Autowired protected MoetedeltakerRepository moetedeltakerRepository;
  @Autowired protected MoetedokumentRepository moetedokumentRepository;
  @Autowired protected MoetemappeRepository moetemappeRepository;
  @Autowired protected MoetesakRepository moetesakRepository;
  @Autowired protected MoetesaksbeskrivelseRepository moetesaksbeskrivelseRepository;
  @Autowired protected SaksmappeRepository saksmappeRepository;
  @Autowired protected SkjermingRepository skjermingRepository;
  @Autowired protected TilbakemeldingRepository tilbakemeldingRepository;
  @Autowired protected UtredningRepository utredningRepository;
  @Autowired protected VedtakRepository vedtakRepository;
  @Autowired protected VoteringRepository voteringRepository;

  @Autowired protected ApiKeyService apiKeyService;
  @Autowired protected BrukerUserDetailsService brukerUserDetailsService;
  @Autowired protected JwtService jwtService;
  @Autowired protected ArkivService arkivService;
  @Autowired protected ArkivdelService arkivdelService;
  @Autowired protected BehandlingsprotokollService behandlingsprotokollService;
  @Autowired protected BrukerService brukerService;
  @Autowired protected DokumentbeskrivelseService dokumentbeskrivelseService;
  @Autowired protected DokumentobjektService dokumentobjektService;
  @Autowired protected EnhetService enhetService;
  @Autowired protected IdentifikatorService identifikatorService;
  @Autowired protected InnsynskravSenderService innsynskravSenderService;
  @Autowired protected InnsynskravBestillingService innsynskravBestillingService;
  @Autowired protected InnsynskravService innsynskravService;
  @Autowired protected JournalpostService journalpostService;
  @Autowired protected KlasseService klasseService;
  @Autowired protected KlassifikasjonssystemService klassifikasjonssystemService;
  @Autowired protected KorrespondansepartService korrespondansepartService;
  @Autowired protected LagretSakService lagretSakService;
  @Autowired protected LagretSoekService lagretSoekService;
  @Autowired protected MoetedeltakerService moetedeltakerService;
  @Autowired protected MoetedokumentService moetedokumentService;
  @Autowired protected MoetemappeService moetemappeService;
  @Autowired protected MoetesakService moetesakService;
  @Autowired protected MoetesaksbeskrivelseService moetesaksbeskrivelseService;
  @Autowired protected SaksmappeService saksmappeService;
  @Autowired protected SearchService searchService;
  @Autowired protected SkjermingService skjermingService;
  @Autowired protected TilbakemeldingService tilbakemeldingService;
  @Autowired protected UtredningService utredningService;
  @Autowired protected VedtakService vedtakService;
  @Autowired protected VoteringService voteringService;

  protected String journalenhetId;
  protected String journalenhetKey;
  protected String journalenhetKeyId;
  protected String journalenhet2Id;
  protected String journalenhet2Key;
  protected String journalenhet2KeyId;
  protected String rootEnhetId;
  protected String rootEnhetIri;
  protected String rootEnhetNavn;
  protected String adminKey;
  protected String adminKeyId;
  protected String underenhetId;
  private int enhetCounter = 0;

  private Map<String, Long> rowCountBefore = new HashMap<>();

  protected final CountDownLatch waiter = new CountDownLatch(1);

  public @MockitoBean ElasticsearchClient esClient;
  public @MockitoBean JavaMailSender javaMailSender;

  /**
   * Count the number of elements in the database, to make sure it is empty after each test
   *
   * @return
   */
  Map<String, Long> countRows() {
    var counts = new HashMap<String, Long>();
    counts.put("apiKey", apiKeyRepository.count());
    counts.put("arkiv", arkivRepository.count());
    counts.put("arkivdel", arkivdelRepository.count());
    counts.put("behandlingsprotokoll", behandlingsprotokollRepository.count());
    counts.put("bruker", brukerRepository.count());
    counts.put("dokumentbeskrivelse", dokumentbeskrivelseRepository.count());
    counts.put("dokumentobjekt", dokumentobjektRepository.count());
    counts.put("enhet", enhetRepository.count());
    counts.put("identifikator", identifikatorRepository.count());
    counts.put("innsynskravBestilling", innsynskravBestillingRepository.count());
    counts.put("innsynskrav", innsynskravRepository.count());
    counts.put("journalpost", journalpostRepository.count());
    counts.put("klasse", klasseRepository.count());
    counts.put("klassifikasjonssystem", klassifikasjonssystemRepository.count());
    counts.put("korrespondansepart", korrespondansepartRepository.count());
    counts.put("lagretSak", lagretSakRepository.count());
    counts.put("lagretSoek", lagretSoekRepository.count());
    counts.put("moetedeltaker", moetedeltakerRepository.count());
    counts.put("moetedokument", moetedokumentRepository.count());
    counts.put("moetemappe", moetemappeRepository.count());
    counts.put("moetesak", moetesakRepository.count());
    counts.put("moetesaksbeskrivelse", moetesaksbeskrivelseRepository.count());
    counts.put("saksmappe", saksmappeRepository.count());
    counts.put("skjerming", skjermingRepository.count());
    counts.put("tilbakemelding", tilbakemeldingRepository.count());
    counts.put("utredning", utredningRepository.count());
    counts.put("vedtak", vedtakRepository.count());
    counts.put("votering", voteringRepository.count());
    return counts;
  }

  @SuppressWarnings("unchecked")
  @BeforeEach
  public void resetEsMock() throws Exception {
    reset(esClient);

    // Always return "Created" when indexing
    var indexResponseMock = getIndexResponseMock();
    when(esClient.index(any(Function.class))).thenReturn(indexResponseMock);
    when(esClient.index(any(IndexRequest.class))).thenReturn(indexResponseMock);

    when(esClient.delete(any(Function.class))).thenReturn(mock(DeleteResponse.class));

    // Return an empty list by default
    var searchResponse = ElasticsearchMocks.searchResponse(0, List.of());
    when(esClient.search(any(SearchRequest.class), any())).thenReturn(searchResponse);

    when(esClient.bulk(any(BulkRequest.class))).thenReturn(mock(BulkResponse.class));
  }

  public IndexResponse getIndexResponseMock() {
    var indexResponse = mock(IndexResponse.class);
    when(indexResponse.result()).thenReturn(Result.Created);
    return indexResponse;
  }

  @BeforeEach
  @BeforeAll
  public void resetJavaMailSenderMock() {
    reset(javaMailSender);
    when(javaMailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));
  }

  @BeforeAll
  @Transactional
  public void _insertBaseEnhets() {
    var rootEnhet = enhetRepository.findByExternalId("root");

    var journalenhet = new Enhet();
    journalenhet.setNavn("Journalenhet");
    journalenhet.setEnhetId(UUID.randomUUID());
    journalenhet.setExternalId("journalenhet");
    journalenhet.setOpprettetDato(Date.from(Instant.now()));
    journalenhet.setOppdatertDato(Date.from(Instant.now()));
    journalenhet.setEnhetstype(EnhetstypeEnum.KOMMUNE);
    journalenhet.setOrgnummer(String.valueOf(100000000 + ++enhetCounter));
    journalenhet.setInnsynskravEpost("innsynskravepost@example.com");
    journalenhet.setKontaktpunktEpost("kontaktpost@example.com");
    journalenhet.setEFormidling(true);
    journalenhet.setParent(rootEnhet);

    var underenhet1 = new Enhet();
    underenhet1.setNavn("Testunderenhet 1");
    underenhet1.setEnhetId(UUID.randomUUID());
    underenhet1.setExternalId("underenhet1");
    underenhet1.setOpprettetDato(Date.from(Instant.now()));
    underenhet1.setOppdatertDato(Date.from(Instant.now()));
    underenhet1.setEnhetstype(EnhetstypeEnum.BYDEL);
    underenhet1.setParent(journalenhet);

    var underenhet2 = new Enhet();
    underenhet2.setNavn("Testunderenhet 2");
    underenhet2.setEnhetId(UUID.randomUUID());
    underenhet2.setExternalId("underenhet2");
    underenhet2.setOpprettetDato(Date.from(Instant.now()));
    underenhet2.setOppdatertDato(Date.from(Instant.now()));
    underenhet2.setEnhetstype(EnhetstypeEnum.UTVALG);
    underenhet2.setEnhetskode("UNDER");
    underenhet2.setParent(journalenhet);

    journalenhet.addUnderenhet(underenhet1);
    journalenhet.addUnderenhet(underenhet2);
    enhetRepository.saveAndFlush(journalenhet);
    underenhetId = underenhet2.getId();

    var journalenhet2 = new Enhet();
    journalenhet2.setNavn("Journalenhet2");
    journalenhet2.setEnhetId(UUID.randomUUID());
    journalenhet2.setOpprettetDato(Date.from(Instant.now()));
    journalenhet2.setOppdatertDato(Date.from(Instant.now()));
    journalenhet2.setEnhetstype(EnhetstypeEnum.UTVALG);
    journalenhet2.setOrgnummer(String.valueOf(100000000 + ++enhetCounter));
    journalenhet2.setInnsynskravEpost("journalenhet2@example.com");
    journalenhet2.setEFormidling(true);
    journalenhet2.setParent(rootEnhet);
    enhetRepository.saveAndFlush(journalenhet2);

    journalenhetId = journalenhet.getId();
    journalenhet2Id = journalenhet2.getId();

    // Add keys
    var journalenhetKeyObject = new ApiKey();
    journalenhetKey = "secret_key_1";
    journalenhetKeyObject.setEnhet(journalenhet);
    journalenhetKeyObject.setName("Journalenhet");
    journalenhetKeyObject.setSecret(DigestUtils.sha256Hex(journalenhetKey));
    journalenhetKeyObject = apiKeyRepository.saveAndFlush(journalenhetKeyObject);
    journalenhetKeyId = journalenhetKeyObject.getId();

    var journalenhet2KeyObject = new ApiKey();
    journalenhet2Key = "secret_key_2";
    journalenhet2KeyObject.setEnhet(journalenhet2);
    journalenhet2KeyObject.setName("Journalenhet2");
    journalenhet2KeyObject.setSecret(DigestUtils.sha256Hex(journalenhet2Key));
    journalenhet2KeyObject = apiKeyRepository.saveAndFlush(journalenhet2KeyObject);
    journalenhet2KeyId = journalenhet2KeyObject.getId();

    var adminKeyObject = new ApiKey();
    adminKey = "secret_testsecret";
    adminKeyObject.setEnhet(rootEnhet);
    adminKeyObject.setName("Admin");
    adminKeyObject.setSecret(DigestUtils.sha256Hex(adminKey));
    adminKeyObject = apiKeyRepository.saveAndFlush(adminKeyObject);
    adminKeyId = adminKeyObject.getId();
    rootEnhetId = rootEnhet.getId();
    rootEnhetIri = rootEnhet.getIri();
    rootEnhetNavn = rootEnhet.getNavn();
  }

  @AfterAll
  @Transactional
  public void _deleteBaseEnhets() {
    enhetRepository.deleteById(underenhetId);

    apiKeyRepository.deleteById(journalenhetKeyId);
    enhetRepository.deleteById(journalenhetId);

    apiKeyRepository.deleteById(journalenhet2KeyId);
    enhetRepository.deleteById(journalenhet2Id);

    apiKeyRepository.deleteById(adminKeyId);

    // Make sure all tables are empty
    var rowCount = countRows();
    for (var entry : rowCount.entrySet()) {
      var key = entry.getKey();
      var count = entry.getValue();
      if (count > 0) {
        System.err.println("Table " + key + " has " + count + " rows.");
      }
    }
  }

  @BeforeEach
  @Transactional
  void _fetchRowsBefore() {
    rowCountBefore = countRows();
  }

  @AfterEach
  @Transactional
  void _countRowsAfter() {
    var rowCountAfter = countRows();

    // Match entries in `counts` against `entityCount`
    for (var entry : rowCountAfter.entrySet()) {
      var key = entry.getKey();
      var before = rowCountBefore.get(key);
      var after = rowCountAfter.get(key);
      assertEquals(before, after, key + " has " + (after - before) + " extra rows.");
    }
  }

  @AfterEach
  @AfterAll
  void awaitAsync() {
    var targetThreadName = "EInnsyn-RequestSideEffect-";
    Awaitility.await()
        .until(
            () ->
                Thread.getAllStackTraces().keySet().stream()
                    .noneMatch(
                        thread ->
                            thread.getName().startsWith(targetThreadName)
                                && thread.getState() == Thread.State.RUNNABLE));
  }
}
