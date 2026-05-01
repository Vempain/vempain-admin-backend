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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceUTC {

	@Mock
	private ScanQueueScheduleRepository scanQueueScheduleRepository;
	@Mock
	private PublishScheduleRepository   publishScheduleRepository;

	@InjectMocks
	private ScheduleService scheduleService;

	// ---- scheduleFileProcessRequest ----

	@Test
	void scheduleFileProcessRequestOk() {
		FileProcessRequest request = FileProcessRequest.builder()
													   .sourceDirectory("/source")
													   .destinationDirectory("/dest")
													   .generateGallery(true)
													   .galleryShortname("my-gallery")
													   .galleryDescription("desc")
													   .generatePage(false)
													   .pageTitle("title")
													   .pagePath("/path")
													   .pageBody("body")
													   .pageFormId(1L)
													   .build();
		ScanQueueSchedule saved = ScanQueueSchedule.builder()
												   .id(1L)
												   .sourceDirectory("/source")
												   .destinationDirectory("/dest")
												   .build();
		when(scanQueueScheduleRepository.save(any(ScanQueueSchedule.class))).thenReturn(saved);

		scheduleService.scheduleFileProcessRequest(request, 42L);

		verify(scanQueueScheduleRepository).save(any(ScanQueueSchedule.class));
	}

	// ---- getUpcomingFileImportSchedules ----

	@Test
	void getUpcomingFileImportSchedulesOk() {
		ScanQueueSchedule schedule = ScanQueueSchedule.builder()
													  .id(1L)
													  .sourceDirectory("/src")
													  .destinationDirectory("/dst")
													  .createGallery(false)
													  .createPage(false)
													  .pageTitle("")
													  .pageFormId(1L)
													  .createdBy(1L)
													  .createdAt(Instant.now())
													  .updatedAt(Instant.now())
													  .build();
		when(scanQueueScheduleRepository.findAll()).thenReturn(List.of(schedule));

		List<FileImportScheduleResponse> result = scheduleService.getUpcomingFileImportSchedules();

		assertNotNull(result);
		assertEquals(1, result.size());
	}

	@Test
	void getUpcomingFileImportSchedulesEmptyOk() {
		when(scanQueueScheduleRepository.findAll()).thenReturn(List.of());

		List<FileImportScheduleResponse> result = scheduleService.getUpcomingFileImportSchedules();

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	// ---- getFileImportScheduleById ----

	@Test
	void getFileImportScheduleByIdOk() {
		ScanQueueSchedule schedule = ScanQueueSchedule.builder()
													  .id(5L)
													  .sourceDirectory("/src")
													  .destinationDirectory("/dst")
													  .createGallery(false)
													  .createPage(false)
													  .pageTitle("")
													  .pageFormId(1L)
													  .createdBy(1L)
													  .createdAt(Instant.now())
													  .updatedAt(Instant.now())
													  .build();
		when(scanQueueScheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));

		FileImportScheduleResponse result = scheduleService.getFileImportScheduleById(5L);

		assertNotNull(result);
	}

	@Test
	void getFileImportScheduleByIdNotFoundReturnsNullOk() {
		when(scanQueueScheduleRepository.findById(99L)).thenReturn(Optional.empty());

		FileImportScheduleResponse result = scheduleService.getFileImportScheduleById(99L);

		assertNull(result);
	}

	// ---- schedulePublish ----

	@Test
	void schedulePublishOk() {
		Instant publishTime = Instant.now().plus(1, ChronoUnit.HOURS);
		PublishSchedule saved = PublishSchedule.builder()
											   .id(1L)
											   .publishId(10L)
											   .publishTime(publishTime)
											   .publishType(ContentTypeEnum.PAGE)
											   .publishMessage("msg")
											   .publishStatus(PublishStatusEnum.NOT_PUBLISHED)
											   .createdAt(Instant.now())
											   .updatedAt(Instant.now())
											   .build();
		when(publishScheduleRepository.save(any(PublishSchedule.class))).thenReturn(saved);

		PublishScheduleResponse result = scheduleService.schedulePublish(publishTime, 10L, ContentTypeEnum.PAGE, "msg");

		assertNotNull(result);
		assertEquals(1L, result.getId());
		assertEquals(PublishStatusEnum.NOT_PUBLISHED, result.getPublishStatus());
	}

	// ---- getUpcomingPublishSchedules ----

	@Test
	void getUpcomingPublishSchedulesOk() {
		PublishSchedule ps1 = buildPublishSchedule(1L, PublishStatusEnum.NOT_PUBLISHED);
		PublishSchedule ps2 = buildPublishSchedule(2L, PublishStatusEnum.PROCESSING);
		when(publishScheduleRepository.findAllUpcomingSchedulesByPublishStatus(PublishStatusEnum.NOT_PUBLISHED))
				.thenReturn(List.of(ps1));
		when(publishScheduleRepository.findAllUpcomingSchedulesByPublishStatus(PublishStatusEnum.PROCESSING))
				.thenReturn(List.of(ps2));

		List<PublishScheduleResponse> result = scheduleService.getUpcomingPublishSchedules();

		assertNotNull(result);
		assertEquals(2, result.size());
	}

	@Test
	void getUpcomingPublishSchedulesEmptyOk() {
		when(publishScheduleRepository.findAllUpcomingSchedulesByPublishStatus(PublishStatusEnum.NOT_PUBLISHED))
				.thenReturn(List.of());
		when(publishScheduleRepository.findAllUpcomingSchedulesByPublishStatus(PublishStatusEnum.PROCESSING))
				.thenReturn(List.of());

		List<PublishScheduleResponse> result = scheduleService.getUpcomingPublishSchedules();

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	// ---- getPublishScheduleById ----

	@Test
	void getPublishScheduleByIdOk() {
		PublishSchedule ps = buildPublishSchedule(3L, PublishStatusEnum.NOT_PUBLISHED);
		when(publishScheduleRepository.findById(3L)).thenReturn(Optional.of(ps));

		PublishScheduleResponse result = scheduleService.getPublishScheduleById(3L);

		assertNotNull(result);
		assertEquals(3L, result.getId());
	}

	@Test
	void getPublishScheduleByIdNotFoundReturnsNullOk() {
		when(publishScheduleRepository.findById(99L)).thenReturn(Optional.empty());

		PublishScheduleResponse result = scheduleService.getPublishScheduleById(99L);

		assertNull(result);
	}

	// ---- triggerPublishSchedule ----

	@Test
	void triggerPublishScheduleFutureTimeOk() {
		Instant future = Instant.now().plus(1, ChronoUnit.HOURS);
		PublishSchedule existing = buildPublishSchedule(4L, PublishStatusEnum.NOT_PUBLISHED);
		existing.setPublishTime(future);

		PublishScheduleRequest request = PublishScheduleRequest.builder()
															   .publishId(4L)
															   .publishTime(future)
															   .build();
		when(publishScheduleRepository.findById(4L)).thenReturn(Optional.of(existing));
		when(publishScheduleRepository.save(any(PublishSchedule.class))).thenReturn(existing);

		PublishScheduleResponse result = scheduleService.triggerPublishSchedule(request);

		assertNotNull(result);
		assertEquals(PublishStatusEnum.NOT_PUBLISHED, result.getPublishStatus());
	}

	@Test
	void triggerPublishSchedulePastTimeProcessingOk() {
		Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
		PublishSchedule existing = buildPublishSchedule(5L, PublishStatusEnum.NOT_PUBLISHED);
		existing.setPublishTime(past);

		PublishScheduleRequest request = PublishScheduleRequest.builder()
															   .publishId(5L)
															   .publishTime(past)
															   .build();
		when(publishScheduleRepository.findById(5L)).thenReturn(Optional.of(existing));
		when(publishScheduleRepository.save(any(PublishSchedule.class))).thenReturn(existing);

		PublishScheduleResponse result = scheduleService.triggerPublishSchedule(request);

		assertNotNull(result);
		assertEquals(PublishStatusEnum.PROCESSING, result.getPublishStatus());
	}

	@Test
	void triggerPublishScheduleNotFoundReturnsNullOk() {
		PublishScheduleRequest request = PublishScheduleRequest.builder()
															   .publishId(99L)
															   .publishTime(Instant.now())
															   .build();
		when(publishScheduleRepository.findById(99L)).thenReturn(Optional.empty());

		PublishScheduleResponse result = scheduleService.triggerPublishSchedule(request);

		assertNull(result);
	}

	private PublishSchedule buildPublishSchedule(long id, PublishStatusEnum status) {
		return PublishSchedule.builder()
							  .id(id)
							  .publishId(id * 10)
							  .publishTime(Instant.now().plus(1, ChronoUnit.HOURS))
							  .publishType(ContentTypeEnum.PAGE)
							  .publishMessage("test")
							  .publishStatus(status)
							  .createdAt(Instant.now())
							  .updatedAt(Instant.now())
							  .build();
	}
}
