package no.einnsyn.apiv3.entities.expandablefield;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;

public class ExpandableFieldSerializer
    implements JsonSerializer<ExpandableField<? extends EinnsynObjectJSON>> {

  @Override
  public JsonElement serialize(
      ExpandableField<? extends EinnsynObjectJSON> src,
      Type typeOfSrc,
      JsonSerializationContext context) {
    if (src.isExpanded()) {
      return context.serialize(src.getExpandedObject());
    } else {
      return new JsonPrimitive(src.getId());
    }
  }
}
