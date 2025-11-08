package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.api.ContentTypeEnum;
import fi.poltsi.vempain.admin.api.PublishStatusEnum;
import fi.poltsi.vempain.admin.api.request.FileProcessRequest;
import fi.poltsi.vempain.admin.api.request.PublishScheduleRequest;
import fi.poltsi.vempain.admin.api.response.PublishScheduleResponse;
import fi.poltsi.vempain.admin.api.response.file.FileImportScheduleResponse;
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

	public List<FileImportScheduleResponse> getUpcomingFileImportSchedules() {
		var fileImportSchedules = scanQueueScheduleRepository.findAll();
		var fileImportScheduleResponses = new ArrayList<FileImportScheduleResponse>();
		for (var fileImportSchedule : fileImportSchedules) {
			fileImportScheduleResponses.add(fileImportSchedule.toResponse());
		}

		return fileImportScheduleResponses;
	}


	public FileImportScheduleResponse getFileImportScheduleById(long id) {
		var optionalScanQueueSchedule = scanQueueScheduleRepository.findById(id);

		return optionalScanQueueSchedule.map(ScanQueueSchedule::toResponse)
										.orElse(null);
	}

	public PublishScheduleResponse schedulePublish(Instant publishTime, long itemId, ContentTypeEnum contentTypeEnum, String publishMessage) {
		var creationInstant = Instant.now();
		var publishStatus = PublishStatusEnum.NOT_PUBLISHED;

		var scanQueueSchedule = PublishSchedule.builder()
											   .publishId(itemId)
											   .publishTime(publishTime)
											   .publishType(contentTypeEnum)
											   .publishMessage(publishMessage)
											   .publishStatus(publishStatus)
											   .createdAt(creationInstant)
											   .updatedAt(creationInstant)
											   .build();

		var newSchedule = publishScheduleRepository.save(scanQueueSchedule);
		return newSchedule.toResponse();
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

	public PublishScheduleResponse getPublishScheduleById(long id) {
		var publishSchedule = publishScheduleRepository.findById(id);

		return publishSchedule.map(PublishSchedule::toResponse)
							  .orElse(null);
	}

	public PublishScheduleResponse triggerPublishSchedule(PublishScheduleRequest publishScheduleRequest) {
		var publishSchedule = publishScheduleRepository.findById(publishScheduleRequest.getPublishId());

		if (publishSchedule.isPresent()) {
			var schedule = publishSchedule.get();
			schedule.setPublishTime(publishScheduleRequest.getPublishTime());

			if (!publishScheduleRequest.getPublishTime()
									   .isAfter(Instant.now())) {
				schedule.setPublishStatus(PublishStatusEnum.PROCESSING);
			} else {
				schedule.setPublishStatus(PublishStatusEnum.NOT_PUBLISHED);
			}

			var newSchedule = publishScheduleRepository.save(schedule);
			return newSchedule.toResponse();
		}

		return null;
	}
}
