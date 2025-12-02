/**
 * Based on Stripe's ExpandableFieldDeserializer.java
 * https://github.com/stripe/stripe-java/blob/master/src/main/java/com/stripe/net/ExpandableFieldDeserializer.java
 */
package no.einnsyn.backend.configuration.typeadapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.hasid.HasId;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.utils.id.IdResolver;

public class ExpandableFieldDeserializer
    implements JsonDeserializer<ExpandableField<? extends HasId>> {

  private final IdResolver idResolver;

  public ExpandableFieldDeserializer(IdResolver idResolver) {
    this.idResolver = idResolver;
  }

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
      var jsonPrimitive = json.getAsJsonPrimitive();
      if (jsonPrimitive.isString()) {
        var inputId = jsonPrimitive.getAsString();

        // Get the entity class from the generic type parameter
        var entityClass = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
        var resolvedId = idResolver.resolveToEInnsynId(inputId, (Class<?>) entityClass);
        expandableField = new ExpandableField<>(resolvedId, null);
        return expandableField;
      } else {
        throw new JsonParseException("ExpandableField is a non-string primitive type.");
      }
      // Check if json is an expanded Object. If so, the field has been expanded, so we need to
      // serialize it into the proper typeOfT, and create an ExpandableField with both the String id
      // and this serialized object.
    } else if (json.isJsonObject()) {
      // Get the `id` out of the response
      var fieldAsJsonObject = json.getAsJsonObject();
      var idPrimitive = fieldAsJsonObject.getAsJsonPrimitive("id");
      var inputId = idPrimitive != null ? idPrimitive.getAsString() : null;
      // Get the entity class from the generic type parameter
      var entityClass = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
      var resolvedId =
          inputId != null ? idResolver.resolveToEInnsynId(inputId, (Class<?>) entityClass) : null;
      // We need to get the type inside the generic ExpandableField to make sure fromJson correctly
      // serializes the JsonObject:
      var clazz = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
      var object = (BaseDTO) context.deserialize(json, clazz);
      expandableField = new ExpandableField<>(resolvedId, object);

      return expandableField;
    }

    // If json is neither a String nor an Object, error. (We expect all expandable objects to fit
    // the known string-or-object design. If one doesn't, then something may have changed in the API
    // and this code may need to be updated.)
    throw new JsonParseException("ExpandableField is a non-object, non-primitive type.");
  }
}
