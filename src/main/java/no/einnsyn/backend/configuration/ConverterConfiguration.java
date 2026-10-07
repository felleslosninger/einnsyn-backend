package no.einnsyn.backend.configuration;

import com.google.gson.Gson;
import java.util.Collections;
import java.util.List;
import no.einnsyn.backend.validation.unknownparameters.UnknownQueryParametersInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Converters and interceptors used when binding request parameters and path variables. */
@Configuration
public class ConverterConfiguration implements WebMvcConfigurer {

  private final ObjectProvider<Gson> gsonProvider;
  private final UnknownQueryParametersInterceptor unknownQueryParametersInterceptor;

  public ConverterConfiguration(
      ObjectProvider<Gson> gsonProvider,
      UnknownQueryParametersInterceptor unknownQueryParametersInterceptor) {
    this.gsonProvider = gsonProvider;
    this.unknownQueryParametersInterceptor = unknownQueryParametersInterceptor;
  }

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(new StringToListConverter());
    registry.addConverter(new StringToExpandableFieldConverter(gsonProvider.getObject()));
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(unknownQueryParametersInterceptor);
  }

  static class StringToListConverter implements Converter<String, List<String>> {
    @Override
    public List<String> convert(String source) {
      return Collections.singletonList(source);
    }
  }
}
