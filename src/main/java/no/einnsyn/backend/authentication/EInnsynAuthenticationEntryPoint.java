package no.einnsyn.backend.authentication;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EInnsynAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final Gson gson;

  public EInnsynAuthenticationEntryPoint(Gson gson) {
    this.gson = gson;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {

    log.warn(
        "Authentication failed for request {}: {} - Reason: {}",
        request.getMethod(),
        request.getRequestURI(),
        authException.getMessage());
    log.debug("AuthenticationException details:", authException);

    var errorMessage = "Authentication failed";

    var errorResponse =
        new no.einnsyn.backend.common.exceptions.models.AuthenticationException.ClientResponse(
            errorMessage);
    var errorResponseJson = gson.toJson(errorResponse);

    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getOutputStream().println(errorResponseJson);
  }
}
