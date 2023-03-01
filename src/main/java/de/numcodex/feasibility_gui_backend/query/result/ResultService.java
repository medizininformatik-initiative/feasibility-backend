package de.numcodex.feasibility_gui_backend.query.result;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.numcodex.feasibility_gui_backend.query.persistence.ResultType;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Stores results of queries volatile.
 * <p>
 * Query results are stored in form of {@link ResultLine result lines}, one for each site per query.
 * All results of a query are cleared after a configurable duration. Only the first result per query
 * will be kept.
 */
@Slf4j
public class ResultService {

  private final Cache<Long, QueryResult> queryResultCache;

  /**
   * Creates a new ResultService.
   *
   * @param resultExpiry the duration after which a result shouldn't be available anymore
   */
  public ResultService(Duration resultExpiry) {
    this.queryResultCache = Caffeine.newBuilder()
        .expireAfterWrite(resultExpiry)
        .build();
  }

  /**
   * Finds all {@link ResultLine results} for a query, that have been successful.
   * <p>
   * In case the query is not found, or the query has no results, an empty {@link List} is
   * returned.
   *
   * @param queryId the query id
   * @return the list of all {@link ResultLine results} for the given {@code queryId}, that have
   * the {@link ResultType result type} {@link ResultType#SUCCESS success}
   */
  public List<ResultLine> findSuccessfulByQuery(long queryId) {
    return queryResultCache.asMap().getOrDefault(queryId, QueryResult.EMPTY).resultsBySite()
        .values().stream().filter(resultLine -> resultLine.type() == ResultType.SUCCESS).toList();
  }

  /**
   * Adds {@code result} to the results of the query with {@code queryId}.
   * <p>
   * Note: Only the <b>first</b> {@link ResultLine result} per site is kept. Subsequent results will
   * be discarded.
   *
   * @param queryId the query id
   * @param result  the {@link ResultLine result} from a site
   */
  public void addResultLine(Long queryId, ResultLine result) {
    queryResultCache.asMap()
        .merge(queryId, QueryResult.ofResultLine(result), QueryResult::merge);
  }

  /**
   * Holds all submitted {@link ResultLine results} from sites to a query.
   */
  private record QueryResult(Map<String, ResultLine> resultsBySite) {

    private static final QueryResult EMPTY = new QueryResult(Map.of());

    private QueryResult {
      resultsBySite = Map.copyOf(resultsBySite);
    }

    private static QueryResult ofResultLine(ResultLine resultLine) {
      return new QueryResult(Map.of(resultLine.siteName(), resultLine));
    }

    /**
     * Merges the result lines of {@code this} and the {@code other} query result, returning a new
     * query result.
     * <p>
     * Prioritises result lines of {@code this} query result over the ones of the {@code other}
     * query result in a way that result lines of a site already present in {@code this} query
     * result will not be overwritten with ones from the {@code other} query result.
     *
     * @param other the query result to merge into this query result
     * @return a new query result containing the result lines from this query result and result
     * lines from new sites of the other query result
     */
    private QueryResult merge(QueryResult other) {
      Map<String, ResultLine> mergedResultsBySite = new HashMap<>(resultsBySite);
      other.resultsBySite.forEach(mergedResultsBySite::putIfAbsent);
      return new QueryResult(mergedResultsBySite);
    }
  }
}
