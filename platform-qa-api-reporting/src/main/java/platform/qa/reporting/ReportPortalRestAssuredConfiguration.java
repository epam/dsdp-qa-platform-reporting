package platform.qa.reporting;

import com.epam.reportportal.formatting.http.entities.BodyType;
import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;

/**
 * Configuration class for ReportPortal RestAssured logging that handles file uploads properly to
 * prevent ClassCastException when logging multipart requests with File objects
 */
@Log4j2
public class ReportPortalRestAssuredConfiguration {

  /** Body type mapping to handle file uploads and binary content properly */
  public static final Map<String, BodyType> BODY_TYPE_MAP =
      Map.of(
          "application/octet-stream", BodyType.BINARY,
          "multipart/form-data", BodyType.BINARY,
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
              BodyType.BINARY,
          "application/pdf", BodyType.BINARY,
          "image/jpeg", BodyType.BINARY,
          "image/png", BodyType.BINARY,
          "image/jpg", BodyType.BINARY,
          "application/zip", BodyType.BINARY);

  /**
   * Static initializer to automatically configure ReportPortal logging when this class is loaded
   */
  static {
    configureReportPortalLogging();
  }

  /**
   * Configures RestAssured with a safe ReportPortal logging filter that handles file uploads
   * properly
   */
  public static void configureReportPortalLogging() {
    try {
      // Clear all existing filters first
      RestAssured.reset();

      // Add only our safe ReportPortal filter
      RestAssured.filters(new SafeReportPortalLoggingFilter());


      log.info("Safe ReportPortal RestAssured logging configured with file upload support");
    } catch (Exception e) {
      log.info("Failed to configure ReportPortal RestAssured logging: {}", e.getMessage());
      // Don't throw exception - we don't want to break tests if logging configuration fails
    }
  }

  /** Alternative method that tries to remove problematic filters more aggressively */
  public static void forceConfigureReportPortalLogging() {
    try {
      // Get all current filters
      List<Filter> allFilters = new ArrayList<>(RestAssured.filters());

      // Remove ALL ReportPortal related filters
      allFilters.removeIf(
          filter -> {
            String className = filter.getClass().getName();
            return className.contains("ReportPortal") || className.contains("reportportal");
          });

      // Add our safe filter
      allFilters.add(new SafeReportPortalLoggingFilter());

      // Replace with our safe list
      RestAssured.replaceFiltersWith(allFilters);

      log.info("Force-configured safe ReportPortal RestAssured logging");
    } catch (Exception e) {
      log.error("Failed to force-configure ReportPortal RestAssured logging: {}", e.getMessage());
    }
  }
}
