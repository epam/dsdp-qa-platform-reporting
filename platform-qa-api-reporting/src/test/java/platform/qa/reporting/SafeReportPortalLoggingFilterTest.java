package platform.qa.reporting;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.epam.reportportal.service.ReportPortal;
import io.restassured.filter.FilterContext;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.MultiPartSpecification;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

class SafeReportPortalLoggingFilterTest {

  private SafeReportPortalLoggingFilter filter;
  private FilterableRequestSpecification request;
  private FilterableResponseSpecification responseSpec;
  private FilterContext ctx;
  private Response response;

  private MockedStatic<ReportPortal> reportPortalMock;

  @BeforeEach
  void setup() {
    filter = new SafeReportPortalLoggingFilter();

    request = mock(FilterableRequestSpecification.class);
    responseSpec = mock(FilterableResponseSpecification.class);
    ctx = mock(FilterContext.class);
    response = mock(Response.class);

    when(ctx.next(any(), any())).thenReturn(response);

    reportPortalMock = mockStatic(ReportPortal.class);
  }

  @AfterEach
  void tearDown() {
    reportPortalMock.close();
  }

  // ---------------------------------------------------------
  //                 TEST: MAIN filter() METHOD
  // ---------------------------------------------------------

  @Test
  void testAppendBody_withValidBody_appendsBody() throws Exception {
    // Arrange
    Object body =
        new Object() {
          @Override
          public String toString() {
            return "my-body";
          }
        };

    when(request.getBody()).thenReturn(body);

    StringBuilder sb = new StringBuilder();

    // Act
    invokeAppendBody(request, sb);

    // Assert
    String result = sb.toString();

    Assertions.assertTrue(result.contains("Body:\n"));
    Assertions.assertTrue(result.contains("my-body"));
  }

  @Test
  void testAppendBody_whenToStringThrows_shouldAppendUnavailable() throws Exception {
    // Arrange
    Object brokenBody =
        new Object() {
          @Override
          public String toString() {
            throw new RuntimeException("boom");
          }
        };

    when(request.getBody()).thenReturn(brokenBody);

    StringBuilder sb = new StringBuilder();

    // Act
    invokeAppendBody(request, sb);

    // Assert
    Assertions.assertTrue(sb.toString().contains("Body: [unavailable]"));
  }

  @Test
  void testAppendBody_nullBody_shouldDoNothing() throws Exception {
    when(request.getBody()).thenReturn(null);

    StringBuilder sb = new StringBuilder();

    invokeAppendBody(request, sb);

    Assertions.assertEquals("", sb.toString());
  }

  @Test
  void testFilter_normalFlow_logsRequestAndResponse() {
    when(request.getMethod()).thenReturn("POST");
    when(request.getURI()).thenReturn("http://test");
    when(request.getMultiPartParams()).thenReturn(List.of());
    when(request.getHeaders()).thenReturn(new Headers());

    when(response.getStatusCode()).thenReturn(200);
    when(response.getStatusLine()).thenReturn("OK");
    when(response.getHeaders()).thenReturn(new Headers());
    when(response.getBody()).thenReturn(mock(io.restassured.response.ResponseBody.class));
    when(response.getBody().asString()).thenReturn("Success");

    Response out = filter.filter(request, responseSpec, ctx);

    Assertions.assertEquals(response, out);
    reportPortalMock.verify(
        () -> ReportPortal.emitLog(anyString(), eq("INFO"), any(Date.class)), atLeastOnce());
  }

  // ---------------------------------------------------------
  //                 TEST: logRequest Fallback Branch
  // ---------------------------------------------------------

  @Test
  void testLogRequest_fallbackWhenIllegalArgument() {
    when(request.getMethod()).thenReturn("GET");
    when(request.getURI()).thenReturn("http://fallback");

    SafeReportPortalLoggingFilter spyFilter = spy(filter);
    doThrow(new IllegalArgumentException("fail")).when(spyFilter).appendMultipartInfo(any(), any());

    spyFilter.filter(request, responseSpec, ctx);

    reportPortalMock.verify(
        () -> ReportPortal.emitLog(contains("Request:"), eq("INFO"), any(Date.class)));
  }

  @Test
  void testFilter_requestLoggingThrowsException_shouldNotFailFlow() {
    // Arrange
    SafeReportPortalLoggingFilter spyFilter = spy(filter);

    doThrow(new RuntimeException("boom")).when(spyFilter).logRequest(any());

    when(ctx.next(any(), any())).thenReturn(response);

    // Act
    Response result = spyFilter.filter(request, responseSpec, ctx);

    // Assert
    Assertions.assertEquals(response, result);
    verify(ctx).next(any(), any());
  }

  @Test
  void testAppendQueryParams_withParams_appendsCorrectly() throws Exception {
    // Arrange
    when(request.getQueryParams()).thenReturn(Map.of("param1", "value1", "param2", "value2"));

    StringBuilder sb = new StringBuilder();

    // Act
    invokeAppendQueryParams(request, sb);

    // Assert
    String result = sb.toString();

    Assertions.assertTrue(result.startsWith("Query params:\t"));
    Assertions.assertTrue(result.contains("param1=value1"));
    Assertions.assertTrue(result.contains("param2=value2"));
    Assertions.assertTrue(result.endsWith("\n"));
  }

  @Test
  void testAppendQueryParams_empty_shouldDoNothing() throws Exception {
    when(request.getQueryParams()).thenReturn(Map.of());

    StringBuilder sb = new StringBuilder();

    invokeAppendQueryParams(request, sb);

    Assertions.assertEquals("", sb.toString());
  }

  // ---------------------------------------------------------
  //                 MULTIPART HANDLING
  // ---------------------------------------------------------

  @Test
  void testDescribeMultipart_file() {
    MultiPartSpecification part = mock(MultiPartSpecification.class);
    File temp = mock(File.class);
    when(temp.getName()).thenReturn("test.txt");
    when(temp.length()).thenReturn(123L);
    when(part.getContent()).thenReturn(temp);

    String result = invokeDescribe(part);

    Assertions.assertTrue(result.contains("File: test.txt"));
    Assertions.assertTrue(result.contains("123"));
  }

  @Test
  void testDescribeMultipart_bytes() {
    MultiPartSpecification part = mock(MultiPartSpecification.class);
    when(part.getContent()).thenReturn("hello".getBytes());

    String result = invokeDescribe(part);

    Assertions.assertTrue(result.contains("Binary data"));
  }

  @Test
  void testDescribeMultipart_string() {
    MultiPartSpecification part = mock(MultiPartSpecification.class);
    when(part.getContent()).thenReturn("Hello world");

    String result = invokeDescribe(part);

    Assertions.assertTrue(result.contains("String"));
    Assertions.assertTrue(result.contains("Hello world"));
  }

  @Test
  void testDescribeMultipart_null() {
    MultiPartSpecification part = mock(MultiPartSpecification.class);
    when(part.getContent()).thenReturn(null);

    String result = invokeDescribe(part);

    Assertions.assertEquals("[null] ", result);
  }

  @Test
  void testDescribeMultipart_exceptionDuringProcessing() {
    MultiPartSpecification part = mock(MultiPartSpecification.class);
    when(part.getContent()).thenThrow(new RuntimeException("err"));

    String result = invokeDescribe(part);

    Assertions.assertEquals("[Multipart data] ", result);
  }

  // ---------------------------------------------------------
  //                 HEADERS HANDLING
  // ---------------------------------------------------------

  @Test
  void testFormatHeader_sensitiveHeadersAreMasked() {
    Header header = new Header("Authorization", "secret");

    String result = invokeFormat(header);

    Assertions.assertEquals("Authorization=***", result);
  }

  @Test
  void testFormatHeader_regularHeadersShown() {
    Header header = new Header("User-Agent", "Chrome");

    String result = invokeFormat(header);

    Assertions.assertEquals("User-Agent=Chrome", result);
  }

  // ---------------------------------------------------------
  //                 ABBREVIATE
  // ---------------------------------------------------------

  @Test
  void testAbbreviate_shortStringNotCut() {
    String result = invokeAbbrev("short");
    Assertions.assertEquals("short", result);
  }

  @Test
  void testAbbreviate_longStringCut() {
    String longStr = "aaaaaaaa".repeat(275);
    String result = invokeAbbrev(longStr);

    Assertions.assertTrue(result.length() < longStr.length());
    Assertions.assertTrue(result.endsWith("..."));
  }

  // ---------------------------------------------------------
  //                 RESPONSE LOGGING
  // ---------------------------------------------------------

  @Test
  void testLogResponse_normalPath() {
    Response responseMock = mock(Response.class);
    when(responseMock.getStatusCode()).thenReturn(200);
    when(responseMock.getStatusLine()).thenReturn("OK");

    Headers headers = new Headers(new Header("h1", "v1"));
    when(responseMock.getHeaders()).thenReturn(headers);

    var body = mock(io.restassured.response.ResponseBody.class);
    when(body.asString()).thenReturn("response-body");
    when(responseMock.getBody()).thenReturn(body);

    invokeLogResponse(responseMock);

    reportPortalMock.verify(() -> ReportPortal.emitLog(anyString(), eq("INFO"), any(Date.class)));
  }

  @Test
  void testLogResponse_illegalArgumentFallback() {
    Response responseMock = mock(Response.class);
    when(responseMock.getStatusCode()).thenReturn(500);
    when(responseMock.getStatusLine()).thenReturn("ERR");

    SafeReportPortalLoggingFilter spyFilter = spy(filter);
    doThrow(new IllegalArgumentException("bad")).when(spyFilter).logResponse(any());

    // fallback via filter()
    spyFilter.filter(request, responseSpec, ctx);

    reportPortalMock.verify(
        () -> ReportPortal.emitLog(anyString(), eq("INFO"), any(Date.class)), atLeastOnce());
  }

  @Test
  void testAppendHeadersInfo_nullHeaders_returnsImmediately() throws Exception {
    when(request.getHeaders()).thenReturn(null);

    StringBuilder sb = new StringBuilder();

    invokeAppendHeadersInfo(request, sb);

    Assertions.assertEquals("", sb.toString());
  }

  @Test
  void testAppendHeadersInfo_emptyHeaders_returnsImmediately() throws Exception {
    when(request.getHeaders()).thenReturn(new Headers());

    StringBuilder sb = new StringBuilder();

    invokeAppendHeadersInfo(request, sb);

    Assertions.assertEquals("", sb.toString());
  }

  @Test
  void testAppendHeadersInfo_headersPresent_appendsFormattedHeaders() throws Exception {
    Header h1 = new Header("User-Agent", "Chrome");
    Header h2 = new Header("Authorization", "secret-token");

    Headers headers = new Headers(List.of(h1, h2));

    when(request.getHeaders()).thenReturn(headers);

    StringBuilder sb = new StringBuilder();

    invokeAppendHeadersInfo(request, sb);

    String result = sb.toString();

    Assertions.assertTrue(result.startsWith("Headers:\t\t"));
    Assertions.assertTrue(result.contains("User-Agent=Chrome"));
    Assertions.assertTrue(result.contains("Authorization=***"));
    Assertions.assertTrue(result.endsWith("\n"));
  }

  @Test
  void testAppendMultipartInfo_withMultiparts_appendsAllExpectedStrings() throws Exception {
    // Arrange
    MultiPartSpecification part1 = mock(MultiPartSpecification.class);
    MultiPartSpecification part2 = mock(MultiPartSpecification.class);

    SafeReportPortalLoggingFilter spyFilter = spy(filter);
    doReturn("[mp1]").when(spyFilter).describeMultipart(part1);
    doReturn("[mp2]").when(spyFilter).describeMultipart(part2);

    when(request.getMultiPartParams()).thenReturn(List.of(part1, part2));

    StringBuilder sb = new StringBuilder();

    // Act
    invokeAppendMultipartInfo(spyFilter, request, sb);

    // Assert
    String result = sb.toString();

    Assertions.assertTrue(result.startsWith("Multiparts:\t\t"));

    Assertions.assertTrue(result.contains("[mp1]"));
    Assertions.assertTrue(result.contains("[mp2]"));

    Assertions.assertTrue(result.endsWith("\n"));
  }

  // ---------------------------------------------------------
  //                 PRIVATE METHOD INVOKERS
  // ---------------------------------------------------------

  private String invokeDescribe(MultiPartSpecification part) {
    try {
      var m =
          SafeReportPortalLoggingFilter.class.getDeclaredMethod(
              "describeMultipart", MultiPartSpecification.class);
      m.setAccessible(true);
      return (String) m.invoke(filter, part);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void invokeAppendMultipartInfo(
      SafeReportPortalLoggingFilter f, FilterableRequestSpecification req, StringBuilder sb)
      throws Exception {
    var m =
        SafeReportPortalLoggingFilter.class.getDeclaredMethod(
            "appendMultipartInfo", FilterableRequestSpecification.class, StringBuilder.class);
    m.setAccessible(true);
    m.invoke(f, req, sb);
  }

  private void invokeAppendHeadersInfo(FilterableRequestSpecification req, StringBuilder sb)
      throws Exception {
    var m =
        SafeReportPortalLoggingFilter.class.getDeclaredMethod(
            "appendHeadersInfo", FilterableRequestSpecification.class, StringBuilder.class);
    m.setAccessible(true);
    m.invoke(filter, req, sb);
  }

  private String invokeFormat(Header header) {
    try {
      var m = SafeReportPortalLoggingFilter.class.getDeclaredMethod("formatHeader", Header.class);
      m.setAccessible(true);
      return (String) m.invoke(filter, header);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String invokeAbbrev(String text) {
    try {
      var m = SafeReportPortalLoggingFilter.class.getDeclaredMethod("abbreviate", String.class);
      m.setAccessible(true);
      return (String) m.invoke(filter, text);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void invokeLogResponse(Response r) {
    try {
      var m = SafeReportPortalLoggingFilter.class.getDeclaredMethod("logResponse", Response.class);
      m.setAccessible(true);
      m.invoke(filter, r);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void invokeAppendQueryParams(FilterableRequestSpecification req, StringBuilder sb)
      throws Exception {

    var m =
        SafeReportPortalLoggingFilter.class.getDeclaredMethod(
            "appendQueryParams", FilterableRequestSpecification.class, StringBuilder.class);

    m.setAccessible(true);
    m.invoke(filter, req, sb);
  }

  private void invokeAppendBody(FilterableRequestSpecification req, StringBuilder sb)
      throws Exception {

    var m =
        SafeReportPortalLoggingFilter.class.getDeclaredMethod(
            "appendBody", FilterableRequestSpecification.class, StringBuilder.class);

    m.setAccessible(true);
    m.invoke(filter, req, sb);
  }
}
