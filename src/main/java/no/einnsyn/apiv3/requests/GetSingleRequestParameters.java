package no.einnsyn.apiv3.requests;

import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetSingleRequestParameters {

  private String externalId;

  private Set<String> expand;

}
