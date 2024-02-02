// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.klasse.models;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelDTO;
import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KlasseParentDTOTypeAdapter {

  @Bean
  GsonBuilderCustomizer registerTypeAdapter() {
    return builder -> {
      builder.registerTypeAdapter(KlasseParentDTO.class, new Serializer());
      builder.registerTypeAdapter(KlasseParentDTO.class, new Deserializer());
    };
  }

  class Serializer implements JsonSerializer<KlasseParentDTO> {

    @Override
    public JsonElement serialize(
        KlasseParentDTO src, Type typeOfSrc, JsonSerializationContext context) {
      if (src.getArkivdel() != null) {
        return context.serialize(src.getArkivdel(), ArkivdelDTO.class);
      }
      if (src.getKlasse() != null) {
        return context.serialize(src.getKlasse(), KlasseDTO.class);
      }
      return new JsonPrimitive(src.getId());
    }
  }

  class Deserializer implements JsonDeserializer<KlasseParentDTO> {

    @Override
    public KlasseParentDTO deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      if (json.isJsonNull()) {
        return null;
      }

      if (json.isJsonPrimitive()) {
        JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();
        if (jsonPrimitive.isString()) {
          return new KlasseParentDTO(jsonPrimitive.getAsString());
        }
      }

      if (json.isJsonObject()) {
        JsonObject jsonObject = json.getAsJsonObject();
        String entity = jsonObject.get("entity").getAsString();
        switch (entity) {
          case "Arkivdel":
            ArkivdelDTO arkivdel = (ArkivdelDTO) context.deserialize(json, ArkivdelDTO.class);
            return new KlasseParentDTO(arkivdel);
          case "Klasse":
            KlasseDTO klasse = (KlasseDTO) context.deserialize(json, KlasseDTO.class);
            return new KlasseParentDTO(klasse);
          default:
        }
      }

      throw new JsonParseException("Could not deserialize KlasseParentDTO");
    }
  }
}
