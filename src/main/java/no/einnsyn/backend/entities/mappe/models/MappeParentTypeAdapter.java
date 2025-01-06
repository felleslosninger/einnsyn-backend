// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.entities.mappe.models;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.klasse.models.KlasseDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MappeParentTypeAdapter {

  @Bean
  public GsonBuilderCustomizer registerMappeParentTypeAdapter() {
    return builder -> {
      builder.registerTypeAdapter(MappeParent.class, new Serializer());
      builder.registerTypeAdapter(MappeParent.class, new Deserializer());
    };
  }

  public class Serializer implements JsonSerializer<MappeParent> {

    @Override
    public JsonElement serialize(
        MappeParent src, Type typeOfSrc, JsonSerializationContext context) {
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

  public class Deserializer implements JsonDeserializer<MappeParent> {

    @Override
    public MappeParent deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context) {
      if (json.isJsonNull()) {
        return null;
      }

      if (json.isJsonPrimitive()) {
        var jsonPrimitive = json.getAsJsonPrimitive();
        if (jsonPrimitive.isString()) {
          return new MappeParent(jsonPrimitive.getAsString());
        }
      }

      if (json.isJsonObject()) {
        var jsonObject = json.getAsJsonObject();
        var entity = jsonObject.get("entity").getAsString();
        switch (entity) {
          case "Saksmappe":
            SaksmappeDTO Saksmappe = context.deserialize(jsonObject, SaksmappeDTO.class);
            return new MappeParent(Saksmappe);
          case "Moetemappe":
            MoetemappeDTO Moetemappe = context.deserialize(jsonObject, MoetemappeDTO.class);
            return new MappeParent(Moetemappe);
          case "Arkiv":
            ArkivDTO Arkiv = context.deserialize(jsonObject, ArkivDTO.class);
            return new MappeParent(Arkiv);
          case "Arkivdel":
            ArkivdelDTO Arkivdel = context.deserialize(jsonObject, ArkivdelDTO.class);
            return new MappeParent(Arkivdel);
          case "Klasse":
            KlasseDTO Klasse = context.deserialize(jsonObject, KlasseDTO.class);
            return new MappeParent(Klasse);
        }
      }

      throw new JsonParseException("Cannot deserialize MappeParent " + json);
    }
  }
}
