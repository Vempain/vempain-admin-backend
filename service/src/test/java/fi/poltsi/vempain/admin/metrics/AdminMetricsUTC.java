package fi.poltsi.vempain.admin.metrics;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The custom meters of the admin backend: names, tags and the once-a-minute cache of the database backed gauges.
 */
class AdminMetricsUTC {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @Test
    void countersCarryTheOutcomeTags() {
        var metrics = new AdminMetrics(registry);

        metrics.apiTokenRequest("accepted");
        metrics.apiTokenRequest("accepted");
        metrics.apiTokenRequest("expired");
        metrics.ingest("created", 1_000);
        metrics.ingest("updated", 500);
        metrics.ingest("rejected", 700);
        metrics.publishScheduleItem("published");
        metrics.publishScheduleLag(Duration.ofSeconds(90));
        metrics.publishScheduleLag(Duration.ofSeconds(-5));
        metrics.consistencyFixed("acl", 3);
        metrics.consistencyFixed("subject_file", 0);

        assertThat(registry.get(AdminMetrics.API_TOKEN_REQUESTS)
            .tag("result", "accepted")
            .counter()
            .count()).isEqualTo(2);
        assertThat(registry.get(AdminMetrics.API_TOKEN_REQUESTS)
            .tag("result", "expired")
            .counter()
            .count()).isEqualTo(1);
        assertThat(registry.get(AdminMetrics.INGEST_FILES)
            .tag("result", "rejected")
            .counter()
            .count()).isEqualTo(1);
        assertThat(registry.get(AdminMetrics.INGEST_BYTES)
            .counter()
            .count()).as("rejected uploads are not counted as ingested bytes")
            .isEqualTo(1_500);
        assertThat(registry.get(AdminMetrics.PUBLISH_SCHEDULE_ITEMS)
            .tag("result", "published")
            .counter()
            .count()).isEqualTo(1);
        var lag = registry.get(AdminMetrics.PUBLISH_SCHEDULE_LAG)
            .timer();
        assertThat(lag.count()).isEqualTo(2);
        assertThat(lag.totalTime(java.util.concurrent.TimeUnit.SECONDS)).as("a negative lag is recorded as zero")
            .isEqualTo(90);
        assertThat(registry.get(AdminMetrics.CONSISTENCY_FIXED)
            .tag("schedule", "acl")
            .counter()
            .count()).isEqualTo(3);
        assertThat(registry.find(AdminMetrics.CONSISTENCY_FIXED)
            .tag("schedule", "subject_file")
            .counter()).as("nothing fixed registers no meter")
            .isNull();
    }

    @Test
    void cachedGaugeReadsAtMostOncePerTtlAndKeepsTheValueWhenTheReaderFails() {
        var now = new AtomicReference<>(Instant.parse("2026-10-19T10:00:00Z"));
        var clock = new Clock() {

            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now.get();
            }
        };
        var metrics = new AdminMetrics(registry, clock);
        var reads = new AtomicInteger();
        var fail = new AtomicReference<>(false);
        metrics.cachedGauge("vempain.test.cached", "test", () -> {
            if (fail.get()) {
                throw new IllegalStateException("database gone");
            }
            return reads.incrementAndGet() * 10;
        });
        var gauge = registry.get("vempain.test.cached")
            .gauge();

        assertThat(gauge.value()).isEqualTo(10);
        assertThat(gauge.value()).as("a second export within the TTL reuses the value")
            .isEqualTo(10);
        assertThat(reads.get()).isEqualTo(1);

        now.set(now.get()
            .plus(AdminMetrics.GAUGE_TTL)
            .plusSeconds(1));
        assertThat(gauge.value()).isEqualTo(20);

        fail.set(true);
        now.set(now.get()
            .plus(AdminMetrics.GAUGE_TTL)
            .plusSeconds(1));
        assertThat(gauge.value()).as("a failing reader keeps the previous value")
            .isEqualTo(20);
        assertThat(reads.get()).isEqualTo(2);
    }

    @Test
    void cachedGaugeIsNanUntilTheFirstSuccessfulRead() {
        var metrics = new AdminMetrics(registry);
        metrics.cachedGauge("vempain.test.failing", "test", () -> {
            throw new IllegalStateException("database gone");
        });

        assertThat(registry.get("vempain.test.failing")
            .gauge()
            .value()).isNaN();
    }
}
