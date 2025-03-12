package no.einnsyn.backend.configuration.typeadapters;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.validation.password.Password;

/**
 * A TypeAdapterFactory serializer that will:
 *
 * <ul>
 *   <li>Add properties in the order Superclass -> Subclass
 *   <li>Ignore static and transient fields
 *   <li>Skip properties annotated with @Password
 */
public class BaseDTOTypeAdapterFactory implements TypeAdapterFactory {

  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
    // Only apply to BaseDTO types.
    if (!BaseDTO.class.isAssignableFrom(type.getRawType())) {
      return null;
    }

    // Get the default delegate adapter for deserialization.
    final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);

    return new TypeAdapter<T>() {
      @Override
      public void write(JsonWriter out, T value) throws IOException {
        var jsonObject = new JsonObject();

        // Build a list representing the class hierarchy from the top-most superclass to the
        // subclass.
        var classHierarchy = new ArrayList<Class<?>>();
        var current = value.getClass();
        while (current != null && current != Object.class) {
          classHierarchy.add(0, current); // insert at beginning so that top-most class comes first
          current = current.getSuperclass();
        }

        // Iterate through the hierarchy: superclass properties first, then subclass.
        for (var clazz : classHierarchy) {
          for (var field : clazz.getDeclaredFields()) {

            // Skip static or transient fields.
            if (Modifier.isStatic(field.getModifiers())
                || Modifier.isTransient(field.getModifiers())) {
              continue;
            }

            // Skip fields annotated with @Password
            if (field.getAnnotation(Password.class) != null) {
              continue;
            }

            field.setAccessible(true);
            try {
              var fieldValue = field.get(value);
              // Use Gson's toJsonTree to serialize the field value.
              var element = gson.toJsonTree(fieldValue);
              jsonObject.add(field.getName(), element);
            } catch (IllegalAccessException e) {
              throw new RuntimeException(e);
            }
          }
        }

        // Write the constructed JSON object.
        Streams.write(jsonObject, out);
      }

      @Override
      public T read(JsonReader in) throws IOException {
        // Delegate deserialization to the default adapter.
        return delegate.read(in);
      }
    };
  }
}
