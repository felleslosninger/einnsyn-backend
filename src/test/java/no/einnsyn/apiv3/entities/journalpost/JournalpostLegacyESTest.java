package no.einnsyn.apiv3.entities.journalpost;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.core.IndexRequest;
import com.google.gson.Gson;
import java.util.List;
import java.util.function.Function;
import no.einnsyn.apiv3.EinnsynLegacyElasticTestBase;
import no.einnsyn.apiv3.authentication.apikey.ApiKeyUserDetails;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostES;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.apiv3.entities.skjerming.models.SkjermingDTO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@SuppressWarnings({"rawtypes", "unchecked"})
class JournalpostLegacyESTest extends EinnsynLegacyElasticTestBase {

  @Mock Authentication authentication;
  @Mock SecurityContext securityContext;
  @Autowired protected Gson gson;

  @Captor ArgumentCaptor<Function> builderCaptor;
  @Captor ArgumentCaptor<JournalpostES> documentCaptor;
  @Mock IndexRequest.Builder<JournalpostES> builderMock;

  SaksmappeDTO saksmappeDTO;
  String saksaar;
  String saksaarShort;
  String sakssekvensnummer;

  @BeforeAll
  void setUp() throws Exception {
    var apiKey = apiKeyService.findById(adminKey);
    var apiKeyUserDetails = new ApiKeyUserDetails(apiKey);
    when(authentication.getPrincipal()).thenReturn(apiKeyUserDetails);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    saksmappeDTO = saksmappeService.add(getSaksmappeDTO());
    saksaar = String.valueOf(saksmappeDTO.getSaksaar());
    saksaarShort = saksaar.substring(2);
    sakssekvensnummer = String.valueOf(saksmappeDTO.getSakssekvensnummer());

    // Prepare mock
    when(builderMock.index(anyString())).thenReturn(builderMock);
    when(builderMock.id(anyString())).thenReturn(builderMock);
    when(builderMock.document(any())).thenReturn(builderMock);
  }

  @AfterAll
  void tearDown() throws Exception {
    saksmappeService.delete(saksmappeDTO.getId());
  }

  @BeforeEach
  void resetMocks() {
    reset(esClient);
  }

  @Test
  void testJournalpostES() throws Exception {
    var journalpostRequestDTO = getJournalpostDTO();

    var skjermingRequestDTO = getSkjermingDTO();
    journalpostRequestDTO.setSkjerming(new ExpandableField<SkjermingDTO>(skjermingRequestDTO));

    var korrespondansepart1RequestDTO = getKorrespondansepartDTO();
    var korrespondansepart2RequestDTO = getKorrespondansepartDTO();
    korrespondansepart2RequestDTO.setKorrespondansepartNavn("test 2 navn");
    korrespondansepart2RequestDTO.setKorrespondansepartNavnSensitiv("test 2 navn sensitiv");
    korrespondansepart2RequestDTO.setErBehandlingsansvarlig(true);
    korrespondansepart2RequestDTO.setKorrespondanseparttype("avsender");
    korrespondansepart2RequestDTO.setEpostadresse("epost@example.com");
    korrespondansepart2RequestDTO.setAdministrativEnhet("https://testAdmEnhet2");
    journalpostRequestDTO.setKorrespondansepart(
        List.of(
            new ExpandableField<>(korrespondansepart1RequestDTO),
            new ExpandableField<>(korrespondansepart2RequestDTO)));

    var dokumentbeskrivelse1RequestDTO = getDokumentbeskrivelseDTO();
    var dokumentbeskrivelse2RequestDTO = getDokumentbeskrivelseDTO();
    journalpostRequestDTO.setDokumentbeskrivelse(
        List.of(
            new ExpandableField<>(dokumentbeskrivelse1RequestDTO),
            new ExpandableField<>(dokumentbeskrivelse2RequestDTO)));

    var journalpostDTO =
        saksmappeService.addJournalpost(saksmappeDTO.getId(), journalpostRequestDTO);

    // Verify the document
    verify(esClient).index(builderCaptor.capture());
    var builder = builderCaptor.getValue();
    builder.apply(builderMock);
    verify(builderMock).document(documentCaptor.capture());
    var journalpostES = documentCaptor.getValue();

    compareJournalpost(journalpostDTO, journalpostES);

    // Clean up
    var deletedJournalpost = journalpostService.delete(journalpostDTO.getId());
    assertTrue(deletedJournalpost.getDeleted());
    assertNull(journalpostRepository.findById(journalpostDTO.getId()).orElse(null));
  }
}
