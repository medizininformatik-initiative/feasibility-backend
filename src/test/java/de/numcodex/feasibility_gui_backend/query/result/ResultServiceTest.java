package de.numcodex.feasibility_gui_backend.query.result;

import static de.numcodex.feasibility_gui_backend.query.persistence.ResultType.ERROR;
import static de.numcodex.feasibility_gui_backend.query.persistence.ResultType.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("query")
@Tag("result")
@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

  public static final String SITE_NAME = "site-name-142848";
  public static final String SITE_NAME_1 = "site-name-143720";
  public static final String SITE_NAME_2 = "site-name-143723";
  public static final long QUERY_ID = 0L;
  public static final long QUERY_ID_1 = 1L;
  public static final long QUERY_ID_2 = 2L;

  private final Duration expiryTime = Duration.ofSeconds(1);

  private ResultService resultService;

  @BeforeEach
  void setUp() {
    resultService = new ResultService(expiryTime);
  }

  @Test
  void findSuccessfulByQuery_withSuccessResultLine() {
    var resultLine = new ResultLine(SITE_NAME, SUCCESS, 0L);
    resultService.addResultLine(QUERY_ID, resultLine);

    var resultLines = resultService.findSuccessfulByQuery(QUERY_ID);

    assertThat(resultLines).singleElement().isEqualTo(resultLine);
  }

  @Test
  void findSuccessfulByQuery_withErrorResultLine() {
    resultService.addResultLine(QUERY_ID, new ResultLine(SITE_NAME, ERROR, 0L));

    var resultLines = resultService.findSuccessfulByQuery(QUERY_ID);

    assertThat(resultLines).isEmpty();
  }

  @Test
  void testResultExpiry() throws Exception {
    resultService.addResultLine(QUERY_ID, new ResultLine(SITE_NAME, SUCCESS, 0L));
    Thread.sleep(expiryTime.plusMillis(250).toMillis());

    var resultLines = resultService.findSuccessfulByQuery(QUERY_ID);

    assertThat(resultLines).isEmpty();
  }

  @Test
  void testKeepsFirstResultPerSite() {
    ResultLine resultLine = new ResultLine(SITE_NAME, SUCCESS, 0L);
    resultService.addResultLine(QUERY_ID, resultLine);
    resultService.addResultLine(QUERY_ID, new ResultLine(SITE_NAME, ERROR, 0L));

    var resultLines = resultService.findSuccessfulByQuery(QUERY_ID);

    assertThat(resultLines).singleElement().isEqualTo(resultLine);
  }

  @Test
  void testStoresResultsFromMultipleSitesPerQuery() {
    ResultLine resultLine1 = new ResultLine(SITE_NAME_1, SUCCESS, 0L);
    resultService.addResultLine(QUERY_ID, resultLine1);
    ResultLine resultLine2 = new ResultLine(SITE_NAME_2, SUCCESS, 0L);
    resultService.addResultLine(QUERY_ID, resultLine2);

    var resultLines = resultService.findSuccessfulByQuery(QUERY_ID);

    assertThat(resultLines).contains(resultLine1, resultLine2);
  }

  @Test
  void testStoresResultsFromMultipleQueries() {
    ResultLine resultLine = new ResultLine(SITE_NAME, SUCCESS, 0L);
    resultService.addResultLine(QUERY_ID_1, resultLine);
    resultService.addResultLine(QUERY_ID_2, resultLine);

    var resultLines1 = resultService.findSuccessfulByQuery(QUERY_ID_1);
    var resultLines2 = resultService.findSuccessfulByQuery(QUERY_ID_1);

    assertThat(resultLines1).singleElement().isEqualTo(resultLine);
    assertThat(resultLines2).singleElement().isEqualTo(resultLine);
  }
}
