package no.einnsyn.apiv3.logging;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import no.einnsyn.apiv3.authentication.apikey.models.ApiKeyUserDetails;
import no.einnsyn.apiv3.authentication.bruker.models.BrukerUserDetails;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthLoggingFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    try {
      // Append auth info to all log messages
      var authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null) {
        var principal = authentication.getPrincipal();
        if (principal instanceof BrukerUserDetails brukerUserDetails) {
          MDC.put("authType", "bruker");
          MDC.put("authId", brukerUserDetails.getId());
        } else if (principal instanceof ApiKeyUserDetails apiKeyUserDetails) {
          MDC.put("authType", "apiKey");
          MDC.put("authId", apiKeyUserDetails.getId());
          MDC.put("authEnhetId", apiKeyUserDetails.getEnhetId());
        }
      }

      chain.doFilter(request, response);
    } finally {
      MDC.remove("authType");
      MDC.remove("authId");
      MDC.remove("authEnhetId");
    }
  }
}
