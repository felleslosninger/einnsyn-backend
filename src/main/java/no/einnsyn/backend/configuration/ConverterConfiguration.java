package no.einnsyn.backend.configuration;

import com.google.gson.Gson;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Converters used when binding request parameters and path variables. */
@Configuration
public class ConverterConfiguration implements WebMvcConfigurer {

  private final ObjectProvider<Gson> gsonProvider;

  public ConverterConfiguration(ObjectProvider<Gson> gsonProvider) {
    this.gsonProvider = gsonProvider;
  }

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(new StringToListConverter());
    registry.addConverter(new StringToExpandableFieldConverter(gsonProvider.getObject()));
  }

  static class StringToListConverter implements Converter<String, List<String>> {
    @Override
    public List<String> convert(String source) {
      return Collections.singletonList(source);
    }
  }
}
