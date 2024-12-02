package no.einnsyn.backend.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.expandablefield.ExpandableFieldDeserializer;
import no.einnsyn.backend.common.expandablefield.ExpandableFieldSerializer;
import no.einnsyn.backend.entities.base.BaseDTOTypeAdapterFactory;
import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class GsonConfiguration {

  private final ExpandableFieldDeserializer expandableFieldDeserializer;
  private final ExpandableFieldSerializer expandableFieldSerializer;

  public GsonConfiguration(
      ExpandableFieldDeserializer expandableFieldDeserializer,
      ExpandableFieldSerializer expandableFieldSerializer) {
    this.expandableFieldDeserializer = expandableFieldDeserializer;
    this.expandableFieldSerializer = expandableFieldSerializer;
  }

  @Bean
  @Primary
  GsonBuilderCustomizer registerCommonTypeAdapter() {
    return builder -> {
      builder.registerTypeAdapterFactory(new BaseDTOTypeAdapterFactory());
      builder.registerTypeAdapter(ExpandableField.class, expandableFieldDeserializer);
      builder.registerTypeAdapter(ExpandableField.class, expandableFieldSerializer);
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
}
