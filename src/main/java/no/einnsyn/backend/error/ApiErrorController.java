package no.einnsyn.backend.error;

import com.google.gson.Gson;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import no.einnsyn.backend.common.exceptions.models.AuthenticationException;
import no.einnsyn.backend.common.exceptions.models.AuthorizationException;
import no.einnsyn.backend.common.exceptions.models.BadRequestException;
import no.einnsyn.backend.common.exceptions.models.ConflictException;
import no.einnsyn.backend.common.exceptions.models.InternalServerErrorException;
import no.einnsyn.backend.common.exceptions.models.MethodNotAllowedException;
import no.einnsyn.backend.common.exceptions.models.NotFoundException;
import no.einnsyn.backend.common.exceptions.models.TooManyUnverifiedOrdersException;
import no.einnsyn.backend.common.responses.models.ErrorResponse;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiErrorController implements ErrorController {

  private final Gson gson;

  public ApiErrorController(Gson gson) {
    this.gson = gson;
  }

  @RequestMapping("/error")
  public void error(HttpServletRequest request, HttpServletResponse response) throws IOException {
    var status = resolveStatus(request);
    var path = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
    var message = resolveMessage(status, path, request);
    var errorResponse = buildErrorResponse(status, message);

    // This is a JSON API. Never let /error fall back to view resolution.
    response.setStatus(status.value());
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write(gson.toJson(errorResponse));
  }

  private HttpStatus resolveStatus(HttpServletRequest request) {
    var statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    if (statusCode instanceof Integer integerStatus) {
      var status = HttpStatus.resolve(integerStatus);
      if (status != null) {
        return status;
      }
    }
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }

  private String resolveMessage(HttpStatus status, String path, HttpServletRequest request) {
    var errorMessage = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
    if (errorMessage instanceof String message && !message.isBlank()) {
      return message;
    }

    if (status == HttpStatus.NOT_FOUND && StringUtils.hasText(path)) {
      return "No handler found: " + path;
    }

    return status.getReasonPhrase();
  }

  private ErrorResponse buildErrorResponse(HttpStatus status, String message) {
    return switch (status) {
      case BAD_REQUEST -> new BadRequestException.ClientResponse(message);
      case UNAUTHORIZED -> new AuthenticationException.ClientResponse(message);
      case FORBIDDEN -> new AuthorizationException.ClientResponse(message);
      case NOT_FOUND -> new NotFoundException.ClientResponse(message);
      case METHOD_NOT_ALLOWED -> new MethodNotAllowedException.ClientResponse(message);
      case CONFLICT -> new ConflictException.ClientResponse(message);
      case TOO_MANY_REQUESTS -> new TooManyUnverifiedOrdersException.ClientResponse(message);
      default -> new InternalServerErrorException.ClientResponse(message);
    };
  }
}
