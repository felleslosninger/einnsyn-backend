package no.einnsyn.backend.common.expandablefield;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import org.springframework.stereotype.Component;

@Component
public class ExpandableFieldSerializer
    implements JsonSerializer<ExpandableField<? extends BaseDTO>> {

  @Override
  public JsonElement serialize(
      ExpandableField<? extends BaseDTO> src, Type typeOfSrc, JsonSerializationContext context) {
    if (src.isExpanded()) {
      return context.serialize(src.getExpandedObject());
    } else {
      return new JsonPrimitive(src.getId());
    }
  }
}
