package platform.qa.reporting;

import com.epam.reportportal.service.ReportPortal;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.MultiPartSpecification;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class SafeReportPortalLoggingFilter implements Filter {

  public static final String STR = "\n\t\t\t\t";
  private static final int MAX_LENGTH = 2000;

  @Override
  public Response filter(
      FilterableRequestSpecification requestSpec,
      FilterableResponseSpecification responseSpec,
      FilterContext ctx) {

    try {
      logRequest(requestSpec);
    } catch (Exception e) {
      log.info("Request logging failed: {}", e.getMessage());
    }

    Response response = ctx.next(requestSpec, responseSpec);

    try {
      logResponse(response);
    } catch (Exception e) {
      log.info("Response logging failed: {}", e.getMessage());
    }

    return response;
  }

  // ================= REQUEST =================

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

      appendHeadersInfo(requestSpec, requestLog);
      appendQueryParams(requestSpec, requestLog);
      appendBody(requestSpec, requestLog);
      appendMultipartInfo(requestSpec, requestLog);

      String log = "\n===== REQUEST =====\n" + requestLog + "\n===================\n";
      ReportPortal.emitLog(log, "INFO", new Date());

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
    multiParts.forEach(part -> log.append(describeMultipart(part)));
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
        return String.format(
            "[%s: %s] ", content.getClass().getSimpleName(), abbreviate(content.toString()));
      }

      return "[null] ";
    } catch (Exception e) {
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

  private void appendQueryParams(FilterableRequestSpecification requestSpec, StringBuilder log) {
    Map<String, ?> params = requestSpec.getQueryParams();
    if (params == null || params.isEmpty()) {
      return;
    }

    log.append("Query params:\t");
    params.forEach((k, v) -> log.append(k).append("=").append(v).append(STR));
    log.append("\n");
  }

  private void appendBody(FilterableRequestSpecification requestSpec, StringBuilder log) {
    if (requestSpec.getBody() == null) {
      return;
    }

    try {
      String body = requestSpec.getBody().toString();
      log.append("Body:\n").append(abbreviate(body)).append("\n");
    } catch (Exception e) {
      log.append("Body: [unavailable]\n");
    }
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

  private String abbreviate(String text) {
    if (text == null) {
      return "";
    }
    return text.length() > MAX_LENGTH ? text.substring(0, MAX_LENGTH) + "..." : text;
  }

  private void logSimpleRequest(FilterableRequestSpecification requestSpec) {
    try {
      ReportPortal.emitLog(
          "Request: " + requestSpec.getMethod() + " " + requestSpec.getURI(), "INFO", new Date());
    } catch (IllegalArgumentException ex) {
      log.info("ReportPortal request logging failed: {}", ex.getMessage());
    }
  }

  // ================= RESPONSE =================

  void logResponse(Response response) {
    try {
      StringBuilder responseLog = new StringBuilder();

      responseLog
          .append("Response status:\t")
          .append(response.getStatusCode())
          .append(" ")
          .append(response.getStatusLine())
          .append("\n");

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

      try {
        String body = response.getBody().asString();
        if (body != null && !body.isBlank()) {
          responseLog.append("Response body:\n").append(abbreviate(body));
        }
      } catch (Exception e) {
        responseLog.append("Response body: [binary/unreadable]");
      }

      String log = "\n===== RESPONSE =====\n" + responseLog + "\n====================\n";
      ReportPortal.emitLog(log, "INFO", new Date());

    } catch (IllegalArgumentException e) {
      log.warn("Response logging failed", e);
      ReportPortal.emitLog(
          "Response: " + response.getStatusCode() + " " + response.getStatusLine(),
          "INFO",
          new Date());
    }
  }
}
