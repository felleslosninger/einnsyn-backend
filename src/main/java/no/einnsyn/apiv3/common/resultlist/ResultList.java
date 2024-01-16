package no.einnsyn.apiv3.common.resultlist;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResultList<T> {

  private Boolean hasMore = false;

  private List<T> items = new ArrayList<>();

  private String next;

  private String previous;

  public ResultList() {}

  public ResultList(List<T> items, int limit) {
    this.hasMore = items.size() > limit;
    this.items = items.subList(0, Math.min(items.size(), limit));
  }

  public ResultList(List<T> items) {
    this.items = items;
  }
}
