package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.api.ContentTypeEnum;
import fi.poltsi.vempain.admin.api.request.FileProcessRequest;
import fi.poltsi.vempain.admin.api.request.PublishScheduleRequest;
import fi.poltsi.vempain.admin.api.response.PublishScheduleResponse;
import fi.poltsi.vempain.admin.api.response.file.FileImportScheduleResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Slf4j
class ScheduleServiceITC extends AbstractITCTest {

	@Autowired
	private ScheduleService scheduleService;

	// ---- scheduleFileProcessRequest ----

	@Test
	void scheduleFileProcessRequestOk() {
		var request = FileProcessRequest.builder()
										.sourceDirectory("/source/dir")
										.destinationDirectory("/dest/dir")
										.generateGallery(false)
										.generatePage(false)
										.build();
		long userId = testITCTools.generateUser();

		scheduleService.scheduleFileProcessRequest(request, userId);

		List<FileImportScheduleResponse> schedules = scheduleService.getUpcomingFileImportSchedules();
		assertNotNull(schedules);
		boolean found = schedules.stream().anyMatch(s -> "/source/dir".equals(s.getSourceDirectory()));
		assertTrue(found);
	}

	// ---- getUpcomingFileImportSchedules ----

	@Test
	void getUpcomingFileImportSchedulesOk() {
		List<FileImportScheduleResponse> schedules = scheduleService.getUpcomingFileImportSchedules();
		assertNotNull(schedules);
	}

	// ---- getFileImportScheduleById ----

	@Test
	void getFileImportScheduleByIdOk() {
		var request = FileProcessRequest.builder()
										.sourceDirectory("/src/path")
										.destinationDirectory("/dst/path")
										.generateGallery(false)
										.generatePage(false)
										.build();
		long userId = testITCTools.generateUser();
		scheduleService.scheduleFileProcessRequest(request, userId);

		List<FileImportScheduleResponse> schedules = scheduleService.getUpcomingFileImportSchedules();
		assertNotNull(schedules);
		long id = schedules.stream()
						   .filter(s -> "/src/path".equals(s.getSourceDirectory()))
						   .map(FileImportScheduleResponse::getId)
						   .findFirst()
						   .orElseThrow();

		FileImportScheduleResponse result = scheduleService.getFileImportScheduleById(id);
		assertNotNull(result);
		assertEquals(id, result.getId());
	}

	@Test
	void getFileImportScheduleByIdNotFoundOk() {
		FileImportScheduleResponse result = scheduleService.getFileImportScheduleById(999999L);
		assertNull(result);
	}

	// ---- schedulePublish ----

	@Test
	void schedulePublishOk() {
		var publishTime = Instant.now().plus(1, ChronoUnit.HOURS);
		PublishScheduleResponse response = scheduleService.schedulePublish(
				publishTime, 1L, ContentTypeEnum.PAGE, "Test publish");

		assertNotNull(response);
		assertEquals(ContentTypeEnum.PAGE, response.getPublishType());
	}

	// ---- getUpcomingPublishSchedules ----

	@Test
	void getUpcomingPublishSchedulesOk() {
		// Create a schedule first
		var publishTime = Instant.now().plus(2, ChronoUnit.HOURS);
		scheduleService.schedulePublish(publishTime, 2L, ContentTypeEnum.PAGE, "Test upcoming");

		List<PublishScheduleResponse> result = scheduleService.getUpcomingPublishSchedules();
		assertNotNull(result);
	}

	// ---- getPublishScheduleById ----

	@Test
	void getPublishScheduleByIdOk() {
		var publishTime = Instant.now().plus(1, ChronoUnit.HOURS);
		PublishScheduleResponse created = scheduleService.schedulePublish(
				publishTime, 3L, ContentTypeEnum.PAGE, "Lookup test");

		PublishScheduleResponse found = scheduleService.getPublishScheduleById(created.getId());
		assertNotNull(found);
		assertEquals(created.getId(), found.getId());
	}

	@Test
	void getPublishScheduleByIdNotFoundOk() {
		PublishScheduleResponse result = scheduleService.getPublishScheduleById(999999L);
		assertNull(result);
	}

	// ---- triggerPublishSchedule - past time ----

	@Test
	void triggerPublishSchedulePastTimeOk() {
		var futureTime = Instant.now().plus(1, ChronoUnit.HOURS);
		PublishScheduleResponse created = scheduleService.schedulePublish(
				futureTime, 4L, ContentTypeEnum.PAGE, "Trigger test past");

		// Trigger with a past time → should set PROCESSING
		var pastTime = Instant.now().minus(1, ChronoUnit.HOURS);
		var request = PublishScheduleRequest.builder()
											.publishId(created.getId())
											.publishTime(pastTime)
											.build();

		PublishScheduleResponse result = scheduleService.triggerPublishSchedule(request);
		assertNotNull(result);
		assertEquals(fi.poltsi.vempain.admin.api.PublishStatusEnum.PROCESSING, result.getPublishStatus());
	}

	// ---- triggerPublishSchedule - future time ----

	@Test
	void triggerPublishScheduleFutureTimeOk() {
		var futureTime = Instant.now().plus(1, ChronoUnit.HOURS);
		PublishScheduleResponse created = scheduleService.schedulePublish(
				futureTime, 5L, ContentTypeEnum.PAGE, "Trigger test future");

		var laterTime = Instant.now().plus(2, ChronoUnit.HOURS);
		var request = PublishScheduleRequest.builder()
											.publishId(created.getId())
											.publishTime(laterTime)
											.build();

		PublishScheduleResponse result = scheduleService.triggerPublishSchedule(request);
		assertNotNull(result);
		assertEquals(fi.poltsi.vempain.admin.api.PublishStatusEnum.NOT_PUBLISHED, result.getPublishStatus());
	}

	// ---- triggerPublishSchedule - not found ----

	@Test
	void triggerPublishScheduleNotFoundOk() {
		var request = PublishScheduleRequest.builder()
											.publishId(999999L)
											.publishTime(Instant.now())
											.build();

		PublishScheduleResponse result = scheduleService.triggerPublishSchedule(request);
		assertNull(result);
	}
}
