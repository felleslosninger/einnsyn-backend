// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.search.models;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UnionResourceSearchTypeAdapter {

  @Bean
  GsonBuilderCustomizer registerTypeAdapter() {
    return builder -> {
      builder.registerTypeAdapter(UnionResourceSearch.class, new Serializer());
      builder.registerTypeAdapter(UnionResourceSearch.class, new Deserializer());
    };
  }

  class Serializer implements JsonSerializer<UnionResourceSearch> {

    @Override
    public JsonElement serialize(
        UnionResourceSearch src, Type typeOfSrc, JsonSerializationContext context) {
      if (src.getJournalpost() != null) {
        return context.serialize(src.getJournalpost(), JournalpostDTO.class);
      }
      if (src.getMoetemappe() != null) {
        return context.serialize(src.getMoetemappe(), MoetemappeDTO.class);
      }
      if (src.getMoetesak() != null) {
        return context.serialize(src.getMoetesak(), MoetesakDTO.class);
      }
      if (src.getSaksmappe() != null) {
        return context.serialize(src.getSaksmappe(), SaksmappeDTO.class);
      }
      return new JsonPrimitive(src.getId());
    }
  }

  class Deserializer implements JsonDeserializer<UnionResourceSearch> {

    @Override
    public UnionResourceSearch deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      if (json.isJsonNull()) {
        return null;
      }

      if (json.isJsonPrimitive()) {
        JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();
        if (jsonPrimitive.isString()) {
          return new UnionResourceSearch(jsonPrimitive.getAsString());
        }
      }

      if (json.isJsonObject()) {
        JsonObject jsonObject = json.getAsJsonObject();
        String entity = jsonObject.get("entity").getAsString();
        switch (entity) {
          case "Journalpost":
            return context.deserialize(json, JournalpostDTO.class);
          case "Moetemappe":
            return context.deserialize(json, MoetemappeDTO.class);
          case "Moetesak":
            return context.deserialize(json, MoetesakDTO.class);
          case "Saksmappe":
            return context.deserialize(json, SaksmappeDTO.class);
          default:
        }
      }

      throw new JsonParseException("Could not deserialize UnionResourceParent");
    }
  }
}
