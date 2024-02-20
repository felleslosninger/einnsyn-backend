package no.einnsyn.apiv3.entities.base;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.HashSet;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;

public class BaseDTOTypeAdapterFactory implements TypeAdapterFactory {

  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
    final var delegate = gson.getDelegateAdapter(this, typeToken);

    if (!BaseDTO.class.isAssignableFrom(typeToken.getRawType())) {
      return null;
    }

    // Create a new TypeAdapter for the specific type
    return new TypeAdapter<T>() {

      @Override
      public void write(JsonWriter out, T value) throws IOException {
        delegate.write(out, value);
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
