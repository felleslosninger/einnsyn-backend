package no.einnsyn.backend.common.statistics;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.exceptions.models.InternalServerErrorException;
import no.einnsyn.backend.common.search.SearchQueryService;
import no.einnsyn.backend.common.statistics.models.StatisticsParameters;
import no.einnsyn.backend.common.statistics.models.StatisticsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@SuppressWarnings("java:S1192") // Allow string literals
public class StatisticsService {

  private static final int MAX_BUCKETS = 1000;
  private static final String INTERVAL_HOUR = "hour";
  private static final String INTERVAL_DAY = "day";
  private static final String INTERVAL_WEEK = "week";
  private static final String INTERVAL_YEAR = "year";

  private final ElasticsearchClient esClient;
  private final SearchQueryService searchQueryService;

  @Value("${application.elasticsearch.index}")
  private String elasticsearchIndex;

  public StatisticsService(ElasticsearchClient esClient, SearchQueryService searchQueryService) {
    this.esClient = esClient;
    this.searchQueryService = searchQueryService;
  }

  /**
   * Query statistics based on the provided parameters
   *
   * @param statisticsParameters the parameters for filtering and aggregating statistics
   * @return the statistics response containing summary, time series, and metadata
   * @throws EInnsynException if the query fails
   */
  public StatisticsResponse query(StatisticsParameters statisticsParameters)
      throws EInnsynException {
    var queryBuilder = searchQueryService.getQueryBuilder(statisticsParameters);

    // Get aggregate from/to range. Incoming values has already been validated when parsed.
    var aggregateTo =
        statisticsParameters.getAggregateTo() != null
            ? LocalDate.parse(statisticsParameters.getAggregateTo()).plusDays(1).atStartOfDay()
            : LocalDate.now().plusDays(1).atStartOfDay();
    var aggregateFrom =
        statisticsParameters.getAggregateFrom() != null
            ? LocalDate.parse(statisticsParameters.getAggregateFrom()).atStartOfDay()
            : aggregateTo.minusYears(1);
    var calendarInterval =
        calculateCalendarInterval(
            aggregateFrom, aggregateTo, statisticsParameters.getAggregateInterval());

    // No need to check documents created after the aggregation range
    if (aggregateTo.isBefore(LocalDateTime.now())) {
      var createdDateRangeQuery = getCreatedDateRangeQuery(null, aggregateTo);
      if (createdDateRangeQuery != null) {
        queryBuilder.filter(f -> f.range(createdDateRangeQuery));
      }
    }

    var query = queryBuilder.build();
    var createdCountAggregation =
        buildCreatedCountAggregation(aggregateFrom, aggregateTo, calendarInterval);
    var innsynskravCountAggregation =
        buildInnsynskravCountAggregation(aggregateFrom, aggregateTo, calendarInterval);
    var downloadCountAggregation =
        buildDownloadCountAggregation(aggregateFrom, aggregateTo, calendarInterval);
    var fulltextCountAggregation =
        buildFulltextCountAggregation(aggregateFrom, aggregateTo, calendarInterval);

    var searchRequestBuilder = new SearchRequest.Builder();
    searchRequestBuilder.index(elasticsearchIndex);
    searchRequestBuilder.query(q -> q.bool(query));
    searchRequestBuilder.size(0); // Don't fetch results
    searchRequestBuilder.aggregations("innsynskravCount", innsynskravCountAggregation);
    searchRequestBuilder.aggregations("downloadCount", downloadCountAggregation);
    searchRequestBuilder.aggregations("fulltextCount", fulltextCountAggregation);
    searchRequestBuilder.aggregations("createdCount", createdCountAggregation);
    var searchRequest = searchRequestBuilder.build();

    try {
      log.debug("getStatistics() request: {}", searchRequest.toString());
      var searchResponse = esClient.search(searchRequest, Void.class);
      log.debug("getStatistics() response: {}", searchResponse.toString());
      return buildResponse(searchResponse, aggregateFrom, aggregateTo, calendarInterval);
    } catch (IOException e) {
      throw new InternalServerErrorException("Failed to get statistics", e);
    }
  }

  /**
   * Build statistics response from Elasticsearch search response
   *
   * @param response the Elasticsearch search response containing aggregations
   * @param aggregateFrom the start date of the aggregation
   * @param aggregateTo the end date of the aggregation
   * @param calendarInterval the interval used for the aggregation
   * @return the statistics response with populated summary, time series, and metadata
   */
  @SuppressWarnings("java:S1192") // Allow string literals
  StatisticsResponse buildResponse(
      SearchResponse<Void> response,
      LocalDateTime aggregateFrom,
      LocalDateTime aggregateTo,
      CalendarInterval calendarInterval) {
    var innsynskravAggregations = response.aggregations().get("innsynskravCount");
    var downloadAggregations = response.aggregations().get("downloadCount");
    var fulltextCountAggregations = response.aggregations().get("fulltextCount");
    var createdCountAggregations = response.aggregations().get("createdCount");
    var statisticsResponse = new StatisticsResponse();

    // Build summary
    var summary = new StatisticsResponse.Summary();
    statisticsResponse.setSummary(summary);

    // Set total document count
    if (createdCountAggregations != null && createdCountAggregations.isFilter()) {
      summary.setCreatedCount((int) createdCountAggregations.filter().docCount());
    } else {
      summary.setCreatedCount(0);
    }

    // Set total fulltext count
    if (fulltextCountAggregations != null && fulltextCountAggregations.isFilter()) {
      summary.setCreatedWithFulltextCount((int) fulltextCountAggregations.filter().docCount());
    } else {
      summary.setCreatedWithFulltextCount(0);
    }

    // Child aggregations (innsynskrav, download) wrap their results in a "filtered" aggregation
    var innsynskravFiltered = childrenFiltered(innsynskravAggregations);
    var downloadFiltered = childrenFiltered(downloadAggregations);

    // Set total innsynskrav children count
    if (innsynskravFiltered != null && innsynskravFiltered.isFilter()) {
      summary.setCreatedInnsynskravCount((int) innsynskravFiltered.filter().docCount());
    } else {
      summary.setCreatedInnsynskravCount(0);
    }

    // Set total download count. Download children carry a count each, so sum them.
    summary.setDownloadCount(extractSumAggregationValue(downloadFiltered, "countSum"));

    // Build timeSeries - collect all unique time buckets from all aggregations
    var timeSeriesMap = new LinkedHashMap<String, StatisticsResponse.TimeSeries>();

    // Collect buckets from all aggregations
    collectTimeSeriesBuckets(
        createdCountAggregations,
        timeSeriesMap,
        StatisticsService::bucketDocCount,
        StatisticsResponse.TimeSeries::setCreatedCount);
    collectTimeSeriesBuckets(
        fulltextCountAggregations,
        timeSeriesMap,
        StatisticsService::bucketDocCount,
        StatisticsResponse.TimeSeries::setCreatedWithFulltextCount);
    collectTimeSeriesBuckets(
        innsynskravFiltered,
        timeSeriesMap,
        StatisticsService::bucketDocCount,
        StatisticsResponse.TimeSeries::setCreatedInnsynskravCount);
    collectTimeSeriesBuckets(
        downloadFiltered,
        timeSeriesMap,
        bucket -> sumValue(bucket.aggregations(), "countSum"),
        StatisticsResponse.TimeSeries::setDownloadCount);

    // Sort merged buckets chronologically. The different aggregations can have disjoint bucket
    // sets, so insertion order alone does not guarantee a time-ordered response.
    var timeSeries = new ArrayList<>(timeSeriesMap.values());
    timeSeries.sort(Comparator.comparing(StatisticsResponse.TimeSeries::getTime));
    statisticsResponse.setTimeSeries(timeSeries);

    // Build metadata
    var metadata = new StatisticsResponse.Metadata();
    statisticsResponse.setMetadata(metadata);
    metadata.setAggregateFrom(aggregateFrom.toLocalDate().toString());
    // Internally we treat aggregateTo as an exclusive upper bound (start of the day after the
    // requested end date). The API response metadata should reflect the user-facing end date.
    metadata.setAggregateTo(aggregateTo.minusDays(1).toLocalDate().toString());
    metadata.setAggregateInterval(calendarInterval.jsonValue());

    return statisticsResponse;
  }

  /**
   * Build aggregation for counting verified innsynskrav children over time
   *
   * @param aggregateFrom the start date for filtering
   * @param aggregateTo the end date for filtering
   * @param calendarInterval the interval for the date histogram
   * @return the children aggregation with date histogram buckets
   */
  Aggregation buildInnsynskravCountAggregation(
      LocalDateTime aggregateFrom, LocalDateTime aggregateTo, CalendarInterval calendarInterval) {

    // Filter children by verified, and created range
    var childrenFilterQueryBuilder = new BoolQuery.Builder();
    var aggregationDateRangeQuery = getCreatedDateRangeQuery(aggregateFrom, aggregateTo);
    if (aggregationDateRangeQuery != null) {
      childrenFilterQueryBuilder.filter(f -> f.range(aggregationDateRangeQuery));
    }

    // Filter by verified
    var verifiedTermQuery = TermQuery.of(t -> t.field("verified").value(v -> v.booleanValue(true)));
    childrenFilterQueryBuilder.filter(f -> f.term(verifiedTermQuery));

    var histogramAgg =
        Aggregation.of(
            a ->
                a.dateHistogram(
                    h -> h.field("created").calendarInterval(calendarInterval).minDocCount(1)));

    return Aggregation.of(
        a ->
            a.children(c -> c.type("innsynskrav"))
                .aggregations(
                    "filtered",
                    Aggregation.of(
                        f ->
                            f.filter(q -> q.bool(childrenFilterQueryBuilder.build()))
                                .aggregations("buckets", histogramAgg))));
  }

  /**
   * Build aggregation for counting document downloads over time. Download documents are stored as
   * hourly child buckets with a numeric count field, so this uses a sum aggregation instead of
   * child doc_count.
   */
  Aggregation buildDownloadCountAggregation(
      LocalDateTime aggregateFrom, LocalDateTime aggregateTo, CalendarInterval calendarInterval) {

    var childrenFilterQueryBuilder = new BoolQuery.Builder();
    var aggregationDateRangeQuery = getCreatedDateRangeQuery(aggregateFrom, aggregateTo);
    if (aggregationDateRangeQuery != null) {
      childrenFilterQueryBuilder.filter(f -> f.range(aggregationDateRangeQuery));
    }

    // "count" field comes from DownloadCountES.count in the download child documents
    var sumAgg = Aggregation.of(a -> a.sum(s -> s.field("count")));
    var histogramAgg =
        Aggregation.of(
            a ->
                a.dateHistogram(
                        h -> h.field("created").calendarInterval(calendarInterval).minDocCount(1))
                    .aggregations("countSum", sumAgg));

    return Aggregation.of(
        a ->
            a.children(c -> c.type("download"))
                .aggregations(
                    "filtered",
                    Aggregation.of(
                        f ->
                            f.filter(q -> q.bool(childrenFilterQueryBuilder.build()))
                                .aggregations("countSum", sumAgg)
                                .aggregations("buckets", histogramAgg))));
  }

  /**
   * Build aggregation for counting all documents over time
   *
   * @param aggregateFrom the start date for filtering
   * @param aggregateTo the end date for filtering
   * @param calendarInterval the interval for the date histogram
   * @return the filter aggregation with date histogram buckets for all documents
   */
  Aggregation buildCreatedCountAggregation(
      LocalDateTime aggregateFrom, LocalDateTime aggregateTo, CalendarInterval calendarInterval) {

    // Filter by created date range
    var aggregationDateRangeQuery = getCreatedDateRangeQuery(aggregateFrom, aggregateTo);
    var filterQueryBuilder = new BoolQuery.Builder();
    filterQueryBuilder.filter(f -> f.range(aggregationDateRangeQuery));

    var histogramAgg =
        Aggregation.of(
            a ->
                a.dateHistogram(
                    h -> h.field("created").calendarInterval(calendarInterval).minDocCount(1)));

    return Aggregation.of(
        f ->
            f.filter(q -> q.bool(filterQueryBuilder.build()))
                .aggregations("buckets", histogramAgg));
  }

  /**
   * Build aggregation for documents with fulltext property
   *
   * @param aggregateFrom the start date for filtering
   * @param aggregateTo the end date for filtering
   * @param calendarInterval the interval for the date histogram
   * @return the filter aggregation with date histogram buckets for documents with fulltext=true
   */
  Aggregation buildFulltextCountAggregation(
      LocalDateTime aggregateFrom, LocalDateTime aggregateTo, CalendarInterval calendarInterval) {

    // Filter by created date range
    var aggregationDateRangeQuery = getCreatedDateRangeQuery(aggregateFrom, aggregateTo);
    var filterQueryBuilder = new BoolQuery.Builder();
    filterQueryBuilder.filter(f -> f.range(aggregationDateRangeQuery));

    // Filter by fulltext = true
    var fulltextTermQuery = TermQuery.of(t -> t.field("fulltext").value(v -> v.booleanValue(true)));
    filterQueryBuilder.filter(f -> f.term(fulltextTermQuery));

    var histogramAgg =
        Aggregation.of(
            a ->
                a.dateHistogram(
                    h -> h.field("created").calendarInterval(calendarInterval).minDocCount(1)));

    return Aggregation.of(
        f ->
            f.filter(q -> q.bool(filterQueryBuilder.build()))
                .aggregations("buckets", histogramAgg));
  }

  /**
   * Select a date histogram bucket interval for the given date range.
   *
   * <p>The returned interval is the most fine-grained interval that does not exceed {@link
   * #MAX_BUCKETS}. If {@code requestedInterval} is provided, it is treated as the <em>maximum</em>
   * desired resolution (hour/day/week/month/year). If that would exceed the bucket limit, the
   * method falls back to progressively coarser intervals until it fits.
   *
   * <p>{@code year} is only ever returned when it is explicitly requested. It is the coarsest
   * interval we offer, so the fallback chain stops at {@code month} and never reaches it on its
   * own.
   *
   * @param aggregateFrom the start date in ISO-8601 format (yyyy-MM-dd)
   * @param aggregateTo the end date in ISO-8601 format (yyyy-MM-dd)
   * @param requestedInterval the desired maximum resolution: hour/day/week/month/year
   * @return the chosen bucket interval (Hour/Day/Week/Month/Year)
   */
  private CalendarInterval calculateCalendarInterval(
      LocalDateTime aggregateFrom, LocalDateTime aggregateTo, String requestedInterval) {

    var aggregateFromDate = aggregateFrom.toLocalDate().atStartOfDay();
    var aggregateToDate = aggregateTo.toLocalDate().atStartOfDay();

    if (!StringUtils.hasText(requestedInterval)) {
      requestedInterval = INTERVAL_HOUR;
    } else {
      requestedInterval = requestedInterval.toLowerCase();
    }

    if (INTERVAL_HOUR.equals(requestedInterval)) {
      var hours = ChronoUnit.HOURS.between(aggregateFromDate, aggregateToDate) + 24;
      if (hours <= MAX_BUCKETS) {
        return CalendarInterval.Hour;
      }
      // Resolution too high, fall back to day
      requestedInterval = INTERVAL_DAY;
    }

    if (INTERVAL_DAY.equals(requestedInterval)) {
      var days = ChronoUnit.DAYS.between(aggregateFromDate, aggregateToDate) + 1;
      if (days <= MAX_BUCKETS) {
        return CalendarInterval.Day;
      }
      // Resolution too high, fall back to week
      requestedInterval = INTERVAL_WEEK;
    }

    if (INTERVAL_WEEK.equals(requestedInterval)) {
      // Buckets are aligned to Monday, so count the weeks the range touches rather than the whole
      // seven day periods between its ends: Sunday to the following Tuesday touches three weeks.
      var firstWeek =
          aggregateFromDate.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
      var lastWeek =
          aggregateToDate
              .toLocalDate()
              .minusDays(1)
              .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
      var weeks = ChronoUnit.WEEKS.between(firstWeek, lastWeek) + 1;
      if (weeks <= MAX_BUCKETS) {
        return CalendarInterval.Week;
      }
    }

    if (INTERVAL_YEAR.equals(requestedInterval)) {
      return CalendarInterval.Year;
    }

    return CalendarInterval.Month;
  }

  /**
   * Create a range query for filtering documents by created date
   *
   * @param aggregateFrom the start date (inclusive), or null for no lower bound
   * @param aggregateTo the end date (inclusive), or null for no upper bound
   * @return the range query, or null if both parameters are empty
   */
  RangeQuery getCreatedDateRangeQuery(LocalDateTime aggregateFrom, LocalDateTime aggregateTo) {
    if (aggregateFrom == null && aggregateTo == null) {
      return null;
    }

    return RangeQuery.of(
        r ->
            r.date(
                d -> {
                  if (aggregateFrom != null) {
                    d.field("created").gte(aggregateFrom.toString());
                  }
                  if (aggregateTo != null) {
                    d.field("created").lte(aggregateTo.toString());
                  }
                  return d;
                }));
  }

  /**
   * Helper method to collect time series buckets from aggregation results into a map
   *
   * @param aggregation the aggregation result to extract buckets from
   * @param timeSeriesMap the map to populate with time series data points
   * @param valueOf how to read the value of a bucket, e.g. its doc count or a sub-aggregation
   * @param setter the consumer to set the value on each data point
   */
  private void collectTimeSeriesBuckets(
      Aggregate aggregation,
      Map<String, StatisticsResponse.TimeSeries> timeSeriesMap,
      ToIntFunction<DateHistogramBucket> valueOf,
      ObjIntConsumer<StatisticsResponse.TimeSeries> setter) {
    if (aggregation != null && aggregation.isFilter()) {
      var buckets = aggregation.filter().aggregations().get("buckets");
      if (buckets != null && buckets.isDateHistogram()) {
        var dateHistogram = buckets.dateHistogram();
        for (var bucket : dateHistogram.buckets().array()) {
          var dataPoint =
              timeSeriesMap.computeIfAbsent(
                  bucket.keyAsString(), StatisticsService::newTimeSeriesPoint);
          setter.accept(dataPoint, valueOf.applyAsInt(bucket));
        }
      }
    }
  }

  private static StatisticsResponse.TimeSeries newTimeSeriesPoint(String time) {
    var point = new StatisticsResponse.TimeSeries();
    point.setTime(time);
    point.setCreatedCount(0);
    point.setCreatedWithFulltextCount(0);
    point.setCreatedInnsynskravCount(0);
    point.setDownloadCount(0);
    return point;
  }

  private static int bucketDocCount(DateHistogramBucket bucket) {
    return (int) bucket.docCount();
  }

  /**
   * Unwrap the "filtered" sub-aggregation of a children aggregation.
   *
   * @param aggregation a children aggregation, or null
   * @return the filtered sub-aggregation, or null if there is none
   */
  private static Aggregate childrenFiltered(Aggregate aggregation) {
    if (aggregation != null && aggregation.isChildren()) {
      return aggregation.children().aggregations().get("filtered");
    }
    return null;
  }

  private static int extractSumAggregationValue(Aggregate aggregation, String sumAggregationName) {
    if (aggregation != null && aggregation.isFilter()) {
      return sumValue(aggregation.filter().aggregations(), sumAggregationName);
    }
    return 0;
  }

  /** Read a sum metric from a set of sub-aggregations. Sums of integer fields are exact. */
  private static int sumValue(Map<String, Aggregate> aggregations, String sumAggregationName) {
    var metric = aggregations.get(sumAggregationName);
    return metric != null && metric.isSum() ? (int) Math.round(metric.sum().value()) : 0;
  }
}
