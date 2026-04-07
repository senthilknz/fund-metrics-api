package com.fundmetrics.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Servlet filter that logs every inbound HTTP request and its outbound response at INFO level.
 *
 * <p>Uses {@link ContentCachingRequestWrapper} and {@link ContentCachingResponseWrapper} to
 * buffer the body so it can be read for logging after the filter chain processes it, without
 * consuming the stream that the controller also needs to read.
 *
 * <p>Extends {@link OncePerRequestFilter} to guarantee the filter runs exactly once per
 * request, even in forward/include dispatch scenarios.
 *
 * <p>Log format:
 * <pre>
 *   &gt;&gt;&gt; GET /api/v1/funds | query="" | content-type=
 *   &lt;&lt;&lt; 200 | duration=12ms | content-type=application/json | body={...}
 * </pre>
 *
 * <p>Response bodies longer than {@value #MAX_BODY_LOG_LENGTH} characters are truncated
 * to avoid flooding logs with large payloads.
 *
 * <p>Actuator, Swagger UI, and OpenAPI spec paths are excluded — see {@link #shouldNotFilter}.
 */
@Component
@Order(1)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    /** Maximum number of characters to log from a request or response body before truncating. */
    private static final int MAX_BODY_LOG_LENGTH = 2000;

    /**
     * Wraps the request and response with caching wrappers, executes the filter chain,
     * then logs the request and response details. The response body is copied back to
     * the original response via {@code copyBodyToResponse()} after logging.
     *
     * @param request     the incoming HTTP request
     * @param response    the outgoing HTTP response
     * @param filterChain the remaining filter chain
     * @throws ServletException if a downstream filter or servlet throws a servlet error
     * @throws IOException      if an I/O error occurs reading/writing the request or response
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startMs = System.currentTimeMillis();
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long durationMs = System.currentTimeMillis() - startMs;
            logRequest(wrappedRequest);
            logResponse(wrappedResponse, durationMs);
            // Must copy the cached body back; without this the client receives an empty response.
            wrappedResponse.copyBodyToResponse();
        }
    }

    /**
     * Logs the inbound request: method, URI, query string, content type, and body (if present).
     *
     * @param request the caching wrapper around the original request
     */
    private void logRequest(ContentCachingRequestWrapper request) {
        String body = extractBody(request.getContentAsByteArray());
        if (body.isEmpty()) {
            log.info(">>> {} {} | query=\"{}\" | content-type={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    nullToEmpty(request.getQueryString()),
                    nullToEmpty(request.getContentType()));
        } else {
            log.info(">>> {} {} | query=\"{}\" | content-type={} | body={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    nullToEmpty(request.getQueryString()),
                    nullToEmpty(request.getContentType()),
                    body);
        }
    }

    /**
     * Logs the outbound response: HTTP status, duration in milliseconds, content type,
     * and response body (if present).
     *
     * @param response   the caching wrapper around the original response
     * @param durationMs wall-clock time from request start to filter chain completion
     */
    private void logResponse(ContentCachingResponseWrapper response, long durationMs) {
        String body = extractBody(response.getContentAsByteArray());
        if (body.isEmpty()) {
            log.info("<<< {} | duration={}ms | content-type={}",
                    response.getStatus(),
                    durationMs,
                    nullToEmpty(response.getContentType()));
        } else {
            log.info("<<< {} | duration={}ms | content-type={} | body={}",
                    response.getStatus(),
                    durationMs,
                    nullToEmpty(response.getContentType()),
                    body);
        }
    }

    /**
     * Converts a byte array to a UTF-8 string, trimming whitespace and truncating
     * to {@value #MAX_BODY_LOG_LENGTH} characters if necessary.
     *
     * @param bytes raw body bytes from the caching wrapper
     * @return the body as a string, or an empty string if the array is null or empty
     */
    private String extractBody(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        String body = new String(bytes, StandardCharsets.UTF_8).trim();
        if (body.length() > MAX_BODY_LOG_LENGTH) {
            return body.substring(0, MAX_BODY_LOG_LENGTH) + "... [truncated]";
        }
        return body;
    }

    /**
     * Returns the value as-is, or an empty string if it is {@code null}.
     * Avoids "null" appearing literally in log output.
     *
     * @param value a potentially null string
     * @return the value, or {@code ""} if null
     */
    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    /**
     * Excludes infrastructure paths from logging to reduce noise in production logs.
     * The following prefixes are excluded:
     * <ul>
     *   <li>{@code /actuator} — Spring Boot health and info endpoints</li>
     *   <li>{@code /swagger} — Swagger UI assets</li>
     *   <li>{@code /v3/api-docs} — OpenAPI spec endpoint</li>
     * </ul>
     *
     * @param request the incoming request
     * @return {@code true} if the request path should bypass this filter
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.startsWith("/swagger") || path.startsWith("/v3/api-docs");
    }
}
