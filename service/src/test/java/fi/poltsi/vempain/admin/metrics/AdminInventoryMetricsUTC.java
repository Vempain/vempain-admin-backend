package fi.poltsi.vempain.admin.metrics;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fi.poltsi.vempain.admin.api.PublishStatusEnum;
import fi.poltsi.vempain.admin.entity.ApiToken;
import fi.poltsi.vempain.admin.repository.ApiTokenRepository;
import fi.poltsi.vempain.admin.repository.PageRepository;
import fi.poltsi.vempain.admin.repository.PublishScheduleRepository;
import fi.poltsi.vempain.admin.repository.file.GalleryRepository;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.admin.repository.file.SubjectRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The inventory, pending publish and token expiry gauges read the repositories through the cached gauge helper.
 */
@ExtendWith(MockitoExtension.class)
class AdminInventoryMetricsUTC {

    @Mock
    private PageRepository pageRepository;
    @Mock
    private GalleryRepository galleryRepository;
    @Mock
    private SiteFileRepository siteFileRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private PublishScheduleRepository publishScheduleRepository;
    @Mock
    private ApiTokenRepository apiTokenRepository;

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    private AdminInventoryMetrics metrics() {
        var metrics = new AdminInventoryMetrics(new AdminMetrics(registry), pageRepository, galleryRepository, siteFileRepository, subjectRepository,
                publishScheduleRepository, apiTokenRepository);
        metrics.register();
        return metrics;
    }

    @Test
    void gaugesReportTheCountsAndCacheThem() {
        when(pageRepository.count()).thenReturn(12L);
        when(galleryRepository.count()).thenReturn(3L);
        when(siteFileRepository.count()).thenReturn(400L);
        when(subjectRepository.count()).thenReturn(25L);
        when(publishScheduleRepository.countByPublishStatus(PublishStatusEnum.NOT_PUBLISHED)).thenReturn(2L);
        when(apiTokenRepository.findAll()).thenReturn(List.of());
        metrics();

        assertThat(registry.get(AdminMetrics.INVENTORY_PAGES)
            .gauge()
            .value()).isEqualTo(12);
        assertThat(registry.get(AdminMetrics.INVENTORY_GALLERIES)
            .gauge()
            .value()).isEqualTo(3);
        assertThat(registry.get(AdminMetrics.INVENTORY_SITE_FILES)
            .gauge()
            .value()).isEqualTo(400);
        assertThat(registry.get(AdminMetrics.INVENTORY_SUBJECTS)
            .gauge()
            .value()).isEqualTo(25);
        assertThat(registry.get(AdminMetrics.PUBLISH_SCHEDULE_PENDING)
            .gauge()
            .value()).isEqualTo(2);
        assertThat(registry.get(AdminMetrics.API_TOKEN_EXPIRY_DAYS)
            .gauge()
            .value()).as("no unexpired token: the gauge has no value")
            .isNaN();

        registry.get(AdminMetrics.INVENTORY_PAGES)
            .gauge()
            .value();
        verify(pageRepository, times(1)).count();
    }

    @Test
    void tokenExpiryIsTheSoonestOfTheUnexpiredTokens() {
        var now = Instant.now();
        when(apiTokenRepository.findAll()).thenReturn(List.of(ApiToken.builder()
            .expiresAt(now.minusSeconds(3_600))
            .build(),
                ApiToken.builder()
                    .expiresAt(now.plusSeconds(10 * 86_400))
                    .build(),
                ApiToken.builder()
                    .expiresAt(now.plusSeconds(3 * 86_400))
                    .build()));

        assertThat(metrics().soonestTokenExpiryDays()).isCloseTo(3.0, within(0.01));
    }
}
