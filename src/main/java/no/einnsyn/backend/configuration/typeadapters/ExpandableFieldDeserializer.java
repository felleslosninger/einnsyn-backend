/**
 * Based on Stripe's ExpandableFieldDeserializer.java
 * https://github.com/stripe/stripe-java/blob/master/src/main/java/com/stripe/net/ExpandableFieldDeserializer.java
 */
package no.einnsyn.backend.configuration.typeadapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.hasid.HasId;
import no.einnsyn.backend.entities.base.models.BaseDTO;

public class ExpandableFieldDeserializer
    implements JsonDeserializer<ExpandableField<? extends HasId>> {

  /**
   * Deserializes an expandable field JSON payload (i.e. either a string with just the ID, or a full
   * JSON object) into an {@link ExpandableField} object.
   */
  @Override
  public ExpandableField<? extends HasId> deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    if (json.isJsonNull()) {
      return null;
    }

    ExpandableField<? extends HasId> expandableField;

    // Check if json is a String ID. If so, the field has not been expanded, so we only need to
    // serialize a String and create a new ExpandableField with the String id only.
    if (json.isJsonPrimitive()) {
      JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();
      if (jsonPrimitive.isString()) {
        expandableField = new ExpandableField<>(jsonPrimitive.getAsString(), null);
        return expandableField;
      } else {
        throw new JsonParseException("ExpandableField is a non-string primitive type.");
      }
      // Check if json is an expanded Object. If so, the field has been expanded, so we need to
      // serialize it into the proper typeOfT, and create an ExpandableField with both the String id
      // and this serialized object.
    } else if (json.isJsonObject()) {
      // Get the `id` out of the response
      JsonObject fieldAsJsonObject = json.getAsJsonObject();
      JsonPrimitive idPrimitive = fieldAsJsonObject.getAsJsonPrimitive("id");
      String id = idPrimitive != null ? idPrimitive.getAsString() : null;
      // We need to get the type inside the generic ExpandableField to make sure fromJson correctly
      // serializes the JsonObject:
      Type clazz = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
      BaseDTO object = (BaseDTO) context.deserialize(json, clazz);
      expandableField = new ExpandableField<>(id, object);

      return expandableField;
    }

    // If json is neither a String nor an Object, error. (We expect all expandable objects to fit
    // the known string-or-object design. If one doesn't, then something may have changed in the API
    // and this code may need to be updated.)
    throw new JsonParseException("ExpandableField is a non-object, non-primitive type.");
  }
}
