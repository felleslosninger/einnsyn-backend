package no.einnsyn.apiv3.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.error.responses.ErrorResponse;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class EInnsynErrorController implements ErrorController {

  @GetMapping("/error")
  public ResponseEntity<ErrorResponse> error(HttpServletRequest request) {
    var status = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    var message = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
    var path = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
    log.warn("Error {} at {} with message {}", status, path, message);

    var error = new ErrorResponse(HttpStatus.valueOf(status), message, null, null);
    return ResponseEntity.status(status).body(error);
  }
}
