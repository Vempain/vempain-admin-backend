package fi.poltsi.vempain.admin.metrics;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

/**
 * The custom meters of the admin backend. Every meter is named {@code vempain.<area>.<thing>} and carries only low-cardinality tags; the services and schedules
 * report events through the methods here instead of touching the registry themselves, so that the names and tags stay in one place. Gauges that read the
 * database are registered through {@link #cachedGauge} and refresh their value at most once per {@link #GAUGE_TTL}, never once per export.
 */
@Slf4j
@Component
public class AdminMetrics {

    public static final String API_TOKEN_REQUESTS = "vempain.api_token.requests";
    public static final String API_TOKEN_EXPIRY_DAYS = "vempain.api_token.expiry.days";
    public static final String INGEST_FILES = "vempain.ingest.files";
    public static final String INGEST_BYTES = "vempain.ingest.bytes";
    public static final String PUBLISH_SCHEDULE_PENDING = "vempain.publish_schedule.pending";
    public static final String PUBLISH_SCHEDULE_ITEMS = "vempain.publish_schedule.items";
    public static final String PUBLISH_SCHEDULE_LAG = "vempain.publish_schedule.lag";
    public static final String CONSISTENCY_FIXED = "vempain.consistency.fixed";
    public static final String INVENTORY_PAGES = "vempain.inventory.pages";
    public static final String INVENTORY_GALLERIES = "vempain.inventory.galleries";
    public static final String INVENTORY_SITE_FILES = "vempain.inventory.site_files";
    public static final String INVENTORY_SUBJECTS = "vempain.inventory.subjects";

    /**
     * How long a database backed gauge keeps its value before the next export triggers a new query.
     */
    public static final Duration GAUGE_TTL = Duration.ofMinutes(1);

    private final MeterRegistry registry;
    private final Clock clock;

    @Autowired
    public AdminMetrics(MeterRegistry registry) {
        this(registry, Clock.systemUTC());
    }

    /**
     * @param clock the clock the cached gauges measure their age with; tests pass a controllable one
     */
    public AdminMetrics(MeterRegistry registry, Clock clock) {
        this.registry = registry;
        this.clock = clock;
    }

    /**
     * One service-to-service request that carried an API token.
     *
     * @param result accepted, unknown, expired, wrong_network, forbidden_endpoint or missing_user
     */
    public void apiTokenRequest(String result) {
        Counter.builder(API_TOKEN_REQUESTS)
            .description("Requests authenticated with an API token, by outcome")
            .tag("result", result)
            .register(registry)
            .increment();
    }

    /**
     * One ingest call of the file backend.
     *
     * @param result created, updated or rejected
     * @param bytes  the size of the uploaded content; counted for created and updated files only
     */
    public void ingest(String result, long bytes) {
        Counter.builder(INGEST_FILES)
            .description("Site files ingested from the file backend, by outcome")
            .tag("result", result)
            .register(registry)
            .increment();
        if (bytes > 0
            && !"rejected".equals(result)) {
            Counter.builder(INGEST_BYTES)
                .description("Bytes of site file content ingested from the file backend")
                .baseUnit("bytes")
                .register(registry)
                .increment(bytes);
        }
    }

    /**
     * One item handled by the scheduled publish job.
     *
     * @param result published, missing, unknown_type or failed
     */
    public void publishScheduleItem(String result) {
        Counter.builder(PUBLISH_SCHEDULE_ITEMS)
            .description("Scheduled publishes handled by PublishItemSchedule, by outcome")
            .tag("result", result)
            .register(registry)
            .increment();
    }

    /**
     * How late a scheduled publish ran compared to its planned time.
     */
    public void publishScheduleLag(Duration lag) {
        Timer.builder(PUBLISH_SCHEDULE_LAG)
            .description("Time between the planned and the actual run of a scheduled publish")
            .register(registry)
            .record(lag.isNegative() ? Duration.ZERO : lag);
    }

    /**
     * Rows a consistency schedule had to repair; zero is the healthy value, so this only counts actual fixes.
     *
     * @param schedule acl, subject_file, duplicate_file or missing_thumb
     */
    public void consistencyFixed(String schedule, long count) {
        if (count <= 0) {
            return;
        }
        Counter.builder(CONSISTENCY_FIXED)
            .description("Rows repaired by the consistency schedules")
            .tag("schedule", schedule)
            .register(registry)
            .increment(count);
    }

    /**
     * Registers a gauge whose value comes from {@code reader} at most once per {@link #GAUGE_TTL}; between refreshes the last value is reported. A reader that
     * throws keeps the previous value (NaN before the first successful read, which exporters drop).
     */
    public void cachedGauge(String name, String description, Supplier<Number> reader) {
        Gauge.builder(name, new CachedValue(reader, clock, GAUGE_TTL), CachedValue::doubleValue)
            .description(description)
            .strongReference(true)
            .register(registry);
    }

    /**
     * A value re-read at most once per TTL.
     */
    static final class CachedValue {

        private final Supplier<Number> reader;
        private final Clock clock;
        private final Duration ttl;
        private volatile double value = Double.NaN;
        private volatile Instant refreshedAt;

        CachedValue(Supplier<Number> reader, Clock clock, Duration ttl) {
            this.reader = reader;
            this.clock = clock;
            this.ttl = ttl;
        }

        double doubleValue() {
            var now = clock.instant();
            if (refreshedAt == null
                || refreshedAt.plus(ttl)
                    .isBefore(now)) {
                refresh(now);
            }
            return value;
        }

        private synchronized void refresh(Instant now) {
            if (refreshedAt != null
                && !refreshedAt.plus(ttl)
                    .isBefore(now)) {
                return;
            }
            try {
                var read = reader.get();
                value = read == null ? Double.NaN : read.doubleValue();
            } catch (RuntimeException e) {
                log.warn("Could not refresh a cached gauge, keeping the previous value: {}", e.getMessage());
            }
            refreshedAt = now;
        }
    }
}
