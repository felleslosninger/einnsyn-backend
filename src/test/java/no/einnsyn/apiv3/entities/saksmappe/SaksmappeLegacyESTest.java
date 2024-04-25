package no.einnsyn.apiv3.entities.saksmappe;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.core.IndexRequest;
import com.google.gson.Gson;
import java.util.List;
import java.util.function.Function;
import no.einnsyn.apiv3.EinnsynLegacyElasticTestBase;
import no.einnsyn.apiv3.authentication.apikey.ApiKeyUserDetails;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.base.models.BaseES;
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
class SaksmappeLegacyESTest extends EinnsynLegacyElasticTestBase {

  @Mock Authentication authentication;
  @Mock SecurityContext securityContext;
  @Autowired protected Gson gson;

  @Captor ArgumentCaptor<Function> builderCaptor;
  @Captor ArgumentCaptor<BaseES> documentCaptor;
  @Mock IndexRequest.Builder<BaseES> builderMock;

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
  void testSaksmappeES() throws Exception {

    var journalpostRequestDTO = getJournalpostDTO();
    var skjermingRequestDTO = getSkjermingDTO();
    journalpostRequestDTO.setSkjerming(new ExpandableField<SkjermingDTO>(skjermingRequestDTO));
    journalpostRequestDTO.setKorrespondansepart(
        List.of(new ExpandableField<>(getKorrespondansepartDTO())));
    journalpostRequestDTO.setDokumentbeskrivelse(
        List.of(new ExpandableField<>(getDokumentbeskrivelseDTO())));

    var saksmappeRequestDTO = getSaksmappeDTO();
    saksmappeRequestDTO.setJournalpost(List.of(new ExpandableField<>(journalpostRequestDTO)));
    var saksmappeDTO = saksmappeService.add(saksmappeRequestDTO);

    // Verify the document
    verify(esClient, times(3)).index(builderCaptor.capture());
    var builder = builderCaptor.getValue();
    builder.apply(builderMock);
    verify(builderMock).document(documentCaptor.capture());

    var saksmappeES = documentCaptor.getValue();

    compareSaksmappe(saksmappeDTO, saksmappeES);

    // Clean up
    var deleted = saksmappeService.delete(saksmappeDTO.getId());
    assertTrue(deleted.getDeleted());
    assertNull(saksmappeRepository.findById(saksmappeDTO.getId()).orElse(null));
  }
}
