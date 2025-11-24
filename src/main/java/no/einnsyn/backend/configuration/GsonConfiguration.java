package no.einnsyn.backend.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.configuration.typeadapters.BaseDTOTypeAdapterFactory;
import no.einnsyn.backend.configuration.typeadapters.ExpandableFieldDeserializer;
import no.einnsyn.backend.configuration.typeadapters.ExpandableFieldSerializer;
import no.einnsyn.backend.configuration.typeadapters.NoUnknownPropertiesTypeAdapterFactory;
import no.einnsyn.backend.utils.id.IdResolver;
import org.springframework.boot.gson.autoconfigure.GsonBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class GsonConfiguration {

  @Bean
  @Primary
  GsonBuilderCustomizer customizeGson(IdResolver idResolver) {
    return builder -> {
      builder.registerTypeAdapterFactory(new BaseDTOTypeAdapterFactory());
      builder.registerTypeAdapter(ExpandableField.class, new ExpandableFieldSerializer());
      builder.registerTypeAdapter(
          ExpandableField.class, new ExpandableFieldDeserializer(idResolver));
    };
  }

  @Bean
  GsonBuilderCustomizer denyUnknownProperties() {
    return builder -> {
      builder.registerTypeAdapterFactory(new NoUnknownPropertiesTypeAdapterFactory());
      builder.registerTypeAdapterFactory(new NoUnknownPropertiesTypeAdapterFactory());
    };
  }

  @Bean("pretty")
  @Primary
  Gson gsonPrettyPrinting(List<GsonBuilderCustomizer> customizers) {
    var builder = new GsonBuilder();
    builder.setPrettyPrinting();
    for (var customizer : customizers) {
      customizer.customize(builder);
    }
    return builder.create();
  }

  @Bean("compact")
  Gson gsonCompact(List<GsonBuilderCustomizer> customizers) {
    var builder = new GsonBuilder();
    for (var customizer : customizers) {
      customizer.customize(builder);
    }
    return builder.create();
  }

  @Bean("gsonPrettyAllowUnknown")
  Gson gsonPrettyAllowUnknown(GsonBuilderCustomizer customizer) {
    var builder = new GsonBuilder();
    customizer.customize(builder);
    builder.setPrettyPrinting();
    return builder.create();
  }

  @Bean("gsonCompactAllowUnknown")
  Gson gsonCompactAllowUnknown(GsonBuilderCustomizer customizer) {
    var builder = new GsonBuilder();
    customizer.customize(builder);
    return builder.create();
  }
}
