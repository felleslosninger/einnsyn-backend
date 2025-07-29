package no.einnsyn.backend.utils.id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import no.einnsyn.backend.entities.bruker.BrukerService;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

class IdResolverTest {

  private ApplicationContext applicationContext;
  private IdResolver idResolver;
  private BrukerService brukerService;
  private EnhetService enhetService;

  @BeforeEach
  void setUp() {
    applicationContext = mock(ApplicationContext.class);
    brukerService = mock(BrukerService.class);
    enhetService = mock(EnhetService.class);
    idResolver = new IdResolver(applicationContext);
  }

  @Test
  void testNullInput() {
    assertNull(idResolver.resolveToEInnsynId(null, BrukerDTO.class));
  }

  @Test
  void testEmptyInput() {
    assertEquals("", idResolver.resolveToEInnsynId("", BrukerDTO.class));
  }

  @Test
  void testAlreadyEInnsynId() {
    var eInnsynId = "bru_01hxyz123456789abcdefghij";
    assertEquals(eInnsynId, idResolver.resolveToEInnsynId(eInnsynId, BrukerDTO.class));
  }

  @Test
  void testEmailToEInnsynId() {
    var email = "test@example.com";
    var expectedEInnsynId = "bru_01hxyz123456789abcdefghij";
    
    when(applicationContext.getBean(BrukerService.class)).thenReturn(brukerService);
    when(brukerService.resolveId(email)).thenReturn(expectedEInnsynId);
    
    assertEquals(expectedEInnsynId, idResolver.resolveToEInnsynId(email, BrukerDTO.class));
    verify(brukerService).resolveId(email);
  }

  @Test
  void testOrgnummerToEInnsynId() {
    var orgnummer = "123456789";
    var expectedEInnsynId = "enh_01hxyz123456789abcdefghij";
    
    when(applicationContext.getBean(EnhetService.class)).thenReturn(enhetService);
    when(enhetService.resolveId(orgnummer)).thenReturn(expectedEInnsynId);
    
    assertEquals(expectedEInnsynId, idResolver.resolveToEInnsynId(orgnummer, EnhetDTO.class));
    verify(enhetService).resolveId(orgnummer);
  }

  @Test
  void testSystemIdToEInnsynId() {
    var systemId = "12345";
    var expectedEInnsynId = "enh_01hxyz123456789abcdefghij";
    
    when(applicationContext.getBean(EnhetService.class)).thenReturn(enhetService);
    when(enhetService.resolveId(systemId)).thenReturn(expectedEInnsynId);
    
    assertEquals(expectedEInnsynId, idResolver.resolveToEInnsynId(systemId, EnhetDTO.class));
    verify(enhetService).resolveId(systemId);
  }

  @Test
  void testNoResolutionFound() {
    var unknownId = "unknown123";
    
    when(applicationContext.getBean(BrukerService.class)).thenReturn(brukerService);
    when(brukerService.resolveId(unknownId)).thenReturn(null);
    
    assertEquals(unknownId, idResolver.resolveToEInnsynId(unknownId, BrukerDTO.class));
  }

  @Test
  void testServiceThrowsException() {
    var input = "test@example.com";
    
    when(applicationContext.getBean(BrukerService.class)).thenThrow(new RuntimeException("Service error"));
    
    // Should return original input if service throws exception
    assertEquals(input, idResolver.resolveToEInnsynId(input, BrukerDTO.class));
  }

  @Test
  void testValidTypeIdNotResolved() {
    var validTypeId = "jp_01hxyz123456789abcdefghij";
    
    // Should not attempt resolution for valid eInnsynIds
    assertEquals(validTypeId, idResolver.resolveToEInnsynId(validTypeId, BrukerDTO.class));
    verify(applicationContext, never()).getBean(BrukerService.class);
  }
}