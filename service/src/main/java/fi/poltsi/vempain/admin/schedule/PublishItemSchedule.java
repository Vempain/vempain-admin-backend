package fi.poltsi.vempain.admin.schedule;

import fi.poltsi.vempain.admin.api.ContentTypeEnum;
import fi.poltsi.vempain.admin.api.PublishStatusEnum;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.admin.repository.PublishScheduleRepository;
import fi.poltsi.vempain.admin.service.PublishService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
		var scheduledPublishes = publishScheduleRepository.findAllUpcomingSchedulesByPublishStatus(PublishStatusEnum.NOT_PUBLISHED);

		if (scheduledPublishes.isEmpty()) {
			log.info("No scheduled publishes found");
			return;
		}

		for (var scheduledPublish : scheduledPublishes) {
			log.info("Publishing item: {}", scheduledPublish.getPublishType());

			if (scheduledPublish.getPublishType() == ContentTypeEnum.PAGE) {
				publishPage(scheduledPublish.getPublishId());
			} else if (scheduledPublish.getPublishType() == ContentTypeEnum.GALLERY) {
				publishGallery(scheduledPublish.getPublishId());
			} else {
				log.warn("Unknown publish type: {}", scheduledPublish.getPublishType());
			}
		}

		log.info("Publishing items");
	}

	@Transactional(propagation = Propagation.REQUIRED)
	protected void publishPage(Long pageId) {
		try {
			publishService.publishPage(pageId);
			updatePublishStatus(pageId, ContentTypeEnum.PAGE);
		} catch (VempainEntityNotFoundException e) {
			log.error("The page to publish no longer exists: {}", pageId);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	protected void publishGallery(Long galleryId) {
		try {
			publishService.publishGallery(galleryId);
			updatePublishStatus(galleryId, ContentTypeEnum.GALLERY);
		} catch (VempainEntityNotFoundException e) {
			log.error("The gallery to publish no longer exists: {}", galleryId);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	protected void updatePublishStatus(Long publishId, ContentTypeEnum contentTypeEnum) {
		publishScheduleRepository.updatePublishStatus(publishId, contentTypeEnum);
	}
}
