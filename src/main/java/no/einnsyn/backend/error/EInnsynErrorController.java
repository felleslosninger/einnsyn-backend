package no.einnsyn.backend.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class EInnsynErrorController implements ErrorController {

  @RequestMapping("/error")
  @ExceptionHandler
  public ResponseEntity<String> handleError(HttpServletRequest request) {
    var status = getStatus(request);
    var message = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
    return ResponseEntity.status(status).body(message);
  }

  private HttpStatus getStatus(HttpServletRequest request) {
    Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    if (statusCode == null) {
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }
    return HttpStatus.valueOf(statusCode);
  }
}
