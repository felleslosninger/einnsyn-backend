package no.einnsyn.backend.error;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import no.einnsyn.backend.error.exceptions.UnauthorizedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class EInnsynAuthenticationFailureHandler implements AuthenticationFailureHandler {

  private final Gson gson;

  public EInnsynAuthenticationFailureHandler(Gson gson) {
    this.gson = gson;
  }

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException, ServletException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    var unauthorizedException = new UnauthorizedException(exception.getMessage(), request);
    String message = gson.toJson(unauthorizedException);
    response.getWriter().write(message);
  }
}
