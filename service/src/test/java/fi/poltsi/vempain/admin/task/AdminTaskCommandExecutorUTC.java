package fi.poltsi.vempain.admin.task;

import fi.poltsi.vempain.admin.api.response.DataResponse;
import fi.poltsi.vempain.admin.api.response.RefreshResponse;
import fi.poltsi.vempain.admin.service.DataService;
import fi.poltsi.vempain.admin.service.PublishService;
import fi.poltsi.vempain.admin.service.file.FileService;
import fi.poltsi.vempain.common.task.TaskProgress;
import fi.poltsi.vempain.common.task.entity.TaskCompensationEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTaskCommandExecutorUTC {
	@Mock
	private ApplicationContext applicationContext;
	@Mock
	private PublishService     publishService;
	@Mock
	private FileService        fileService;
	@Mock
	private DataService        dataService;

	private final ObjectMapper             mapper = new ObjectMapper();
	private       AdminTaskCommandExecutor executor;

	@BeforeEach
	void setUp() {
		executor = new AdminTaskCommandExecutor(applicationContext, mapper);
	}

	@Test
	void executesPagePublishCommands() throws Exception {
		var page = task("PUBLISH_PAGE", Map.of("page_id", 7L));
		var all = task("PUBLISH_ALL_PAGES", Map.of("page_ids", List.of(1L, 2L)));
		when(applicationContext.getBean(PublishService.class)).thenReturn(publishService);
		when(publishService.publishPageNow(7L, page)).thenReturn(Map.of("site_page_id", 70L));
		when(publishService.publishPagesNow(List.of(1L, 2L), all)).thenReturn(Map.of("published", 2L, "skipped", 0L));

		assertEquals(Map.of("site_page_id", 70L), executor.execute(page));
		assertEquals(Map.of("published", 2L, "skipped", 0L), executor.execute(all));
	}

	@Test
	void executesGalleryPublishCommandsWithTheSkippedCount() throws Exception {
		var single = task("PUBLISH_GALLERY", Map.of("gallery_ids", List.of(3L), "skipped", 0));
		var selected = task("PUBLISH_SELECTED_GALLERIES", Map.of("gallery_ids", List.of(3L, 4L), "skipped", 2));
		var all = task("PUBLISH_ALL_GALLERIES", Map.of("gallery_ids", List.of(), "skipped", 1));
		when(applicationContext.getBean(PublishService.class)).thenReturn(publishService);

		executor.execute(single);
		executor.execute(selected);
		executor.execute(all);

		verify(publishService).publishGalleriesNow(List.of(3L), 0, single);
		verify(publishService).publishGalleriesNow(List.of(3L, 4L), 2, selected);
		verify(publishService).publishGalleriesNow(List.of(), 1, all);
	}

	@Test
	void executesRefreshAndDataSetCommands() throws Exception {
		var refresh = task("REFRESH_ALL_GALLERY_FILES", Map.of("gallery_ids", List.of(5L)));
		var data = task("PUBLISH_DATA_SET", Map.of("identifier", "music_library"));
		var refreshResponse = RefreshResponse.builder()
											 .build();
		var dataResponse = DataResponse.builder()
									   .identifier("music_library")
									   .build();
		when(applicationContext.getBean(FileService.class)).thenReturn(fileService);
		when(applicationContext.getBean(DataService.class)).thenReturn(dataService);
		when(fileService.refreshAllGalleryFilesNow(List.of(5L), refresh)).thenReturn(refreshResponse);
		when(dataService.publishNow("music_library", data)).thenReturn(dataResponse);

		assertSame(refreshResponse, executor.execute(refresh));
		assertSame(dataResponse, executor.execute(data));
	}

	@Test
	void rejectsUnknownTaskTypesAndCompensations() {
		var unknown = task("UNKNOWN", Map.of());
		var compensation = new TaskCompensationEntity();
		compensation.setCommandType("ANYTHING");
		compensation.setPayload("{}");

		assertThrows(IllegalArgumentException.class, () -> executor.execute(unknown));
		assertThrows(IllegalArgumentException.class, () -> executor.compensate(compensation));
	}

	private TaskProgress task(String type, Object payload) {
		return TaskProgress.unmanaged("task-" + type, type, "title", 1L, 1, mapper.writeValueAsString(payload));
	}
}
