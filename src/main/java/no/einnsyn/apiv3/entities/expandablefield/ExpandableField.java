package no.einnsyn.apiv3.entities.expandablefield;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonDeserialize(using = ExpandableFieldDeserializer.class)
@JsonSerialize(using = ExpandableFieldSerializer.class)
public class ExpandableField<T> {

  private String id = null;

  private T expandedObject = null;

  public ExpandableField(String id, T expandedObject) {
    this.id = id;
    this.expandedObject = expandedObject;
  }

  public boolean isExpanded() {
    return expandedObject != null;
  }

  public void setExpandedObject(T expandedObject) {
    this.expandedObject = expandedObject;
  }

  public T getExpandedObject() {
    return expandedObject;
  }

  public String getId() {
    return id;
  }

}
