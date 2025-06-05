package no.einnsyn.backend.logging;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import no.einnsyn.backend.authentication.EInnsynPrincipal;
import no.einnsyn.backend.authentication.EInnsynPrincipalBruker;
import no.einnsyn.backend.authentication.EInnsynPrincipalEnhet;
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
        if (principal instanceof EInnsynPrincipal eInnsynPrincipal) {
          MDC.put("authType", eInnsynPrincipal.getAuthType());
          MDC.put("authId", eInnsynPrincipal.getAuthId());
          MDC.put("authAsId", eInnsynPrincipal.getId());
        }
        if (principal instanceof EInnsynPrincipalEnhet) {
          MDC.put("authAsEntity", "Enhet");
        } else if (principal instanceof EInnsynPrincipalBruker) {
          MDC.put("authAsEntity", "Bruker");
        }
      }

      chain.doFilter(request, response);
    } finally {
      MDC.remove("authType");
      MDC.remove("authId");
      MDC.remove("authAsEntity");
      MDC.remove("authAsId");
    }
  }
}
