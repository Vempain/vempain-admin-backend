package fi.poltsi.vempain.admin.security;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.filter.OncePerRequestFilter;

import fi.poltsi.vempain.admin.api.Constants;
import io.micrometer.tracing.Tracer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller Test Class (CTC) for the trace propagation: a request that carries a W3C {@code traceparent} (from Traefik, the admin SPA or the file backend's
 * Feign calls) is handled inside that trace, so that the server span and every log line of the request carry the caller's trace id. The tracer is the real
 * OpenTelemetry bridge of the application; only the export is absent (no endpoint is configured in tests).
 */
@SpringBootTest(properties = { "management.tracing.enabled=true", "management.tracing.export.enabled=true", "management.tracing.sampling.probability=1.0" })
@AutoConfigureMockMvc
class TraceContinuationCTC {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String PARENT_SPAN_ID = "00f067aa0ba902b7";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TraceCapture capture;

    @BeforeEach
    void reset() {
        capture.reset();
    }

    @Test
    void requestWithTraceparentContinuesTheCallersTrace() throws Exception {
        // An invalid API token: the request runs through ApiTokenAuthenticationFilter and ends as 401, still inside the trace
        mockMvc.perform(get("/content-management/file/site-file/users").header("traceparent", "00-" + TRACE_ID + "-" + PARENT_SPAN_ID + "-01")
            .header(Constants.API_TOKEN_HEADER, "vat_invalid"))
            .andExpect(status().isUnauthorized());

        assertThat(capture.traceId.get()).as("the server span belongs to the caller's trace")
            .isEqualTo(TRACE_ID);
        assertThat(capture.parentSpanId.get()).as("the caller's span is the parent of the server span")
            .isEqualTo(PARENT_SPAN_ID);
        assertThat(capture.spanId.get()).isNotEqualTo(PARENT_SPAN_ID);
        assertThat(capture.mdcTraceId.get()).as("log lines of the request carry the trace id through the MDC")
            .isEqualTo(TRACE_ID);
        assertThat(capture.mdcSpanId.get()).isEqualTo(capture.spanId.get());
    }

    @Test
    void requestWithoutTraceparentStartsANewTrace() throws Exception {
        mockMvc.perform(get("/content-management/file/site-file/users").header(Constants.API_TOKEN_HEADER, "vat_invalid"))
            .andExpect(status().isUnauthorized());

        assertThat(capture.traceId.get()).isNotNull()
            .hasSize(32)
            .isNotEqualTo(TRACE_ID);
        assertThat(capture.parentSpanId.get()).isNull();
        assertThat(capture.mdcTraceId.get()).isEqualTo(capture.traceId.get());
    }

    /**
     * Records the current span and the MDC as seen by the filters that run after the observation filter and before Spring Security, where the API token filter
     * also runs.
     */
    static class TraceCapture extends OncePerRequestFilter {

        final AtomicReference<String> traceId = new AtomicReference<>();
        final AtomicReference<String> spanId = new AtomicReference<>();
        final AtomicReference<String> parentSpanId = new AtomicReference<>();
        final AtomicReference<String> mdcTraceId = new AtomicReference<>();
        final AtomicReference<String> mdcSpanId = new AtomicReference<>();
        private final Tracer tracer;

        TraceCapture(Tracer tracer) {
            this.tracer = tracer;
        }

        void reset() {
            traceId.set(null);
            spanId.set(null);
            parentSpanId.set(null);
            mdcTraceId.set(null);
            mdcSpanId.set(null);
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            var span = tracer.currentSpan();
            if (span != null) {
                traceId.set(span.context()
                    .traceId());
                spanId.set(span.context()
                    .spanId());
                parentSpanId.set(span.context()
                    .parentId());
            }
            mdcTraceId.set(MDC.get("traceId"));
            mdcSpanId.set(MDC.get("spanId"));
            filterChain.doFilter(request, response);
        }
    }

    @TestConfiguration
    static class TraceCaptureConfiguration {

        @Bean
        TraceCapture traceCapture(Tracer tracer) {
            return new TraceCapture(tracer);
        }

        /**
         * Right after Boot's server observation filter (highest precedence + 1), so that the capture sees the server span like every later filter.
         */
        @Bean
        FilterRegistrationBean<TraceCapture> traceCaptureRegistration(TraceCapture traceCapture) {
            var registration = new FilterRegistrationBean<>(traceCapture);
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
            return registration;
        }
    }
}
