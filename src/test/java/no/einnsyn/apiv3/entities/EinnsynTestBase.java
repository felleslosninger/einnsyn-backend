package no.einnsyn.apiv3.entities;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.transaction.Transactional;
import no.einnsyn.apiv3.entities.bruker.BrukerService;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.DokumentbeskrivelseRepository;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.DokumentbeskrivelseService;
import no.einnsyn.apiv3.entities.dokumentobjekt.DokumentobjektRepository;
import no.einnsyn.apiv3.entities.dokumentobjekt.DokumentobjektService;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.enhet.EnhetRepository;
import no.einnsyn.apiv3.entities.enhet.EnhetService;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.enhet.models.Enhetstype;
import no.einnsyn.apiv3.entities.innsynskrav.InnsynskravRepository;
import no.einnsyn.apiv3.entities.innsynskrav.InnsynskravSenderService;
import no.einnsyn.apiv3.entities.innsynskrav.InnsynskravService;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelRepository;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelService;
import no.einnsyn.apiv3.entities.journalpost.JournalpostRepository;
import no.einnsyn.apiv3.entities.journalpost.JournalpostService;
import no.einnsyn.apiv3.entities.korrespondansepart.KorrespondansepartRepository;
import no.einnsyn.apiv3.entities.korrespondansepart.KorrespondansepartService;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeRepository;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeService;
import no.einnsyn.apiv3.entities.skjerming.SkjermingRepository;
import no.einnsyn.apiv3.entities.skjerming.SkjermingService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(Lifecycle.PER_CLASS)
public abstract class EinnsynTestBase {

  protected static int idSequence = 0;

  @MockBean
  protected ElasticsearchClient esClient;

  @Autowired
  protected DokumentbeskrivelseRepository dokumentbeskrivelseRepository;

  @Autowired
  protected DokumentbeskrivelseService dokumentbeskrivelseService;

  @Autowired
  protected DokumentobjektRepository dokumentobjektRepository;

  @Autowired
  protected DokumentobjektService dokumentobjektService;

  @Autowired
  protected EnhetRepository enhetRepository;

  @Autowired
  protected EnhetService enhetService;

  @Autowired
  protected JournalpostRepository journalpostRepository;

  @Autowired
  protected JournalpostService journalpostService;

  @Autowired
  protected KorrespondansepartRepository korrespondansepartRepository;

  @Autowired
  protected KorrespondansepartService korrespondansepartService;

  @Autowired
  protected SaksmappeRepository saksmappeRepository;

  @Autowired
  protected SaksmappeService saksmappeService;

  @Autowired
  protected SkjermingRepository skjermingRepository;

  @Autowired
  protected SkjermingService skjermingService;

  @Autowired
  protected InnsynskravRepository innsynskravRepository;

  @Autowired
  protected InnsynskravService innsynskravService;

  @Autowired
  protected InnsynskravSenderService innsynskravSenderService;

  @Autowired
  protected InnsynskravDelRepository innsynskravDelRepository;

  @Autowired
  protected InnsynskravDelService innsynskravDelService;

  @Autowired
  protected BrukerService brukerService;

  protected UUID journalenhetId = null;

  protected UUID journalenhet2Id = null;

  private int enhetCounter = 0;

  @BeforeAll
  @Transactional
  public void _insertBaseEnhets() {
    var journalenhet = new Enhet();
    journalenhet.setNavn("Journalenhet");
    journalenhet.setLegacyId(UUID.randomUUID());
    journalenhet.setOpprettetDato(Date.from(Instant.now()));
    journalenhet.setOppdatertDato(Date.from(Instant.now()));
    journalenhet.setEnhetstype(Enhetstype.KOMMUNE);
    journalenhet.setOrgnummer(String.valueOf(100000000 + ++enhetCounter));
    journalenhet.setInnsynskravEpost("innsynskravepost@example.com");
    journalenhet.setEFormidling(true);

    var underenhet1 = new Enhet();
    underenhet1.setNavn("Testunderenhet 1");
    underenhet1.setLegacyId(UUID.randomUUID());
    underenhet1.setOpprettetDato(Date.from(Instant.now()));
    underenhet1.setOppdatertDato(Date.from(Instant.now()));
    underenhet1.setEnhetstype(Enhetstype.BYDEL);
    underenhet1.setParent(journalenhet);

    var underenhet2 = new Enhet();
    underenhet2.setNavn("Testunderenhet 2");
    underenhet2.setLegacyId(UUID.randomUUID());
    underenhet2.setOpprettetDato(Date.from(Instant.now()));
    underenhet2.setOppdatertDato(Date.from(Instant.now()));
    underenhet2.setEnhetstype(Enhetstype.UTVALG);
    underenhet2.setEnhetskode("UNDER");
    underenhet2.setParent(journalenhet);

    journalenhet.addUnderenhet(underenhet1);
    journalenhet.addUnderenhet(underenhet2);
    enhetRepository.saveAndFlush(journalenhet);

    var journalenhet2 = new Enhet();
    journalenhet2.setNavn("Journalenhet2");
    journalenhet2.setLegacyId(UUID.randomUUID());
    journalenhet2.setOpprettetDato(Date.from(Instant.now()));
    journalenhet2.setOppdatertDato(Date.from(Instant.now()));
    journalenhet2.setEnhetstype(Enhetstype.UTVALG);
    journalenhet2.setOrgnummer(String.valueOf(100000000 + ++enhetCounter));
    journalenhet2.setInnsynskravEpost("journalenhet2@example.com");
    journalenhet2.setEFormidling(true);
    enhetRepository.saveAndFlush(journalenhet2);

    EinnsynObjectService.TEMPORARY_ADM_ENHET_ID = journalenhet.getId();
    journalenhetId = journalenhet.getLegacyId();
    journalenhet2Id = journalenhet2.getLegacyId();
  }


  @AfterAll
  @Transactional
  public void _deleteBaseEnhets() {
    var journalenhet = enhetRepository.findById(journalenhetId).get();
    enhetService.delete(journalenhet.getId());
    var journalenhet2 = enhetRepository.findById(journalenhet2Id).get();
    enhetService.delete(journalenhet2.getId());
  }

}
