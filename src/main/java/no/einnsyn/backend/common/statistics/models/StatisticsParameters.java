// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.common.statistics.models;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.queryparameters.models.FilterParameters;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime;
import no.einnsyn.backend.validation.validenum.ValidEnum;

/** Parameters for querying statistics data */
@Getter
@Setter
public class StatisticsParameters extends FilterParameters {
  /**
   * The start date for aggregating statistics. If not provided, it will be set to one year before
   * `aggregateTo`.
   */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
  protected String aggregateFrom;

  /**
   * The end date for aggregating statistics. If not provided, statistics up to the current date
   * will be included.
   */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
  protected String aggregateTo;

  /**
   * The preferred time interval for aggregating statistics data. Determines how data points are
   * grouped in the time series. Note: There is a maximum limit of 1000 data points in the time
   * series. If the requested interval combined with the date range would exceed this limit, the
   * interval will be automatically adjusted to a larger granularity to stay within the limit.
   * Default is "hour".
   */
  @ValidEnum(enumClass = AggregateIntervalEnum.class)
  protected String aggregateInterval = "hour";

  public enum AggregateIntervalEnum {
    @SerializedName("hour")
    HOUR("hour"),
    @SerializedName("day")
    DAY("day"),
    @SerializedName("week")
    WEEK("week"),
    @SerializedName("month")
    MONTH("month"),
    @SerializedName("year")
    YEAR("year");

    private final String value;

    AggregateIntervalEnum(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }

    public String toJson() {
      return value;
    }

    public static AggregateIntervalEnum fromValue(String value) {
      value = value.trim().toLowerCase();
      for (AggregateIntervalEnum val : AggregateIntervalEnum.values()) {
        if (val.value.toLowerCase().equals(value)) {
          return val;
        }
      }
      throw new IllegalArgumentException("Unknown value: " + value);
    }
  }
}
