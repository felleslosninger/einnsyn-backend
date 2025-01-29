// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.common.statistics.models;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime;
import no.einnsyn.backend.validation.validationgroups.Insert;

@Getter
@Setter
public class StatisticsResponse {
  Innsynskrav innsynskrav;

  Download download;

  @Getter
  @Setter
  public class Innsynskrav {
    @NotNull(groups = {Insert.class})
    Integer count;

    @NotNull(groups = {Insert.class})
    Integer interval;

    @NotNull(groups = {Insert.class})
    List<Bucket> bucket;

    @Getter
    @Setter
    public class Bucket {
      @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
      @NotNull(groups = {Insert.class})
      String time;

      @NotNull(groups = {Insert.class})
      Integer count;
    }
  }

  @Getter
  @Setter
  public class Download {
    @NotNull(groups = {Insert.class})
    Integer count;

    @NotNull(groups = {Insert.class})
    Integer interval;

    @NotNull(groups = {Insert.class})
    List<Bucket> bucket;

    @Getter
    @Setter
    public class Bucket {
      @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
      @NotNull(groups = {Insert.class})
      String time;

      @NotNull(groups = {Insert.class})
      Integer count;
    }
  }
}
