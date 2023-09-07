package no.einnsyn.apiv3.entities.expandablefield;

import java.io.IOException;
import java.util.List;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;

public class ExpandableFieldDeserializer extends JsonDeserializer<ExpandableField<?>>
    implements ContextualDeserializer {

  private JavaType type;


  @Override
  public JsonDeserializer<?> createContextual(DeserializationContext deserializationContext,
      BeanProperty beanProperty) {
    if (beanProperty != null) {
      // TODO: This is a workaround, since this deserializer seems to be executed for the lists
      // that wrap ExpandableField as well as the wrapped ExpandableFields themselves. Is this to
      // be expected?
      if (beanProperty.getType().getRawClass() == List.class) {
        this.type = beanProperty.getType();
      } else {
        this.type = beanProperty.getType().containedType(0);
      }
    } else {
      this.type = deserializationContext.getContextualType().containedType(0);
    }
    return this;
  }


  @Override
  public ExpandableField<?> deserialize(JsonParser jsonParser,
      DeserializationContext deserializationContext) throws IOException {

    Object object = null;
    String id = null;
    try {
      id = deserializationContext.readValue(jsonParser, String.class);
    } catch (Exception e) {
      object = deserializationContext.readValue(jsonParser, type);
      if (object != null && object instanceof ExpandableField<?>) {
        return (ExpandableField<?>) object;
      }
      if (object != null && object instanceof EinnsynObjectJSON) {
        id = ((EinnsynObjectJSON) object).getId();
      }
    }
    if (id == null) {
      throw new RuntimeException("Could not deserialize ExpandableField");
    }
    return new ExpandableField<>(id, object);
  }
}
