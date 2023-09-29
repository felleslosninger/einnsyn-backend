package no.einnsyn.apiv3.responses;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;

@Getter
@Setter
public class ResponseList<T extends EinnsynObjectJSON> {

  private Boolean hasMore = false;

  private List<T> data = new ArrayList<>();

  private String next;

  private String previous;

  public ResponseList() {}

  public ResponseList(List<T> data) {
    this.data = data;
  }

  }
