package fi.poltsi.vempain.admin.task;

import fi.poltsi.vempain.admin.api.TaskTypeEnum;
import fi.poltsi.vempain.admin.service.DataService;
import fi.poltsi.vempain.admin.service.PublishService;
import fi.poltsi.vempain.admin.service.file.FileService;
import fi.poltsi.vempain.common.task.TaskCommandExecutor;
import fi.poltsi.vempain.common.task.TaskProgress;
import fi.poltsi.vempain.common.task.entity.TaskCompensationEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * The admin backend's {@link TaskCommandExecutor}: converts durable task commands into service calls. Services are resolved
 * through the Spring proxy so that their transactional methods run correctly on the worker thread. Publishing has no durable
 * compensations: a cancelled publish stops between items and leaves the already published items on the site.
 */
@Service
@RequiredArgsConstructor
public class AdminTaskCommandExecutor implements TaskCommandExecutor {
	private static final TypeReference<List<Long>> ID_LIST = new TypeReference<>() {
	};

	private final ApplicationContext applicationContext;
	private final ObjectMapper       objectMapper;

	@Override
	public Object execute(TaskProgress progress) throws Exception {
		var type = TaskTypeEnum.valueOf(progress.getType());
		JsonNode payload = objectMapper.readTree(progress.getPayload() == null ? "{}" : progress.getPayload());
		return switch (type) {
			case PUBLISH_PAGE -> publishService().publishPageNow(payload.path("page_id")
																		.asLong(), progress);
			case PUBLISH_ALL_PAGES -> publishService().publishPagesNow(ids(payload.path("page_ids")), progress);
			case PUBLISH_GALLERY, PUBLISH_ALL_GALLERIES, PUBLISH_SELECTED_GALLERIES -> publishService().publishGalleriesNow(ids(payload.path("gallery_ids")),
																															payload.path("skipped")
																																   .asInt(0), progress);
			case REFRESH_ALL_GALLERY_FILES -> applicationContext.getBean(FileService.class)
																.refreshAllGalleryFilesNow(ids(payload.path("gallery_ids")), progress);
			case PUBLISH_DATA_SET -> applicationContext.getBean(DataService.class)
													   .publishNow(payload.path("identifier")
																		  .asText(), progress);
		};
	}

	@Override
	public void compensate(TaskCompensationEntity compensation) {
		throw new IllegalArgumentException("Unknown task compensation command: " + compensation.getCommandType());
	}

	private PublishService publishService() {
		return applicationContext.getBean(PublishService.class);
	}

	private List<Long> ids(JsonNode node) {
		return node.isMissingNode() || node.isNull() ? List.of() : objectMapper.convertValue(node, ID_LIST);
	}
}
