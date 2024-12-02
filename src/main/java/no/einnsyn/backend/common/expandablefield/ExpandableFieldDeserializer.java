/**
 * Based on Stripe's ExpandableFieldDeserializer.java
 * https://github.com/stripe/stripe-java/blob/master/src/main/java/com/stripe/net/ExpandableFieldDeserializer.java
 */
package no.einnsyn.backend.common.expandablefield;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import no.einnsyn.backend.common.hasid.HasId;
import no.einnsyn.backend.entities.base.BaseService;
import no.einnsyn.backend.entities.base.models.Base;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.utils.idgenerator.IdValidator;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ExpandableFieldDeserializer
    implements JsonDeserializer<ExpandableField<? extends HasId>> {

  private final ApplicationContext applicationContext;

  public ExpandableFieldDeserializer(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
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

    // Check if json is a String ID. If so, the field has not been expanded, so we only need to
    // serialize a String and create a new ExpandableField with the String id only.
    if (json.isJsonPrimitive()) {
      var jsonPrimitive = json.getAsJsonPrimitive();
      if (jsonPrimitive.isString()) {
        var id = jsonPrimitive.getAsString();

        // Check if this is a valid id. If not, it might be another unique identifier, and we should
        // resolve the actual id.
        if (!IdValidator.isValid(id)) {
          id = resolveId(id, typeOfT);
        }

        return new ExpandableField<>(id);

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
      var id = idPrimitive != null ? idPrimitive.getAsString() : null;

      // We need to get the type inside the generic ExpandableField to make sure fromJson correctly
      // serializes the JsonObject:
      var clazz = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
      var object = (HasId) context.deserialize(json, clazz);

      return new ExpandableField<>(id, object);
    }

    // If json is neither a String nor an Object, error. (We expect all expandable objects to fit
    // the known string-or-object design. If one doesn't, then something may have changed in the API
    // and this code may need to be updated.)
    throw new JsonParseException("ExpandableField is a non-object, non-primitive type.");
  }

  private String resolveId(String identifier, Type typeOfT) {
    var parameterizedType = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
    if (parameterizedType instanceof Class clazz) {
      var service = getService(clazz);

      if (service != null) {
        var obj = service.findById(identifier);
        if (obj != null) {
          return obj.getId();
        }
      }
    }

    return identifier;
  }

  @SuppressWarnings("unchecked")
  private BaseService<? extends Base, ? extends BaseDTO> getService(Class<?> clazz) {
    if (clazz == null) {
      return null;
    }

    // Iterate over the interfaces implemented by the class
    for (var genericInterface : clazz.getGenericInterfaces()) {
      if (genericInterface instanceof ParameterizedType parameterizedType) {
        var rawType = parameterizedType.getRawType();

        // Check if the raw type matches BaseService
        if (rawType instanceof Class<?> classType
            && BaseService.class.isAssignableFrom(classType)) {
          // Validate the type arguments
          var typeArguments = parameterizedType.getActualTypeArguments();
          if (typeArguments.length == 2
              && typeArguments[0] instanceof Class<?> baseType
              && typeArguments[1] instanceof Class<?> dtoType) {

            // Check if they extend Base and BaseDTO, respectively
            if (Base.class.isAssignableFrom(baseType) && BaseDTO.class.isAssignableFrom(dtoType)) {
              // We've already checked types, so we can safely cast:
              return (BaseService<? extends Base, ? extends BaseDTO>)
                  applicationContext.getBean(classType);
            }
          }
        }
      }
    }

    return null;
  }
}
