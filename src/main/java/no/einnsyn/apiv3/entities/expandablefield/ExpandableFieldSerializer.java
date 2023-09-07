package no.einnsyn.apiv3.entities.expandablefield;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;

public class ExpandableFieldSerializer
    extends JsonSerializer<ExpandableField<? extends EinnsynObject>> {

  @Override
  public void serialize(ExpandableField<? extends EinnsynObject> expandableField,
      JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
    if (expandableField.isExpanded()) {
      jsonGenerator.writeObject(expandableField.getExpandedObject());
    } else {
      jsonGenerator.writeNumber(expandableField.getId());
    }
  }
}
