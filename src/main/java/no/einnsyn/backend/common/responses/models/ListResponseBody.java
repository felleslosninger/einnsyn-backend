package no.einnsyn.backend.common.responses.models;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListResponseBody<T> {

  private List<T> items = new ArrayList<>();

  private String next;

  private String previous;

  public ListResponseBody() {}

  public ListResponseBody(List<T> items, int limit) {
    this.items = items.subList(0, Math.min(items.size(), limit));
  }

  public ListResponseBody(List<T> items) {
    this.items = items;
  }
}
