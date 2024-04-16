package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.api.request.ScheduleTriggerRequest;
import fi.poltsi.vempain.admin.api.response.ScheduleTriggerResponse;
import fi.poltsi.vempain.admin.rest.ScheduleAPI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@RestController
public class ScheduleController implements ScheduleAPI {
	private ScheduledAnnotationBeanPostProcessor postProcessor;
	private TaskScheduler                        taskScheduler;

	@Override
	public ResponseEntity<List<ScheduleTriggerResponse>> getSchedules() {
		Set<ScheduledTask> scheduledTaskSet = postProcessor.getScheduledTasks();
		ArrayList<ScheduleTriggerResponse> scheduleTriggerResponses = new ArrayList<>();

		for (ScheduledTask scheduledTask : scheduledTaskSet) {
			scheduleTriggerResponses.add(ScheduleTriggerResponse.builder()
																.scheduleName(scheduledTask.getTask().toString())
																.status("ACTIVE")
																.build());
		}

		return ResponseEntity.ok(scheduleTriggerResponses);
	}

	@Override
	public ResponseEntity<ScheduleTriggerResponse> triggerSchedule(ScheduleTriggerRequest schedule) {
		Set<ScheduledTask> scheduledTaskSet = postProcessor.getScheduledTasks();

		for (ScheduledTask scheduledTask : scheduledTaskSet) {
			if (scheduledTask.getTask().toString().equals(schedule.getScheduleName())) {
				taskScheduler.schedule(scheduledTask.getTask().getRunnable(), Instant.now());

				return ResponseEntity.ok(ScheduleTriggerResponse.builder()
																.scheduleName(scheduledTask.getTask().toString())
																.status("ACTIVE")
																.build());
			}
		}

		return ResponseEntity.notFound().build();
	}


}
