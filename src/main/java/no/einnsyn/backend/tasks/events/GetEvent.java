package no.einnsyn.backend.tasks.events;

import no.einnsyn.backend.entities.base.models.BaseDTO;
import org.springframework.context.ApplicationEvent;

public class GetEvent extends ApplicationEvent {

  transient BaseDTO dto;

  public GetEvent(Object source, BaseDTO dto) {
    super(source);
    this.dto = dto;
  }
}
