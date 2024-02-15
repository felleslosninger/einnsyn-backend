// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.korrespondansepart.models;

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
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KorrespondansepartParentDTOTypeAdapter {

  @Bean
  GsonBuilderCustomizer registerKorrespondansepartParentDTOTypeAdapter() {
    return builder -> {
      builder.registerTypeAdapter(KorrespondansepartParentDTO.class, new Serializer());
      builder.registerTypeAdapter(KorrespondansepartParentDTO.class, new Deserializer());
    };
  }

  class Serializer implements JsonSerializer<KorrespondansepartParentDTO> {

    @Override
    public JsonElement serialize(
        KorrespondansepartParentDTO src, Type typeOfSrc, JsonSerializationContext context) {
      if (src.getJournalpost() != null) {
        return context.serialize(src.getJournalpost(), JournalpostDTO.class);
      }
      if (src.getMoetedokument() != null) {
        return context.serialize(src.getMoetedokument(), MoetedokumentDTO.class);
      }
      if (src.getMoetesak() != null) {
        return context.serialize(src.getMoetesak(), MoetesakDTO.class);
      }
      return new JsonPrimitive(src.getId());
    }
  }

  class Deserializer implements JsonDeserializer<KorrespondansepartParentDTO> {

    @Override
    public KorrespondansepartParentDTO deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      if (json.isJsonNull()) {
        return null;
      }

      if (json.isJsonPrimitive()) {
        JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();
        if (jsonPrimitive.isString()) {
          return new KorrespondansepartParentDTO(jsonPrimitive.getAsString());
        }
      }

      if (json.isJsonObject()) {
        JsonObject jsonObject = json.getAsJsonObject();
        String entity = jsonObject.get("entity").getAsString();
        switch (entity) {
          case "Journalpost":
            JournalpostDTO journalpost = context.deserialize(json, JournalpostDTO.class);
            return new KorrespondansepartParentDTO(journalpost);
          case "Moetedokument":
            MoetedokumentDTO moetedokument = context.deserialize(json, MoetedokumentDTO.class);
            return new KorrespondansepartParentDTO(moetedokument);
          case "Moetesak":
            MoetesakDTO moetesak = context.deserialize(json, MoetesakDTO.class);
            return new KorrespondansepartParentDTO(moetesak);
          default:
        }
      }

      throw new JsonParseException("Could not deserialize KorrespondansepartParentDTO");
    }
  }
}
