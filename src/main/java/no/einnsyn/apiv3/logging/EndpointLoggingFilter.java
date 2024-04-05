package no.einnsyn.apiv3.logging;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.slf4j.MDC;

public class EndpointLoggingFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    try {
      var req = (HttpServletRequest) request;
      MDC.put("method", req.getMethod());
      MDC.put("endpoint", req.getRequestURI());

      chain.doFilter(request, response);
    } finally {
      MDC.remove("method");
      MDC.remove("endpoint");
    }
  }
}
