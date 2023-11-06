package no.einnsyn.apiv3.responses;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseList<T> {

  private Boolean hasMore = false;

  private List<T> data = new ArrayList<>();

  private String next;

  private String previous;

  public ResponseList() {}

  public ResponseList(List<T> data) {
    this.data = data;
  }
}
