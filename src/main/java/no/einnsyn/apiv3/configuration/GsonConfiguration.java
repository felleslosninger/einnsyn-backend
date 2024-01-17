package no.einnsyn.apiv3.configuration;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.expandablefield.ExpandableFieldDeserializer;
import no.einnsyn.apiv3.common.expandablefield.ExpandableFieldSerializer;
import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GsonConfiguration {

  @Bean
  GsonBuilderCustomizer registerCommonTypeAdapter() {
    return builder -> {
      builder.registerTypeAdapter(ExpandableField.class, new ExpandableFieldSerializer());
      builder.registerTypeAdapter(ExpandableField.class, new ExpandableFieldDeserializer());
      builder.registerTypeAdapter(LocalDate.class, new LocalDateSerializer());
      builder.registerTypeAdapter(LocalDate.class, new LocalDateDeserializer());
      builder.registerTypeAdapter(Instant.class, new InstantSerializer());
      builder.registerTypeAdapter(Instant.class, new InstantDeserializer());
    };
  }

  private class LocalDateSerializer implements JsonSerializer<LocalDate> {
    @Override
    public JsonElement serialize(LocalDate date, Type typeOfSrc, JsonSerializationContext context) {
      return new JsonPrimitive(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }
  }

  private class LocalDateDeserializer implements JsonDeserializer<LocalDate> {
    @Override
    public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      return LocalDate.parse(json.getAsString(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
  }

  public class InstantSerializer implements JsonSerializer<Instant> {
    @Override
    public JsonElement serialize(
        Instant instant, Type typeOfSrc, JsonSerializationContext context) {
      return new JsonPrimitive(instant.toString());
    }
  }

  public class InstantDeserializer implements JsonDeserializer<Instant> {
    @Override
    public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      return Instant.parse(json.getAsString());
    }
  }
}
