// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.entities.korrespondansepart.models;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.backend.entities.moetesak.models.MoetesakDTO;
import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KorrespondansepartParentTypeAdapter {

  @Bean
  public GsonBuilderCustomizer registerKorrespondansepartParentTypeAdapter() {
    return builder -> {
      builder.registerTypeAdapter(KorrespondansepartParent.class, new Serializer());
      builder.registerTypeAdapter(KorrespondansepartParent.class, new Deserializer());
    };
  }

  public class Serializer implements JsonSerializer<KorrespondansepartParent> {

    @Override
    public JsonElement serialize(
        KorrespondansepartParent src, Type typeOfSrc, JsonSerializationContext context) {
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

  public class Deserializer implements JsonDeserializer<KorrespondansepartParent> {

    @Override
    public KorrespondansepartParent deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context) {
      if (json.isJsonNull()) {
        return null;
      }

      if (json.isJsonPrimitive()) {
        var jsonPrimitive = json.getAsJsonPrimitive();
        if (jsonPrimitive.isString()) {
          return new KorrespondansepartParent(jsonPrimitive.getAsString());
        }
      }

      if (json.isJsonObject()) {
        var jsonObject = json.getAsJsonObject();
        var entity = jsonObject.get("entity").getAsString();
        switch (entity) {
          case "Journalpost":
            JournalpostDTO Journalpost = context.deserialize(jsonObject, JournalpostDTO.class);
            return new KorrespondansepartParent(Journalpost);
          case "Moetedokument":
            MoetedokumentDTO Moetedokument =
                context.deserialize(jsonObject, MoetedokumentDTO.class);
            return new KorrespondansepartParent(Moetedokument);
          case "Moetesak":
            MoetesakDTO Moetesak = context.deserialize(jsonObject, MoetesakDTO.class);
            return new KorrespondansepartParent(Moetesak);
        }
      }

      throw new JsonParseException("Cannot deserialize KorrespondansepartParent " + json);
    }
  }
}
