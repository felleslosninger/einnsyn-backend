package no.einnsyn.backend.entities.base;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import no.einnsyn.backend.entities.base.models.BaseDTO;

public class BaseDTOTypeAdapterFactory implements TypeAdapterFactory {

  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
    final var delegate = gson.getDelegateAdapter(this, typeToken);

    if (!BaseDTO.class.isAssignableFrom(typeToken.getRawType())) {
      return null;
    }

    // Create a new TypeAdapter for the specific type
    return new TypeAdapter<T>() {

      /**
       * By default, delegate.write() will iterate from subclasses to superclasses when adding
       * properties. This makes the superclass' properties appear last in the JSON. We want the
       * superclass properties first (_id), so we extend this method to iterate in the correct
       * order.
       */
      @Override
      public void write(JsonWriter out, T value) throws IOException {
        if (value == null) {
          out.nullValue();
          return;
        }

        out.beginObject();

        // Get class hierarchy, so we can iterate bottom-up. This way we'll get the superclass'
        // properties first.
        var hierarchy = new ArrayList<Class<?>>();
        var currentClass = value.getClass();
        while (currentClass != null && currentClass != Object.class) {
          hierarchy.add(0, currentClass);
          currentClass = currentClass.getSuperclass();
        }

        // Iterate all fields
        for (var clazz : hierarchy) {
          var fields = clazz.getDeclaredFields();
          for (var field : fields) {
            field.setAccessible(true);
            try {
              out.name(field.getName());
              gson.toJson(field.get(value), field.getGenericType(), out);
            } catch (IllegalAccessException e) {
              throw new RuntimeException(e);
            }
          }
        }

        out.endObject();
      }

      @Override
      public T read(JsonReader in) throws IOException {
        var jsonElement = JsonParser.parseReader(in);
        if (!jsonElement.isJsonObject()) {
          return delegate.fromJsonTree(jsonElement);
        }

        // Infer known properties for this class (and superclasses) using reflection
        var knownProperties = new HashSet<>();
        var currentClass = typeToken.getRawType();
        while (currentClass != null && currentClass != Object.class) {
          var fields = currentClass.getDeclaredFields();
          for (var field : fields) {
            knownProperties.add(field.getName());
          }
          currentClass = currentClass.getSuperclass();
        }

        // Check for unknown properties
        var jsonObject = jsonElement.getAsJsonObject();
        for (var key : jsonObject.keySet()) {
          if (!knownProperties.contains(key)) {
            throw new JsonParseException(
                "Unknown property given for type "
                    + typeToken.getRawType().getSimpleName().replaceAll("DTO$", "")
                    + ": '"
                    + key
                    + "'");
          }
        }

        return delegate.fromJsonTree(jsonElement);
      }
    };
  }
}
