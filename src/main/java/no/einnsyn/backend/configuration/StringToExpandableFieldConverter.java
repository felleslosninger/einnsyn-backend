package no.einnsyn.backend.configuration;

import com.google.gson.Gson;
import com.google.gson.JsonPrimitive;
import jakarta.annotation.Nullable;
import java.util.Set;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.configuration.typeadapters.ExpandableFieldDeserializer;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

/**
 * Converts a String to an {@link ExpandableField}, by handing it to the same Gson deserializer that
 * parses expandable fields in request bodies.
 *
 * <p>Path variables are bound by Spring's ConversionService, not by the HTTP message converters, so
 * they never reach Gson on their own. Without this converter Spring falls back to its generic
 * {@code ObjectToObjectConverter}, which picks up {@link ExpandableField#ExpandableField(String)}
 * and stores the raw path segment verbatim. Alternative identifiers (systemId, slug, orgnummer,
 * email, ...) would then be resolved in request bodies but not in path variables, leaving every
 * consumer of a path variable to re-resolve it.
 *
 * @see ExpandableFieldDeserializer the deserializer this delegates to
 */
public class StringToExpandableFieldConverter implements ConditionalGenericConverter {

  private final Gson gson;

  public StringToExpandableFieldConverter(Gson gson) {
    this.gson = gson;
  }

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Set.of(new ConvertiblePair(String.class, ExpandableField.class));
  }

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    // ExpandableFieldDeserializer reads the entity class from the generic type parameter, so a raw
    // ExpandableField is left to the default conversion.
    return targetType.getResolvableType().hasGenerics();
  }

  @Override
  @Nullable
  public Object convert(
      @Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (source == null) {
      return null;
    }

    // Wrapping the value as a JSON string hits the same branch of ExpandableFieldDeserializer that
    // an ID reference in a request body does.
    return gson.fromJson(
        new JsonPrimitive((String) source), targetType.getResolvableType().getType());
  }
}
