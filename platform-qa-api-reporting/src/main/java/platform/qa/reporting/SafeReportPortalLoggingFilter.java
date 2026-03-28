package platform.qa.reporting;

import com.epam.reportportal.service.ReportPortal;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.MultiPartSpecification;
import java.io.File;
import java.util.List;
import lombok.extern.log4j.Log4j2;

/**
 * A safe ReportPortal logging filter that handles multipart requests with File objects without
 * causing ClassCastException
 */
@Log4j2
public class SafeReportPortalLoggingFilter implements Filter {

  public static final String STR = "\n\t\t\t\t";
  private static final int MAX_LENGTH = 100;

  @Override
  public Response filter(
      FilterableRequestSpecification requestSpec,
      FilterableResponseSpecification responseSpec,
      FilterContext ctx) {
    try {
      // Log request safely
      logRequest(requestSpec);

      // Execute the request
      Response response = ctx.next(requestSpec, responseSpec);

      // Log response safely
      logResponse(response);

      return response;
    } catch (IllegalArgumentException e) {
      // If logging fails, just proceed with the request
      log.info("ReportPortal logging failed: {}, proceeding with request", e.getMessage());
      return ctx.next(requestSpec, responseSpec);
    }
  }

  void logRequest(FilterableRequestSpecification requestSpec) {
    try {
      StringBuilder requestLog =
          new StringBuilder()
              .append("Request method:\t")
              .append(requestSpec.getMethod())
              .append("\n")
              .append("Request URI:\t")
              .append(requestSpec.getURI())
              .append("\n");

      appendMultipartInfo(requestSpec, requestLog);
      appendHeadersInfo(requestSpec, requestLog);

      ReportPortal.emitLog(requestLog.toString(), "INFO", null);
    } catch (IllegalArgumentException e) {
      logSimpleRequest(requestSpec);
      log.info("ReportPortal logging failed: {}", e.getMessage());
    }
  }

  void appendMultipartInfo(FilterableRequestSpecification requestSpec, StringBuilder log) {
    List<MultiPartSpecification> multiParts = requestSpec.getMultiPartParams();
    if (multiParts == null || multiParts.isEmpty()) {
      return;
    }

    log.append("Multiparts:\t\t");
    multiParts.forEach(multiPart -> log.append(describeMultipart(multiPart)));
    log.append("\n");
  }

  String describeMultipart(MultiPartSpecification multiPart) {
    try {
      Object content = multiPart.getContent();
      if (content instanceof File file) {
        return String.format("[File: %s, size: %d bytes] ", file.getName(), file.length());
      } else if (content instanceof byte[] bytes) {
        return String.format("[Binary data: %d bytes] ", bytes.length);
      } else if (content != null) {
        String contentStr = content.toString();
        return String.format(
            "[%s: %s] ", content.getClass().getSimpleName(), abbreviate(contentStr, MAX_LENGTH));
      }
      return "[null] ";
    } catch (Exception e) {
      log.info("ReportPortal logging exception: {}", e.getMessage());
      return "[Multipart data] ";
    }
  }

  private void appendHeadersInfo(FilterableRequestSpecification requestSpec, StringBuilder log) {
    if (requestSpec.getHeaders() == null || requestSpec.getHeaders().asList().isEmpty()) {
      return;
    }

    log.append("Headers:\t\t");
    requestSpec
        .getHeaders()
        .asList()
        .forEach(header -> log.append(formatHeader(header)).append(STR));
    log.append("\n");
  }

  private String formatHeader(io.restassured.http.Header header) {
    String name = header.getName();
    String value = header.getValue();
    if (isSensitiveHeader(name)) {
      value = "***";
    }
    return name + "=" + value;
  }

  private boolean isSensitiveHeader(String name) {
    String lower = name.toLowerCase();
    return lower.contains("token") || lower.contains("authorization");
  }

  private String abbreviate(String text, int maxLength) {
    return text.length() > maxLength ? (text.substring(0, maxLength) + "...") : text;
  }

  private void logSimpleRequest(FilterableRequestSpecification requestSpec) {
    try {
      ReportPortal.emitLog(
          "Request: " + requestSpec.getMethod() + " " + requestSpec.getURI(), "INFO", null);
    } catch (IllegalArgumentException ex) {
      log.info("ReportPortal request logging failed: {}", ex.getMessage());
    }
  }

  void logResponse(Response response) {
    try {
      StringBuilder responseLog = new StringBuilder();
      responseLog
          .append("Response status:\t")
          .append(response.getStatusCode())
          .append(" ")
          .append(response.getStatusLine())
          .append("\n");

      // Add response headers safely
      if (response.getHeaders() != null && !response.getHeaders().asList().isEmpty()) {
        responseLog.append("Response headers:\t");
        response
            .getHeaders()
            .asList()
            .forEach(
                header ->
                    responseLog
                        .append(header.getName())
                        .append("=")
                        .append(header.getValue())
                        .append(STR));
        responseLog.append("\n");
      }

      // Add response body safely
      try {
        String body = response.getBody().asString();
        if (body != null && !body.trim().isEmpty()) {
          responseLog.append("Response body:\n").append(body);
        }
      } catch (Exception e) {
        log.info("ReportPortal logging failed: {}, appending to responseLog", e.getMessage());
        responseLog.append("Response body: [Binary or non-text content]");
      }

      ReportPortal.emitLog(responseLog.toString(), "INFO", null);
    } catch (IllegalArgumentException e) {
      // If response logging fails, log a simple message
      ReportPortal.emitLog(
          "Response: " + response.getStatusCode() + " " + response.getStatusLine(), "INFO", null);
      log.info("Response logging failed: {}", e.getMessage());
    }
  }
}
