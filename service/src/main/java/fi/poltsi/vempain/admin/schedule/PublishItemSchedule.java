package fi.poltsi.vempain.admin.schedule;

import java.time.Duration;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fi.poltsi.vempain.admin.api.ContentTypeEnum;
import fi.poltsi.vempain.admin.api.PublishStatusEnum;
import fi.poltsi.vempain.admin.metrics.AdminMetrics;
import fi.poltsi.vempain.admin.repository.PublishScheduleRepository;
import fi.poltsi.vempain.admin.service.PublishService;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class PublishItemSchedule {

    private static final long DELAY = 5 * 60 * 1000L;
    private static final String INITIAL_DELAY = "#{ 5 * 1000 + T(java.util.concurrent.ThreadLocalRandom).current().nextInt(" + DELAY + ") }";

    private final PublishScheduleRepository publishScheduleRepository;
    private final PublishService publishService;
    private final AdminMetrics adminMetrics;
    @org.springframework.beans.factory.annotation.Value("${vempain.scheduling.enabled:true}")
    private boolean schedulingEnabled = true;

    @Scheduled(fixedDelay = DELAY, initialDelayString = INITIAL_DELAY)
    @Transactional(propagation = Propagation.REQUIRED)
    protected void publishItems() {
        if (!schedulingEnabled) {
            return;
        }
        var scheduledPublishes = publishScheduleRepository.findAllByPublishStatusEqualsAndPublishTimeBefore(PublishStatusEnum.NOT_PUBLISHED, Instant.now());

        if (scheduledPublishes.isEmpty()) {
            log.info("No scheduled publishes found");
            return;
        }

        var counter = 0;

        for (var scheduledPublish : scheduledPublishes) {
            updatePublishStatus(scheduledPublish.getId(), PublishStatusEnum.PROCESSING);

            log.info("Publishing item: {}", scheduledPublish.getPublishType());
            if (scheduledPublish.getPublishTime() != null) {
                adminMetrics.publishScheduleLag(Duration.between(scheduledPublish.getPublishTime(), Instant.now()));
            }

            adminMetrics.publishScheduleItem(publishItem(scheduledPublish.getPublishType(), scheduledPublish.getPublishId()));
            updatePublishStatus(scheduledPublish.getId(), PublishStatusEnum.PUBLISHED);

            counter++;
        }

        log.info("{} items published", counter);
    }

    /**
     * Publishes one item and names the outcome for the metrics: published, missing (the item no longer exists), unknown_type; a publish that throws anything
     * else is counted as failed and the exception propagates as before.
     */
    private String publishItem(ContentTypeEnum publishType, Long publishId) {
        try {
            if (publishType == ContentTypeEnum.PAGE) {
                publishService.publishPageAsSystem(publishId);
            } else if (publishType == ContentTypeEnum.GALLERY) {
                publishService.publishGalleryAsSystem(publishId);
            } else {
                log.warn("Unknown publish type: {}", publishType);
                return "unknown_type";
            }
            return "published";
        } catch (VempainEntityNotFoundException e) {
            log.error("The {} to publish no longer exists: {}", publishType == ContentTypeEnum.PAGE ? "page" : "gallery", publishId);
            return "missing";
        } catch (RuntimeException e) {
            adminMetrics.publishScheduleItem("failed");
            throw e;
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    protected void updatePublishStatus(Long publishId, PublishStatusEnum publishStatusEnum) {
        publishScheduleRepository.updatePublishStatus(publishId, publishStatusEnum);
    }
}
