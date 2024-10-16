package no.einnsyn.apiv3.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.expandablefield.ExpandableFieldDeserializer;
import no.einnsyn.apiv3.common.expandablefield.ExpandableFieldSerializer;
import no.einnsyn.apiv3.entities.base.BaseDTOTypeAdapterFactory;
import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class GsonConfiguration {

  @Bean
  @Primary
  GsonBuilderCustomizer registerCommonTypeAdapter() {
    return builder -> {
      builder.registerTypeAdapterFactory(new BaseDTOTypeAdapterFactory());
      builder.registerTypeAdapter(ExpandableField.class, new ExpandableFieldSerializer());
      builder.registerTypeAdapter(ExpandableField.class, new ExpandableFieldDeserializer());
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
