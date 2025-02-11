package no.einnsyn.backend.common.responses.models;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaginatedList<T> {

  private List<T> items = new ArrayList<>();

  private String next;

  private String previous;

  public PaginatedList() {}

  public PaginatedList(List<T> items, int limit) {
    this.items = items.subList(0, Math.min(items.size(), limit));
  }

  public PaginatedList(List<T> items) {
    this.items = items;
  }
}
