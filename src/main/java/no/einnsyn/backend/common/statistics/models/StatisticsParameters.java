// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.common.statistics.models;

import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.queryparameters.models.FilterParameters;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime;

/** Statistics parameters */
@Getter
@Setter
public class StatisticsParameters extends FilterParameters {
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
  String aggregateFrom;

  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
  String aggregateTo;
}
