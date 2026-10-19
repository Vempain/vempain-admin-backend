package fi.poltsi.vempain.admin.metrics;

import java.time.Duration;
import java.time.Instant;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import fi.poltsi.vempain.admin.api.PublishStatusEnum;
import fi.poltsi.vempain.admin.entity.ApiToken;
import fi.poltsi.vempain.admin.repository.ApiTokenRepository;
import fi.poltsi.vempain.admin.repository.PageRepository;
import fi.poltsi.vempain.admin.repository.PublishScheduleRepository;
import fi.poltsi.vempain.admin.repository.file.GalleryRepository;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.admin.repository.file.SubjectRepository;
import lombok.RequiredArgsConstructor;

/**
 * The database backed gauges of the admin backend: the content inventory, the pending scheduled publishes and the days left on the soonest expiring API token.
 * All of them go through {@link AdminMetrics#cachedGauge}, so the queries run at most once a minute regardless of the export interval.
 */
@Component
@RequiredArgsConstructor
public class AdminInventoryMetrics {

    private final AdminMetrics adminMetrics;
    private final PageRepository pageRepository;
    private final GalleryRepository galleryRepository;
    private final SiteFileRepository siteFileRepository;
    private final SubjectRepository subjectRepository;
    private final PublishScheduleRepository publishScheduleRepository;
    private final ApiTokenRepository apiTokenRepository;

    @PostConstruct
    void register() {
        adminMetrics.cachedGauge(AdminMetrics.INVENTORY_PAGES, "Pages in the admin database", pageRepository::count);
        adminMetrics.cachedGauge(AdminMetrics.INVENTORY_GALLERIES, "Galleries in the admin database", galleryRepository::count);
        adminMetrics.cachedGauge(AdminMetrics.INVENTORY_SITE_FILES, "Site files in the admin database", siteFileRepository::count);
        adminMetrics.cachedGauge(AdminMetrics.INVENTORY_SUBJECTS, "Subjects in the admin database", subjectRepository::count);
        adminMetrics.cachedGauge(AdminMetrics.PUBLISH_SCHEDULE_PENDING, "Scheduled publishes not yet published",
                () -> publishScheduleRepository.countByPublishStatus(PublishStatusEnum.NOT_PUBLISHED));
        adminMetrics.cachedGauge(AdminMetrics.API_TOKEN_EXPIRY_DAYS,
                "Days until the soonest unexpired API token expires; absent while no unexpired token exists", this::soonestTokenExpiryDays);
    }

    /**
     * @return days (fractional) until the earliest expiry among the unexpired tokens, NaN when there is none
     */
    double soonestTokenExpiryDays() {
        var now = Instant.now();
        return apiTokenRepository.findAll()
            .stream()
            .map(ApiToken::getExpiresAt)
            .filter(expiresAt -> expiresAt != null
                                 && expiresAt.isAfter(now))
            .mapToDouble(expiresAt -> Duration.between(now, expiresAt)
                .toSeconds() / 86_400.0)
            .min()
            .orElse(Double.NaN);
    }
}
