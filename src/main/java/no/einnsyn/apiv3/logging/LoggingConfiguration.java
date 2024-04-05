package no.einnsyn.apiv3.logging;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class LoggingConfiguration {

  @Bean
  FilterRegistrationBean<AuthLoggingFilter> authLoggingFilter() {
    FilterRegistrationBean<AuthLoggingFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new AuthLoggingFilter());
    registrationBean.addUrlPatterns("/*");
    return registrationBean;
  }

  @Bean
  FilterRegistrationBean<EndpointLoggingFilter> endpointLoggingFilter() {
    FilterRegistrationBean<EndpointLoggingFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new EndpointLoggingFilter());
    registrationBean.addUrlPatterns("/*");
    return registrationBean;
  }

  @Bean
  CommonsRequestLoggingFilter requestLoggingFilter() {
    CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
    loggingFilter.setIncludeClientInfo(true);
    loggingFilter.setIncludeQueryString(true);
    loggingFilter.setIncludePayload(true);
    loggingFilter.setIncludeHeaders(false);
    return loggingFilter;
  }
}
