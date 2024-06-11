package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.api.ContentTypeEnum;
import fi.poltsi.vempain.admin.api.PublishStatusEnum;
import fi.poltsi.vempain.admin.api.request.FileProcessRequest;
import fi.poltsi.vempain.admin.api.response.PublishScheduleResponse;
import fi.poltsi.vempain.admin.entity.PublishSchedule;
import fi.poltsi.vempain.admin.entity.file.ScanQueueSchedule;
import fi.poltsi.vempain.admin.repository.PublishScheduleRepository;
import fi.poltsi.vempain.admin.repository.file.ScanQueueScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ScheduleService {
	private final ScanQueueScheduleRepository scanQueueScheduleRepository;
	private final PublishScheduleRepository   publishScheduleRepository;

	public void scheduleFileProcessRequest(FileProcessRequest fileProcessRequest, long userId) {
		var creationInstant = Instant.now();
		var scanQueueSchedule = ScanQueueSchedule.builder()
												 .sourceDirectory(fileProcessRequest.getSourceDirectory())
												 .destinationDirectory(fileProcessRequest.getDestinationDirectory())
												 .createGallery(fileProcessRequest.isGenerateGallery())
												 .galleryShortname(fileProcessRequest.getGalleryShortname())
												 .galleryDescription(fileProcessRequest.getGalleryDescription())
												 .createPage(fileProcessRequest.isGeneratePage())
												 .pageTitle(fileProcessRequest.getPageTitle())
												 .pagePath(fileProcessRequest.getPagePath())
												 .pageBody(fileProcessRequest.getPageBody())
												 .pageFormId(fileProcessRequest.getPageFormId())
												 .createdBy(userId)
												 .createdAt(creationInstant)
												 .updatedAt(creationInstant)
												 .build();

		scanQueueScheduleRepository.save(scanQueueSchedule);
	}

	public void schedulePublish(Instant publishTime, long itemId, ContentTypeEnum contentTypeEnum) {
		var creationInstant = Instant.now();
		var scanQueueSchedule = PublishSchedule.builder()
											   .publishTime(publishTime)
											   .publishType(contentTypeEnum)
											   .createdAt(creationInstant)
											   .updatedAt(creationInstant)
											   .build();

		publishScheduleRepository.save(scanQueueSchedule);
	}

	public List<PublishScheduleResponse> getUpcomingPublishSchedules() {
		var publishScheduleResponses = new ArrayList<PublishScheduleResponse>();

		var notPublishedSchedules = publishScheduleRepository.findAllUpcomingSchedulesByPublishStatus(PublishStatusEnum.NOT_PUBLISHED);

		for (var notPublishedSchedule : notPublishedSchedules) {
			publishScheduleResponses.add(notPublishedSchedule.toResponse());
		}

		var processingSchedules = publishScheduleRepository.findAllUpcomingSchedulesByPublishStatus(PublishStatusEnum.PROCESSING);

		for (var processingSchedule : processingSchedules) {
			publishScheduleResponses.add(processingSchedule.toResponse());
		}

		return publishScheduleResponses;
	}
}
