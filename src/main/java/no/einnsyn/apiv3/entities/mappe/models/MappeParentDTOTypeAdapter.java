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
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MappeParentDTOTypeAdapter {

  @Bean
  GsonBuilderCustomizer registerMappeParentDTOTypeAdapter() {
    return builder -> {
      builder.registerTypeAdapter(MappeParentDTO.class, new Serializer());
      builder.registerTypeAdapter(MappeParentDTO.class, new Deserializer());
    };
  }

  class Serializer implements JsonSerializer<MappeParentDTO> {

    @Override
    public JsonElement serialize(
        MappeParentDTO src, Type typeOfSrc, JsonSerializationContext context) {
      if (src.getSaksmappe() != null) {
        return context.serialize(src.getSaksmappe(), SaksmappeDTO.class);
      }
      if (src.getMoetemappe() != null) {
        return context.serialize(src.getMoetemappe(), MoetemappeDTO.class);
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

  class Deserializer implements JsonDeserializer<MappeParentDTO> {

    @Override
    public MappeParentDTO deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      if (json.isJsonNull()) {
        return null;
      }

      if (json.isJsonPrimitive()) {
        JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();
        if (jsonPrimitive.isString()) {
          return new MappeParentDTO(jsonPrimitive.getAsString());
        }
      }

      if (json.isJsonObject()) {
        JsonObject jsonObject = json.getAsJsonObject();
        String entity = jsonObject.get("entity").getAsString();
        switch (entity) {
          case "Saksmappe":
            SaksmappeDTO saksmappe = (SaksmappeDTO) context.deserialize(json, SaksmappeDTO.class);
            return new MappeParentDTO(saksmappe);
          case "Moetemappe":
            MoetemappeDTO moetemappe =
                (MoetemappeDTO) context.deserialize(json, MoetemappeDTO.class);
            return new MappeParentDTO(moetemappe);
          case "Arkiv":
            ArkivDTO arkiv = (ArkivDTO) context.deserialize(json, ArkivDTO.class);
            return new MappeParentDTO(arkiv);
          case "Arkivdel":
            ArkivdelDTO arkivdel = (ArkivdelDTO) context.deserialize(json, ArkivdelDTO.class);
            return new MappeParentDTO(arkivdel);
          case "Klasse":
            KlasseDTO klasse = (KlasseDTO) context.deserialize(json, KlasseDTO.class);
            return new MappeParentDTO(klasse);
          default:
        }
      }

      throw new JsonParseException("Could not deserialize MappeParentDTO");
    }
  }
}
