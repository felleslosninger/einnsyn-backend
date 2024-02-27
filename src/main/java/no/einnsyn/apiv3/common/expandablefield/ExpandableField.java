package no.einnsyn.apiv3.common.expandablefield;

import jakarta.validation.Valid;
import no.einnsyn.apiv3.common.hasid.HasId;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;

/**
 * A class representing "expandable fields" in the API. These are fields that are either an ID or an
 * object. This is inspired by Stripe's API: https://stripe.com/docs/api/expanding_objects
 *
 * <p>An ExpandableField will always contain an ID, and may contain an object if it has been
 * expanded.
 */
public class ExpandableField<T extends HasId> {

  private String id = null;

  @Valid private T expandedObject = null;

  public ExpandableField(String id) {
    this.id = id;
  }

  public ExpandableField(T expandedObject) {
    this.id = expandedObject.getId();
    this.expandedObject = expandedObject;
  }

  public ExpandableField(String id, T expandedObject) {
    this.id = id;
    this.expandedObject = expandedObject;
  }

  public boolean isExpanded() {
    return expandedObject != null;
  }

  public void setExpandedObject(T expandedObject) {
    this.expandedObject = expandedObject;
    if (this.id == null) {
      this.id = expandedObject.getId();
    }
  }

  public T getExpandedObject() {
    return expandedObject;
  }

  public T requireExpandedObject() throws EInnsynException {
    if (expandedObject == null) {
      throw new EInnsynException("An ID was provided, but an object is required.");
    }
    return expandedObject;
  }

  public String getId() {
    if (id == null && expandedObject != null) {
      return expandedObject.getId();
    }
    return id;
  }

  public String toString() {
    return id;
  }
}
