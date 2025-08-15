package fi.poltsi.vempain.admin.schedule;

import fi.poltsi.vempain.admin.api.ContentTypeEnum;
import fi.poltsi.vempain.admin.api.PublishStatusEnum;
import fi.poltsi.vempain.admin.repository.PublishScheduleRepository;
import fi.poltsi.vempain.admin.service.PublishService;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@AllArgsConstructor
@Service
public class PublishItemSchedule {
	private static final long   DELAY         = 5 * 60 * 1000L;
	private static final String INITIAL_DELAY =
			"#{ 5 * 1000 + T(java.util.concurrent.ThreadLocalRandom).current().nextInt(" + DELAY + ") }";

	private final PublishScheduleRepository publishScheduleRepository;
	private final PublishService            publishService;

	@Scheduled(fixedDelay = DELAY, initialDelayString = INITIAL_DELAY)
	@Transactional(propagation = Propagation.REQUIRED)
	protected void publishItems() {
		var scheduledPublishes = publishScheduleRepository.findAllByPublishStatusEqualsAndPublishTimeBefore(PublishStatusEnum.NOT_PUBLISHED, Instant.now());

		if (scheduledPublishes.isEmpty()) {
			log.info("No scheduled publishes found");
			return;
		}

		var counter = 0;

		for (var scheduledPublish : scheduledPublishes) {
			updatePublishStatus(scheduledPublish.getId(), PublishStatusEnum.PROCESSING);

			log.info("Publishing item: {}", scheduledPublish.getPublishType());

			if (scheduledPublish.getPublishType() == ContentTypeEnum.PAGE) {
				try {
					publishService.publishPage(scheduledPublish.getPublishId());
				} catch (VempainEntityNotFoundException e) {
					log.error("The page to publish no longer exists: {}", scheduledPublish.getPublishId());
				}
			} else if (scheduledPublish.getPublishType() == ContentTypeEnum.GALLERY) {
				try {
					publishService.publishGallery(scheduledPublish.getPublishId());
				} catch (VempainEntityNotFoundException e) {
					log.error("The gallery to publish no longer exists: {}", scheduledPublish.getPublishId());
				}
			} else {
				log.warn("Unknown publish type: {}", scheduledPublish.getPublishType());
			}

			updatePublishStatus(scheduledPublish.getId(), PublishStatusEnum.PUBLISHED);

			counter++;
		}

		log.info("{} items published", counter);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	protected void updatePublishStatus(Long publishId, PublishStatusEnum publishStatusEnum) {
		publishScheduleRepository.updatePublishStatus(publishId, publishStatusEnum);
	}
}
