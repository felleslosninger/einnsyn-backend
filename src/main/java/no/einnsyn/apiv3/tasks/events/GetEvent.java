package no.einnsyn.apiv3.tasks.events;

import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import org.springframework.context.ApplicationEvent;

public class GetEvent extends ApplicationEvent {

  BaseDTO dto;

  public GetEvent(Object source, BaseDTO dto) {
    super(source);
    this.dto = dto;
  }
}
