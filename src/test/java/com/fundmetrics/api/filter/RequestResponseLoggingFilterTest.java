package com.fundmetrics.api.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link RequestResponseLoggingFilter}.
 *
 * <p>Protected methods ({@code shouldNotFilter}) are tested directly via instantiation
 * since the test is in the same package. Private methods ({@code extractBody},
 * {@code nullToEmpty}) are tested via reflection to achieve 100% coverage.
 * Integration behaviour is verified through {@link MockMvc}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RequestResponseLoggingFilterTest {

    /** Direct instance for unit-testing protected/private methods. */
    private final RequestResponseLoggingFilter filter = new RequestResponseLoggingFilter();

    @Autowired
    private MockMvc mockMvc;

    // -------------------------------------------------------------------------
    // shouldNotFilter() — excluded paths return true
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "shouldNotFilter(\"{0}\") == true")
    @ValueSource(strings = {
            "/actuator",
            "/actuator/health",
            "/actuator/info",
            "/swagger-ui.html",
            "/swagger-ui/index.html",
            "/swagger-resources",
            "/v3/api-docs",
            "/v3/api-docs/swagger-config"
    })
    void shouldNotFilter_excludedPaths_returnsTrue(String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @ParameterizedTest(name = "shouldNotFilter(\"{0}\") == false")
    @ValueSource(strings = {
            "/api/v1/funds",
            "/api/v1/funds/chooser",
            "/api/v1/funds/history",
            "/api/v1/funds/preview",
            "/"
    })
    void shouldNotFilter_apiPaths_returnsFalse(String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    // -------------------------------------------------------------------------
    // doFilterInternal() — integration via MockMvc
    // -------------------------------------------------------------------------

    @Test
    void doFilterInternal_responseBodyReachesClient_chooser() throws Exception {
        mockMvc.perform(get("/api/v1/funds/chooser"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    @Test
    void doFilterInternal_responseBodyReachesClient_history() throws Exception {
        mockMvc.perform(get("/api/v1/funds/history"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    @Test
    void doFilterInternal_responseBodyReachesClient_activeFunds() throws Exception {
        mockMvc.perform(get("/api/v1/funds"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    @Test
    void doFilterInternal_postRequestIsLogged_activate() throws Exception {
        // POST with request body exercises the request-body logging path
        mockMvc.perform(post("/api/v1/funds/activate").param("version", "2025.04.01"))
                .andExpect(status().isOk());
    }

    @Test
    void doFilterInternal_404ResponseIsLogged() throws Exception {
        // Non-200 response exercises the response status logging path
        mockMvc.perform(get("/api/v1/funds/preview").param("date", "2020-01-01"))
                .andExpect(status().isNotFound());
    }

    @Test
    void doFilterInternal_400ResponseIsLogged() throws Exception {
        mockMvc.perform(get("/api/v1/funds/preview").param("date", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // extractBody() — private method via reflection
    // -------------------------------------------------------------------------

    private String invokeExtractBody(byte[] bytes) throws Exception {
        Method method = RequestResponseLoggingFilter.class
                .getDeclaredMethod("extractBody", byte[].class);
        method.setAccessible(true);
        try {
            return (String) method.invoke(filter, (Object) bytes);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    void extractBody_returnsEmptyStringForNull() throws Exception {
        assertThat(invokeExtractBody(null)).isEmpty();
    }

    @Test
    void extractBody_returnsEmptyStringForEmptyArray() throws Exception {
        assertThat(invokeExtractBody(new byte[0])).isEmpty();
    }

    @Test
    void extractBody_returnsBodyWithinMaxLength() throws Exception {
        byte[] body = "hello world".getBytes(StandardCharsets.UTF_8);
        assertThat(invokeExtractBody(body)).isEqualTo("hello world");
    }

    @Test
    void extractBody_trimsWhitespace() throws Exception {
        byte[] body = "  hello  ".getBytes(StandardCharsets.UTF_8);
        assertThat(invokeExtractBody(body)).isEqualTo("hello");
    }

    @Test
    void extractBody_truncatesBodyExceedingMaxLength() throws Exception {
        // MAX_BODY_LOG_LENGTH is 2000 — create a body of 2001 chars
        String longBody = "x".repeat(2001);
        byte[] bytes = longBody.getBytes(StandardCharsets.UTF_8);
        String result = invokeExtractBody(bytes);
        assertThat(result).endsWith("... [truncated]");
        assertThat(result).startsWith("x".repeat(2000));
    }

    @Test
    void extractBody_doesNotTruncateBodyAtExactMaxLength() throws Exception {
        String exactBody = "x".repeat(2000);
        byte[] bytes = exactBody.getBytes(StandardCharsets.UTF_8);
        String result = invokeExtractBody(bytes);
        assertThat(result).doesNotContain("[truncated]");
        assertThat(result).hasSize(2000);
    }

    // -------------------------------------------------------------------------
    // nullToEmpty() — private method via reflection
    // -------------------------------------------------------------------------

    private String invokeNullToEmpty(String value) throws Exception {
        Method method = RequestResponseLoggingFilter.class
                .getDeclaredMethod("nullToEmpty", String.class);
        method.setAccessible(true);
        try {
            return (String) method.invoke(filter, value);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    void nullToEmpty_returnsEmptyStringForNull() throws Exception {
        assertThat(invokeNullToEmpty(null)).isEmpty();
    }

    @Test
    void nullToEmpty_returnsValueForNonNull() throws Exception {
        assertThat(invokeNullToEmpty("application/json")).isEqualTo("application/json");
    }

    @Test
    void nullToEmpty_returnsEmptyStringForEmptyInput() throws Exception {
        assertThat(invokeNullToEmpty("")).isEmpty();
    }
}
