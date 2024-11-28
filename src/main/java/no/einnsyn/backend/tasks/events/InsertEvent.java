package no.einnsyn.backend.tasks.events;

import no.einnsyn.backend.entities.base.models.BaseDTO;
import org.springframework.context.ApplicationEvent;

public class InsertEvent extends ApplicationEvent {

  transient BaseDTO dto;

  public InsertEvent(Object source, BaseDTO dto) {
    super(source);
    this.dto = dto;
  }
}
