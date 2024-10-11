package no.einnsyn.apiv3.tasks.events;

import lombok.Getter;
import no.einnsyn.apiv3.entities.base.models.BaseES;
import org.springframework.context.ApplicationEvent;

@Getter
public class IndexEvent extends ApplicationEvent {

  BaseES document;
  boolean insert;

  public IndexEvent(Object source, BaseES document, boolean isInsert) {
    super(source);
    this.document = document;
    this.insert = isInsert;
  }
}
