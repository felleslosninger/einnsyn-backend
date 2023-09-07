package no.einnsyn.apiv3.entities.expandablefield;

import java.io.IOException;
import java.util.List;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;

public class ExpandableFieldDeserializer extends JsonDeserializer<ExpandableField<?>>
    implements ContextualDeserializer {

  private JavaType type;


  @Override
  public JsonDeserializer<?> createContextual(DeserializationContext deserializationContext,
      BeanProperty beanProperty) {
    if (beanProperty != null) {
      // TODO: This deserializer seems to be executed for the lists that wrap ExpandableField as
      // well as the wrapped ExpandableFields themselves. Is this to
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

    EinnsynObject object = null;
    String id = null;

    try {
      // Try to parse an ID
      id = deserializationContext.readValue(jsonParser, String.class);
    } catch (Exception e) {
      // Try to parse an EinnsynObjectJSON object
      object = deserializationContext.readValue(jsonParser, type);
      if (object != null) {
        id = object.getId();
      }
    }

    if (id == null) {
      throw new RuntimeException("Could not deserialize ExpandableField");
    }
    return new ExpandableField<>(id, object);
  }
}
