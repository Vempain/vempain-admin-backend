package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.api.request.PublishScheduleRequest;
import fi.poltsi.vempain.admin.api.request.TriggerSystemScheduleRequest;
import fi.poltsi.vempain.admin.api.response.PublishScheduleResponse;
import fi.poltsi.vempain.admin.api.response.ScheduleTriggerResponse;
import fi.poltsi.vempain.admin.api.response.file.FileImportScheduleResponse;
import fi.poltsi.vempain.admin.rest.ScheduleAPI;
import fi.poltsi.vempain.admin.schedule.DirectoryImportSchedule;
import fi.poltsi.vempain.admin.service.ScheduleService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@AllArgsConstructor
@RestController
public class ScheduleController implements ScheduleAPI {
	private ScheduledAnnotationBeanPostProcessor postProcessor;
	private TaskScheduler                        taskScheduler;
	private ScheduleService                      scheduleService;
	private DirectoryImportSchedule              directoryImportSchedule;

	@Override
	public ResponseEntity<List<ScheduleTriggerResponse>> getSystemSchedules() {
		Set<ScheduledTask>                 scheduledTaskSet         = postProcessor.getScheduledTasks();
		ArrayList<ScheduleTriggerResponse> scheduleTriggerResponses = new ArrayList<>();
		var                                idCounter                = 1L;

		for (ScheduledTask scheduledTask : scheduledTaskSet) {
			scheduleTriggerResponses.add(ScheduleTriggerResponse.builder()
																.id(idCounter)
																.scheduleName(scheduledTask.getTask().toString())
																.status("ACTIVE")
																.build());
			idCounter++;
		}

		return ResponseEntity.ok(scheduleTriggerResponses);
	}

	@Override
	public ResponseEntity<ScheduleTriggerResponse> getSystemScheduleByName(String systemScheduleName) {
		Set<ScheduledTask> scheduledTaskSet = postProcessor.getScheduledTasks();

		for (ScheduledTask scheduledTask : scheduledTaskSet) {
			if (scheduledTask.getTask()
							 .toString()
							 .equals(systemScheduleName)) {
				return ResponseEntity.ok(ScheduleTriggerResponse.builder()
																.id(1L)
																.scheduleName(scheduledTask.getTask().toString())
																.status("ACTIVE")
																.build());
			}
		}

		return ResponseEntity.notFound()
							 .build();
	}

	@Override
	public ResponseEntity<ScheduleTriggerResponse> triggerSystemSchedule(TriggerSystemScheduleRequest schedule) {
		log.debug("Call to trigger system schedule: {}", schedule);
		Set<ScheduledTask> scheduledTaskSet = postProcessor.getScheduledTasks();

		for (ScheduledTask scheduledTask : scheduledTaskSet) {
			if (scheduledTask.getTask()
							 .toString()
							 .equals(schedule.getScheduleName())) {
				taskScheduler.schedule(scheduledTask.getTask().getRunnable(), Instant.now().plusSeconds(schedule.getDelay()));

				return ResponseEntity.ok(ScheduleTriggerResponse.builder()
																.id(1L)
																.scheduleName(scheduledTask.getTask().toString())
																.status("ACTIVE")
																.build());
			}
		}

		log.error("Failed to trigger system schedule with name: {}", schedule.getScheduleName());

		return ResponseEntity.notFound().build();
	}

	@Override
	public ResponseEntity<List<PublishScheduleResponse>> listPublishingSchedules() {
		log.debug("Call to get all the publishing schedules");
		var upcomingPublishSchedules = scheduleService.getUpcomingPublishSchedules();
		return ResponseEntity.ok(upcomingPublishSchedules);
	}

	@Override
	public ResponseEntity<PublishScheduleResponse> getPublishingScheduleById(long id) {
		log.debug("Call to get publishing schedule ID: {}", id);
		var publishScheduleResponse = scheduleService.getPublishScheduleById(id);

		if (publishScheduleResponse == null) {
			return ResponseEntity.notFound()
								 .build();
		}

		return ResponseEntity.ok(publishScheduleResponse);
	}

	@Override
	public ResponseEntity<PublishScheduleResponse> triggerPublishSchedule(PublishScheduleRequest publishScheduleRequest) {
		log.debug("Call to trigger publishing schedule: {}", publishScheduleRequest);

		var publishScheduleResponse = scheduleService.triggerPublishSchedule(publishScheduleRequest);

		if (publishScheduleResponse == null) {
			return ResponseEntity.notFound()
								 .build();
		}

		return ResponseEntity.ok(publishScheduleResponse);
	}

	@Override
	public ResponseEntity<List<FileImportScheduleResponse>> listFileImportSchedules() {
		log.debug("Call to get all the file import schedules");
		var upcomingFileImportSchedules = scheduleService.getUpcomingFileImportSchedules();
		return ResponseEntity.ok(upcomingFileImportSchedules);
	}

	@Override
	public ResponseEntity<FileImportScheduleResponse> getFileImportScheduleById(long id) {
		log.debug("Call to get file import schedule ID: {}", id);
		var fileImportSchedule = scheduleService.getFileImportScheduleById(id);

		if (fileImportSchedule == null) {
			return ResponseEntity.notFound()
								 .build();
		}

		return ResponseEntity.ok(fileImportSchedule);
	}
}
