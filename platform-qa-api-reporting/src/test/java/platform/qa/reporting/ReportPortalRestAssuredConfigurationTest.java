package platform.qa.reporting;

import static org.mockito.Mockito.*;

import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import java.util.List;
import org.junit.jupiter.api.*;

class ReportPortalRestAssuredConfigurationTest {

  @BeforeEach
  void resetRestAssured() {
    RestAssured.reset();
  }

  @Test
  void testStaticInitializer_runsWithoutExceptions() {
    Assertions.assertDoesNotThrow(
        ReportPortalRestAssuredConfiguration::configureReportPortalLogging);
  }

  // -------------------------------------------------------
  //  NORMAL configureReportPortalLogging()
  // -------------------------------------------------------
  @Test
  void testConfigureReportPortalLogging_addsSafeFilter() {
    // Act
    ReportPortalRestAssuredConfiguration.configureReportPortalLogging();

    // Assert
    List<Filter> filters = RestAssured.filters();
    Assertions.assertEquals(1, filters.size());
    Assertions.assertInstanceOf(SafeReportPortalLoggingFilter.class, filters.get(0));
  }

  // -------------------------------------------------------
  //  configureReportPortalLogging() — EXCEPTION path
  // -------------------------------------------------------
  @Test
  void testConfigureReportPortalLogging_handlesException() {
    // Mock RestAssured.reset() to throw
    try (var mock = mockStatic(RestAssured.class)) {
      mock.when(RestAssured::reset).thenThrow(new RuntimeException("boom"));

      Assertions.assertDoesNotThrow(
          ReportPortalRestAssuredConfiguration::configureReportPortalLogging);
    }
  }

  // -------------------------------------------------------
  //  forceConfigureReportPortalLogging() — EXCEPTION path
  // -------------------------------------------------------
  @Test
  void testForceConfigureReportPortalLogging_handlesException() {
    try (var mock = mockStatic(RestAssured.class)) {

      mock.when(RestAssured::filters).thenThrow(new RuntimeException("boom"));

      Assertions.assertDoesNotThrow(
          ReportPortalRestAssuredConfiguration::forceConfigureReportPortalLogging);
    }
  }

  // -------------------------------------------------------
  //  BODY_TYPE_MAP sanity test
  // -------------------------------------------------------
  @Test
  void testBodyTypeMap_containsExpectedEntries() {
    Assertions.assertFalse(ReportPortalRestAssuredConfiguration.BODY_TYPE_MAP.isEmpty());
    Assertions.assertTrue(
        ReportPortalRestAssuredConfiguration.BODY_TYPE_MAP.containsKey("image/png"));
  }
}
