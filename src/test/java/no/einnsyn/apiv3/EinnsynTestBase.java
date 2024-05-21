package no.einnsyn.apiv3;

import static org.junit.jupiter.api.Assertions.assertEquals;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import no.einnsyn.apiv3.authentication.bruker.BrukerUserDetailsService;
import no.einnsyn.apiv3.authentication.bruker.JwtService;
import no.einnsyn.apiv3.entities.apikey.ApiKeyRepository;
import no.einnsyn.apiv3.entities.apikey.ApiKeyService;
import no.einnsyn.apiv3.entities.apikey.models.ApiKey;
import no.einnsyn.apiv3.entities.arkiv.ArkivRepository;
import no.einnsyn.apiv3.entities.arkiv.ArkivService;
import no.einnsyn.apiv3.entities.arkivdel.ArkivdelRepository;
import no.einnsyn.apiv3.entities.arkivdel.ArkivdelService;
import no.einnsyn.apiv3.entities.behandlingsprotokoll.BehandlingsprotokollRepository;
import no.einnsyn.apiv3.entities.behandlingsprotokoll.BehandlingsprotokollService;
import no.einnsyn.apiv3.entities.bruker.BrukerRepository;
import no.einnsyn.apiv3.entities.bruker.BrukerService;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.DokumentbeskrivelseRepository;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.DokumentbeskrivelseService;
import no.einnsyn.apiv3.entities.dokumentobjekt.DokumentobjektRepository;
import no.einnsyn.apiv3.entities.dokumentobjekt.DokumentobjektService;
import no.einnsyn.apiv3.entities.enhet.EnhetRepository;
import no.einnsyn.apiv3.entities.enhet.EnhetService;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.enhet.models.EnhetstypeEnum;
import no.einnsyn.apiv3.entities.identifikator.IdentifikatorRepository;
import no.einnsyn.apiv3.entities.identifikator.IdentifikatorService;
import no.einnsyn.apiv3.entities.innsynskrav.InnsynskravRepository;
import no.einnsyn.apiv3.entities.innsynskrav.InnsynskravSenderService;
import no.einnsyn.apiv3.entities.innsynskrav.InnsynskravService;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelRepository;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelService;
import no.einnsyn.apiv3.entities.journalpost.JournalpostRepository;
import no.einnsyn.apiv3.entities.journalpost.JournalpostService;
import no.einnsyn.apiv3.entities.klasse.KlasseRepository;
import no.einnsyn.apiv3.entities.klasse.KlasseService;
import no.einnsyn.apiv3.entities.klassifikasjonssystem.KlassifikasjonssystemRepository;
import no.einnsyn.apiv3.entities.klassifikasjonssystem.KlassifikasjonssystemService;
import no.einnsyn.apiv3.entities.korrespondansepart.KorrespondansepartRepository;
import no.einnsyn.apiv3.entities.korrespondansepart.KorrespondansepartService;
import no.einnsyn.apiv3.entities.lagretsak.LagretSakRepository;
import no.einnsyn.apiv3.entities.lagretsak.LagretSakService;
import no.einnsyn.apiv3.entities.lagretsoek.LagretSoekRepository;
import no.einnsyn.apiv3.entities.lagretsoek.LagretSoekService;
import no.einnsyn.apiv3.entities.moetedeltaker.MoetedeltakerRepository;
import no.einnsyn.apiv3.entities.moetedeltaker.MoetedeltakerService;
import no.einnsyn.apiv3.entities.moetedokument.MoetedokumentRepository;
import no.einnsyn.apiv3.entities.moetedokument.MoetedokumentService;
import no.einnsyn.apiv3.entities.moetemappe.MoetemappeRepository;
import no.einnsyn.apiv3.entities.moetemappe.MoetemappeService;
import no.einnsyn.apiv3.entities.moetesak.MoetesakRepository;
import no.einnsyn.apiv3.entities.moetesak.MoetesakService;
import no.einnsyn.apiv3.entities.moetesaksbeskrivelse.MoetesaksbeskrivelseRepository;
import no.einnsyn.apiv3.entities.moetesaksbeskrivelse.MoetesaksbeskrivelseService;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeRepository;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeService;
import no.einnsyn.apiv3.entities.search.SearchService;
import no.einnsyn.apiv3.entities.skjerming.SkjermingRepository;
import no.einnsyn.apiv3.entities.skjerming.SkjermingService;
import no.einnsyn.apiv3.entities.tilbakemelding.TilbakemeldingRepository;
import no.einnsyn.apiv3.entities.tilbakemelding.TilbakemeldingService;
import no.einnsyn.apiv3.entities.utredning.UtredningRepository;
import no.einnsyn.apiv3.entities.utredning.UtredningService;
import no.einnsyn.apiv3.entities.vedtak.VedtakRepository;
import no.einnsyn.apiv3.entities.vedtak.VedtakService;
import no.einnsyn.apiv3.entities.votering.VoteringRepository;
import no.einnsyn.apiv3.entities.votering.VoteringService;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(Lifecycle.PER_CLASS)
public abstract class EinnsynTestBase {

  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  protected static int idSequence = 0;
  @MockBean protected ElasticsearchClient esClient;

  @Autowired protected ApiKeyRepository apiKeyRepository;
  @Autowired protected ArkivRepository arkivRepository;
  @Autowired protected ArkivdelRepository arkivdelRepository;
  @Autowired protected BehandlingsprotokollRepository behandlingsprotokollRepository;
  @Autowired protected BrukerRepository brukerRepository;
  @Autowired protected DokumentbeskrivelseRepository dokumentbeskrivelseRepository;
  @Autowired protected DokumentobjektRepository dokumentobjektRepository;
  @Autowired protected EnhetRepository enhetRepository;
  @Autowired protected IdentifikatorRepository identifikatorRepository;
  @Autowired protected InnsynskravRepository innsynskravRepository;
  @Autowired protected InnsynskravDelRepository innsynskravDelRepository;
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
  @Autowired protected InnsynskravService innsynskravService;
  @Autowired protected InnsynskravDelService innsynskravDelService;
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
  protected String journalenhetSecret;
  protected String journalenhet2Id;
  protected String journalenhet2Key;
  protected String journalenhet2Secret;
  protected String rootEnhetId;
  protected String adminKey;
  protected String adminSecret;
  private int enhetCounter = 0;

  Map<String, Long> rowCountBefore = new HashMap<>();

  protected final CountDownLatch waiter = new CountDownLatch(1);

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
    counts.put("innsynskrav", innsynskravRepository.count());
    counts.put("innsynskravDel", innsynskravDelRepository.count());
    counts.put("journalpost", journalpostRepository.count());
    counts.put("klasse", klasseRepository.count());
    counts.put("klassifikasjonssystem", klassifikasjonssystemRepository.count());
    counts.put("korrespondansepart", korrespondansepartRepository.count());
    // counts.put("lagretSak", lagretSakRepository.count());
    // counts.put("lagretSoek", lagretSoekRepository.count());
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

  @BeforeAll
  @Transactional
  public void _insertBaseEnhets() throws EInnsynException {
    var rootEnhet = new Enhet();
    rootEnhet.setNavn("Root");
    rootEnhet.setEnhetId(UUID.randomUUID());
    rootEnhet.setOpprettetDato(Date.from(Instant.now()));
    rootEnhet.setOppdatertDato(Date.from(Instant.now()));
    rootEnhet.setEnhetstype(EnhetstypeEnum.DUMMYENHET);
    rootEnhet.setOrgnummer(String.valueOf(100000000 + ++enhetCounter));
    rootEnhet = enhetRepository.saveAndFlush(rootEnhet);

    var journalenhet = new Enhet();
    journalenhet.setNavn("Journalenhet");
    journalenhet.setEnhetId(UUID.randomUUID());
    journalenhet.setOpprettetDato(Date.from(Instant.now()));
    journalenhet.setOppdatertDato(Date.from(Instant.now()));
    journalenhet.setEnhetstype(EnhetstypeEnum.KOMMUNE);
    journalenhet.setOrgnummer(String.valueOf(100000000 + ++enhetCounter));
    journalenhet.setInnsynskravEpost("innsynskravepost@example.com");
    journalenhet.setEFormidling(true);
    journalenhet.setParent(rootEnhet);

    var underenhet1 = new Enhet();
    underenhet1.setNavn("Testunderenhet 1");
    underenhet1.setEnhetId(UUID.randomUUID());
    underenhet1.setOpprettetDato(Date.from(Instant.now()));
    underenhet1.setOppdatertDato(Date.from(Instant.now()));
    underenhet1.setEnhetstype(EnhetstypeEnum.BYDEL);
    underenhet1.setParent(journalenhet);

    var underenhet2 = new Enhet();
    underenhet2.setNavn("Testunderenhet 2");
    underenhet2.setEnhetId(UUID.randomUUID());
    underenhet2.setOpprettetDato(Date.from(Instant.now()));
    underenhet2.setOppdatertDato(Date.from(Instant.now()));
    underenhet2.setEnhetstype(EnhetstypeEnum.UTVALG);
    underenhet2.setEnhetskode("UNDER");
    underenhet2.setParent(journalenhet);

    journalenhet.addUnderenhet(underenhet1);
    journalenhet.addUnderenhet(underenhet2);
    enhetRepository.saveAndFlush(journalenhet);

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
    journalenhetSecret = "SECRET_KEY_1";
    journalenhetKeyObject.setEnhet(journalenhet);
    journalenhetKeyObject.setName("Journalenhet");
    journalenhetKeyObject.setSecret(passwordEncoder.encode(journalenhetSecret));
    journalenhetKeyObject = apiKeyRepository.saveAndFlush(journalenhetKeyObject);
    journalenhetKey = journalenhetKeyObject.getId();

    var journalenhet2KeyObject = new ApiKey();
    journalenhet2Secret = "SECRET_KEY_2";
    journalenhet2KeyObject.setEnhet(journalenhet2);
    journalenhet2KeyObject.setName("Journalenhet2");
    journalenhet2KeyObject.setSecret(passwordEncoder.encode(journalenhet2Secret));
    journalenhet2KeyObject = apiKeyRepository.saveAndFlush(journalenhet2KeyObject);
    journalenhet2Key = journalenhet2KeyObject.getId();

    var adminKeyObject = new ApiKey();
    adminSecret = "ADMIN_SECRET";
    adminKeyObject.setEnhet(rootEnhet);
    adminKeyObject.setName("Admin");
    adminKeyObject.setSecret(passwordEncoder.encode("ADMIN_SECRET"));
    adminKeyObject = apiKeyRepository.saveAndFlush(adminKeyObject);
    adminKey = adminKeyObject.getId();
    rootEnhetId = rootEnhet.getId();
  }

  @AfterAll
  @Transactional
  public void _deleteBaseEnhets() throws EInnsynException {
    apiKeyRepository.deleteById(journalenhetKey);
    enhetRepository.deleteById(journalenhetId);

    apiKeyRepository.deleteById(journalenhet2Key);
    enhetRepository.deleteById(journalenhet2Id);

    apiKeyRepository.deleteById(adminKey);
    enhetRepository.deleteById(rootEnhetId);

    // Make sure all tables are empty
    var rowCount = countRows();
    for (var entry : rowCount.entrySet()) {
      var key = entry.getKey();
      var count = entry.getValue();
      // assertEquals(0, count, key + " has " + count + " rows.");
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
}
