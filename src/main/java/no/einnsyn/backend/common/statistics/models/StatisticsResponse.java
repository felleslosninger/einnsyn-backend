// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.common.statistics.models;

import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

/** Response containing statistics data with summary and optional time series */
@Getter
@Setter
public class StatisticsResponse {
  /** Aggregated summary of statistics over the entire queried period */
  @Null(groups = {Insert.class, Update.class})
  protected Summary summary;

  @Null(groups = {Insert.class, Update.class})
  protected Metadata metadata;

  /**
   * Time series data showing statistics broken down by the specified aggregation interval. Each
   * entry represents metrics for a specific time period.
   */
  @Null(groups = {Insert.class, Update.class})
  protected List<TimeSeries> timeSeries;

  @Getter
  @Setter
  public static class Summary {
    /** Total number of entities created in the period */
    @Null(groups = {Insert.class, Update.class})
    protected Integer createdCount;

    /** Total number of entities created with fulltext content in the period */
    @Null(groups = {Insert.class, Update.class})
    protected Integer createdWithFulltextCount;

    /** Total number of innsynskrav (access requests) created in the period */
    @Null(groups = {Insert.class, Update.class})
    protected Integer createdInnsynskravCount;

    /** Total number of document downloads in the period */
    @Null(groups = {Insert.class, Update.class})
    protected Integer downloadCount;
  }

  @Getter
  @Setter
  public static class Metadata {
    /** The aggregation interval used for the time series data */
    @NoSSN
    @Size(max = 500)
    @Null(groups = {Insert.class, Update.class})
    protected String aggregateInterval;

    /** The start date for the aggregated statistics */
    @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
    @Null(groups = {Insert.class, Update.class})
    protected String aggregateFrom;

    /** The end date for the aggregated statistics */
    @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
    @Null(groups = {Insert.class, Update.class})
    protected String aggregateTo;
  }

  @Getter
  @Setter
  public static class TimeSeries {
    /** The timestamp for this time series data point */
    @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
    @Null(groups = {Insert.class, Update.class})
    protected String time;

    /** Number of entities created during this time interval */
    @Null(groups = {Insert.class, Update.class})
    protected Integer createdCount;

    /** Number of entities created with fulltext content during this time interval */
    @Null(groups = {Insert.class, Update.class})
    protected Integer createdWithFulltextCount;

    /** Number of innsynskrav (access requests) created during this time interval */
    @Null(groups = {Insert.class, Update.class})
    protected Integer createdInnsynskravCount;

    /** Number of document downloads during this time interval */
    @Null(groups = {Insert.class, Update.class})
    protected Integer downloadCount;
  }
}
