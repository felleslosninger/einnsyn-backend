// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.mappe.models;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.apiv3.entities.klasse.models.KlasseDTO;
import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UnionResourceParentTypeAdapter {

  @Bean
  GsonBuilderCustomizer registerTypeAdapter() {
    return builder -> {
      builder.registerTypeAdapter(UnionResourceParent.class, new Serializer());
      builder.registerTypeAdapter(UnionResourceParent.class, new Deserializer());
    };
  }

  class Serializer implements JsonSerializer<UnionResourceParent> {

    @Override
    public JsonElement serialize(
        UnionResourceParent src, Type typeOfSrc, JsonSerializationContext context) {
      if (src.getMappe() != null) {
        return context.serialize(src.getMappe(), MappeDTO.class);
      }
      if (src.getArkiv() != null) {
        return context.serialize(src.getArkiv(), ArkivDTO.class);
      }
      if (src.getArkivdel() != null) {
        return context.serialize(src.getArkivdel(), ArkivdelDTO.class);
      }
      if (src.getKlasse() != null) {
        return context.serialize(src.getKlasse(), KlasseDTO.class);
      }
      return new JsonPrimitive(src.getId());
    }
  }

  class Deserializer implements JsonDeserializer<UnionResourceParent> {

    @Override
    public UnionResourceParent deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      if (json.isJsonNull()) {
        return null;
      }

      if (json.isJsonPrimitive()) {
        JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();
        if (jsonPrimitive.isString()) {
          return new UnionResourceParent(jsonPrimitive.getAsString());
        }
      }

      if (json.isJsonObject()) {
        JsonObject jsonObject = json.getAsJsonObject();
        String entity = jsonObject.get("entity").getAsString();
        switch (entity) {
          case "Mappe":
            return context.deserialize(json, MappeDTO.class);
          case "Arkiv":
            return context.deserialize(json, ArkivDTO.class);
          case "Arkivdel":
            return context.deserialize(json, ArkivdelDTO.class);
          case "Klasse":
            return context.deserialize(json, KlasseDTO.class);
          default:
        }
      }

      throw new JsonParseException("Could not deserialize UnionResourceParent");
    }
  }
}
