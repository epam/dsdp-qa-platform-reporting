package platform.qa.reporting;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit extension that ensures ReportPortal RestAssured logging is properly configured before tests
 * run to handle file uploads without ClassCastException
 */
public class ReportPortalConfigurationExtension implements BeforeAllCallback, BeforeEachCallback {

  @Override
  public void beforeAll(ExtensionContext context) {
    // Configure ReportPortal RestAssured logging before all tests
    ReportPortalRestAssuredConfiguration.configureReportPortalLogging();
    // Also try the force configuration as backup
    ReportPortalRestAssuredConfiguration.forceConfigureReportPortalLogging();
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    // Ensure configuration is still in place before each test
    ReportPortalRestAssuredConfiguration.forceConfigureReportPortalLogging();
  }
}
