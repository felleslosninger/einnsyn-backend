package no.einnsyn.backend.configuration;

import java.util.Collections;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StringListConverterConfig implements WebMvcConfigurer {

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(new StringToListConverter());
  }

  static class StringToListConverter implements Converter<String, List<String>> {
    @Override
    public List<String> convert(String source) {
      return Collections.singletonList(source);
    }
  }
}
