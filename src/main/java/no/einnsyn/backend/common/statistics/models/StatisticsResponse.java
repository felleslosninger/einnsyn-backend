// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.common.statistics.models;

import jakarta.validation.constraints.Null;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

@Getter
@Setter
public class StatisticsResponse {
  @Null(groups = {Insert.class, Update.class})
  protected Innsynskrav innsynskrav;

  @Null(groups = {Insert.class, Update.class})
  protected Download download;

  @Getter
  @Setter
  public static class Innsynskrav {
    @Null(groups = {Insert.class, Update.class})
    protected Integer count;

    @Null(groups = {Insert.class, Update.class})
    protected Integer interval;

    @Null(groups = {Insert.class, Update.class})
    protected List<Bucket> bucket;

    @Getter
    @Setter
    public static class Bucket {
      @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
      @Null(groups = {Insert.class, Update.class})
      protected String time;

      @Null(groups = {Insert.class, Update.class})
      protected Integer count;
    }
  }

  @Getter
  @Setter
  public static class Download {
    @Null(groups = {Insert.class, Update.class})
    protected Integer count;

    @Null(groups = {Insert.class, Update.class})
    protected Integer interval;

    @Null(groups = {Insert.class, Update.class})
    protected List<Bucket> bucket;

    @Getter
    @Setter
    public static class Bucket {
      @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
      @Null(groups = {Insert.class, Update.class})
      protected String time;

      @Null(groups = {Insert.class, Update.class})
      protected Integer count;
    }
  }
}
