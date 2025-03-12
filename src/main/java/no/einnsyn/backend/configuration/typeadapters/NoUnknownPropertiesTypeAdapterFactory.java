package no.einnsyn.backend.configuration.typeadapters;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import no.einnsyn.backend.common.exceptions.models.BadRequestException;
import no.einnsyn.backend.common.queryparameters.models.QueryParameters;
import no.einnsyn.backend.entities.base.models.BaseDTO;

/**
 * A TypeAdapterFactory that enforces strict deserialization of JSON objects, ensuring that only
 * fields defined in the target type are present in the JSON object.
 */
public class NoUnknownPropertiesTypeAdapterFactory implements TypeAdapterFactory {

  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {

    // Only process instances of BaseDTO
    if (!BaseDTO.class.isAssignableFrom(type.getRawType())
        && !QueryParameters.class.isAssignableFrom(type.getRawType())) {
      return null;
    }

    // Get the default delegate adapter.
    final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);

    return new TypeAdapter<T>() {
      @Override
      public void write(JsonWriter out, T value) throws IOException {
        delegate.write(out, value);
      }

      @Override
      public T read(JsonReader in) throws JsonParseException {

        // Parse the JSON into a JsonElement for inspection.
        var jsonElement = Streams.parse(in);
        var targetClass = type.getRawType();
        var expectedFields = new HashSet<String>();

        // Build a set of expected fields from the target class and its superclasses.
        var clazz = targetClass;
        while (clazz != null && clazz != Object.class) {
          for (var field : clazz.getDeclaredFields()) {
            // Skip static and transient fields.
            if (Modifier.isStatic(field.getModifiers())
                || Modifier.isTransient(field.getModifiers())) {
              continue;
            }
            var annotation = field.getAnnotation(SerializedName.class);
            if (annotation != null) {
              expectedFields.add(annotation.value());
              for (var alt : annotation.alternate()) {
                expectedFields.add(alt);
              }
            } else {
              expectedFields.add(field.getName());
            }
          }
          clazz = clazz.getSuperclass();
        }

        // Check the JSON object for unknown keys.
        if (jsonElement.isJsonObject()) {
          var obj = jsonElement.getAsJsonObject();
          for (var key : obj.keySet()) {
            if (!expectedFields.contains(key)) {
              var cause =
                  new BadRequestException(
                      "Unknown property given for type "
                          + targetClass.getSimpleName().replaceAll("DTO$", "")
                          + ": '"
                          + key
                          + "'");
              throw new JsonParseException(cause.getMessage(), cause);
            }
          }
        }

        // Delegate to Gson's standard deserialization.
        return delegate.fromJsonTree(jsonElement);
      }
    };
  }
}
