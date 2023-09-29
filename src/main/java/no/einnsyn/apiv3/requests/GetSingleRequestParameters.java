package no.einnsyn.apiv3.requests;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetSingleRequestParameters {

  private String externalId;

  private List<String> expand;

  }
