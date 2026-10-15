package fi.poltsi.vempain.admin.controller;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.Task;
import org.springframework.security.access.AccessDeniedException;

import fi.poltsi.vempain.admin.api.ContentTypeEnum;
import fi.poltsi.vempain.admin.api.PublishStatusEnum;
import fi.poltsi.vempain.admin.api.request.PublishScheduleRequest;
import fi.poltsi.vempain.admin.api.request.TriggerSystemScheduleRequest;
import fi.poltsi.vempain.admin.api.response.PublishScheduleResponse;
import fi.poltsi.vempain.admin.api.response.file.FileImportScheduleResponse;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.admin.service.ScheduleService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Every schedule endpoint requires the administrator ACL; the system schedules are looked up by the name of their task and triggered through the task scheduler
 * after the requested delay.
 */
@ExtendWith(MockitoExtension.class)
class ScheduleControllerUTC {

    @Mock
    private ScheduledAnnotationBeanPostProcessor postProcessor;
    @Mock
    private TaskScheduler taskScheduler;
    @Mock
    private ScheduleService scheduleService;
    @Mock
    private AccessService accessService;

    @InjectMocks
    private ScheduleController controller;

    private static Runnable namedRunnable(String name) {
        return new Runnable() {

            @Override
            public void run() {}

            @Override
            public String toString() {
                return name;
            }
        };
    }

    private Set<ScheduledTask> scheduledTasks(String... names) {
        var tasks = new LinkedHashSet<ScheduledTask>();
        for (var name : names) {
            var scheduledTask = mock(ScheduledTask.class);
            when(scheduledTask.getTask()).thenReturn(new Task(namedRunnable(name)));
            tasks.add(scheduledTask);
        }
        return tasks;
    }

    @Test
    void systemSchedulesAreListedWithRunningIds() {
        var tasks = scheduledTasks("AclConsistencySchedule.verify", "PublishItemSchedule.publishItems");
        when(postProcessor.getScheduledTasks()).thenReturn(tasks);

        var response = controller.getSystemSchedules();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody()
            .size());
        assertEquals(1L, response.getBody()
            .getFirst()
            .getId());
        assertEquals("AclConsistencySchedule.verify", response.getBody()
            .getFirst()
            .getScheduleName());
        assertEquals(2L, response.getBody()
            .get(1)
            .getId());
        assertEquals("ACTIVE", response.getBody()
            .get(1)
            .getStatus());
        verify(accessService).checkAdminAccess();
    }

    @Test
    void aSystemScheduleIsFoundByItsNameOrAnswersNotFound() {
        var tasks = scheduledTasks("AclConsistencySchedule.verify");
        when(postProcessor.getScheduledTasks()).thenReturn(tasks);

        var found = controller.getSystemScheduleByName("AclConsistencySchedule.verify");
        assertEquals(HttpStatus.OK, found.getStatusCode());
        assertEquals("AclConsistencySchedule.verify", found.getBody()
            .getScheduleName());

        var missing = controller.getSystemScheduleByName("NoSuchSchedule.run");
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());
        assertNull(missing.getBody());
    }

    @Test
    void triggeringASystemScheduleRunsItAfterTheDelayOrAnswersNotFound() {
        var tasks = scheduledTasks("AclConsistencySchedule.verify");
        when(postProcessor.getScheduledTasks()).thenReturn(tasks);
        var before = Instant.now();

        var triggered = controller.triggerSystemSchedule(TriggerSystemScheduleRequest.builder()
            .scheduleName("AclConsistencySchedule.verify")
            .delay(60L)
            .build());

        assertEquals(HttpStatus.OK, triggered.getStatusCode());
        verify(taskScheduler).schedule(any(Runnable.class), org.mockito.ArgumentMatchers.argThat((Instant at) -> !at.isBefore(before.plusSeconds(60))));

        var missing = controller.triggerSystemSchedule(TriggerSystemScheduleRequest.builder()
            .scheduleName("NoSuchSchedule.run")
            .delay(1L)
            .build());
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());
        verify(taskScheduler, never()).schedule(any(Runnable.class), eq(before));
    }

    @Test
    void publishingSchedulesAreListedFetchedAndTriggered() {
        var schedule = PublishScheduleResponse.builder()
            .id(1L)
            .publishId(10L)
            .publishType(ContentTypeEnum.PAGE)
            .publishStatus(PublishStatusEnum.NOT_PUBLISHED)
            .build();
        when(scheduleService.getUpcomingPublishSchedules()).thenReturn(List.of(schedule));
        when(scheduleService.getPublishScheduleById(1L)).thenReturn(schedule);
        when(scheduleService.getPublishScheduleById(2L)).thenReturn(null);
        var trigger = PublishScheduleRequest.builder()
            .publishId(10L)
            .publishTime(Instant.now())
            .build();
        when(scheduleService.triggerPublishSchedule(trigger)).thenReturn(schedule, (PublishScheduleResponse) null);

        assertEquals(1, controller.listPublishingSchedules()
            .getBody()
            .size());
        assertEquals(schedule, controller.getPublishingScheduleById(1L)
            .getBody());
        assertEquals(HttpStatus.NOT_FOUND, controller.getPublishingScheduleById(2L)
            .getStatusCode());
        assertEquals(schedule, controller.triggerPublishSchedule(trigger)
            .getBody());
        assertEquals(HttpStatus.NOT_FOUND, controller.triggerPublishSchedule(trigger)
            .getStatusCode());
    }

    @Test
    void fileImportSchedulesAreListedAndFetched() {
        var schedule = FileImportScheduleResponse.builder()
            .id(1L)
            .build();
        when(scheduleService.getUpcomingFileImportSchedules()).thenReturn(List.of(schedule));
        when(scheduleService.getFileImportScheduleById(1L)).thenReturn(schedule);
        when(scheduleService.getFileImportScheduleById(2L)).thenReturn(null);

        assertEquals(1, controller.listFileImportSchedules()
            .getBody()
            .size());
        assertEquals(schedule, controller.getFileImportScheduleById(1L)
            .getBody());
        assertEquals(HttpStatus.NOT_FOUND, controller.getFileImportScheduleById(2L)
            .getStatusCode());
    }

    @Test
    void everyEndpointRequiresTheAdministratorAcl() {
        doThrow(new AccessDeniedException("not an administrator")).when(accessService)
            .checkAdminAccess();

        assertThrows(AccessDeniedException.class, () -> controller.getSystemSchedules());
        assertThrows(AccessDeniedException.class, () -> controller.getSystemScheduleByName("x"));
        assertThrows(AccessDeniedException.class, () -> controller.triggerSystemSchedule(new TriggerSystemScheduleRequest()));
        assertThrows(AccessDeniedException.class, () -> controller.listPublishingSchedules());
        assertThrows(AccessDeniedException.class, () -> controller.getPublishingScheduleById(1L));
        assertThrows(AccessDeniedException.class, () -> controller.triggerPublishSchedule(new PublishScheduleRequest()));
        assertThrows(AccessDeniedException.class, () -> controller.listFileImportSchedules());
        assertThrows(AccessDeniedException.class, () -> controller.getFileImportScheduleById(1L));
        verify(postProcessor, never()).getScheduledTasks();
        verify(scheduleService, never()).getUpcomingPublishSchedules();
    }
}
