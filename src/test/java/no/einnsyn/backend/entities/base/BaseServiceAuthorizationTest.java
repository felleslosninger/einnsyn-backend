package no.einnsyn.backend.entities.base;

import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import no.einnsyn.backend.common.exceptions.models.AuthorizationException;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BaseServiceAuthorizationTest {

  /**
   * The authorization hooks in BaseService must deny by default, so a service that forgets to
   * override them can never accidentally expose an operation.
   */
  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void testDefaultAuthorizeHooksDeny() {
    BaseService service = Mockito.mock(BaseService.class, Mockito.CALLS_REAL_METHODS);

    assertThrowsExactly(
        AuthorizationException.class, () -> service.authorizeList(new ListParameters()));
    assertThrowsExactly(AuthorizationException.class, () -> service.authorizeGet("id"));
    assertThrowsExactly(AuthorizationException.class, () -> service.authorizeAdd(null));
    assertThrowsExactly(AuthorizationException.class, () -> service.authorizeUpdate("id", null));
    assertThrowsExactly(AuthorizationException.class, () -> service.authorizeDelete("id"));
  }
}
