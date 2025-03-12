package no.einnsyn.backend.configuration;

import no.einnsyn.backend.tasks.handlers.index.ElasticsearchHandlerInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ElasticsearchConfiguration implements WebMvcConfigurer {

  private final ElasticsearchHandlerInterceptor esInterceptor;

  public ElasticsearchConfiguration(ElasticsearchHandlerInterceptor esInterceptor) {
    this.esInterceptor = esInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(esInterceptor);
  }
}
