// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.search.models;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SearchSearchResponseDTOTypeAdapter {

  @Bean
  GsonBuilderCustomizer registerSearchSearchResponseDTOTypeAdapter() {
    return builder -> {
      builder.registerTypeAdapter(SearchSearchResponseDTO.class, new Serializer());
      builder.registerTypeAdapter(SearchSearchResponseDTO.class, new Deserializer());
    };
  }

  class Serializer implements JsonSerializer<SearchSearchResponseDTO> {

    @Override
    public JsonElement serialize(
        SearchSearchResponseDTO src, Type typeOfSrc, JsonSerializationContext context) {
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

  class Deserializer implements JsonDeserializer<SearchSearchResponseDTO> {

    @Override
    public SearchSearchResponseDTO deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      if (json.isJsonNull()) {
        return null;
      }

      if (json.isJsonPrimitive()) {
        JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();
        if (jsonPrimitive.isString()) {
          return new SearchSearchResponseDTO(jsonPrimitive.getAsString());
        }
      }

      if (json.isJsonObject()) {
        JsonObject jsonObject = json.getAsJsonObject();
        String entity = jsonObject.get("entity").getAsString();
        switch (entity) {
          case "Journalpost":
            JournalpostDTO journalpost = context.deserialize(json, JournalpostDTO.class);
            return new SearchSearchResponseDTO(journalpost);
          case "Moetemappe":
            MoetemappeDTO moetemappe = context.deserialize(json, MoetemappeDTO.class);
            return new SearchSearchResponseDTO(moetemappe);
          case "Moetesak":
            MoetesakDTO moetesak = context.deserialize(json, MoetesakDTO.class);
            return new SearchSearchResponseDTO(moetesak);
          case "Saksmappe":
            SaksmappeDTO saksmappe = context.deserialize(json, SaksmappeDTO.class);
            return new SearchSearchResponseDTO(saksmappe);
          default:
        }
      }

      throw new JsonParseException("Could not deserialize SearchSearchResponseDTO");
    }
  }
}
