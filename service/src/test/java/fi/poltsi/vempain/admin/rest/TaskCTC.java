package fi.poltsi.vempain.admin.rest;

import fi.poltsi.vempain.auth.security.jwt.JwtUtils;
import fi.poltsi.vempain.common.task.TaskProgressStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller Test Class (CTC) for the shared task progress API ({@code TaskAPI} of vempain-common) as hosted by the admin backend:
 * listing, polling, cancelling and dismissing background tasks, which are private to the user who started them.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TaskCTC {
	private static final long ADMIN_USER_ID = 1L;
	private static final long OTHER_USER_ID = 2L;

	@Autowired
	private MockMvc           mockMvc;
	@Autowired
	private TaskProgressStore store;
	@Autowired
	private JwtUtils          jwtUtils;

	@Test
	void listsOnlyTheCallersTasksAndPollsThem() throws Exception {
		var own = store.create("PUBLISH_ALL_PAGES", "Publish all pages", ADMIN_USER_ID, 4);
		var other = store.create("PUBLISH_ALL_PAGES", "Not mine", OTHER_USER_ID, 1);
		own.advance("Published page 1");
		own.advance("Published page 2");

		try {
			mockMvc.perform(get("/tasks").header("Authorization", adminBearerToken()))
				   .andExpect(status().isOk())
				   .andExpect(jsonPath("$[?(@.task_id == '" + own.getId() + "')]", hasSize(1)))
				   .andExpect(jsonPath("$[?(@.task_id == '" + other.getId() + "')]", hasSize(0)));

			mockMvc.perform(get("/tasks/" + own.getId()).header("Authorization", adminBearerToken()))
				   .andExpect(status().isOk())
				   .andExpect(jsonPath("$.task_id").value(own.getId()))
				   .andExpect(jsonPath("$.type").value("PUBLISH_ALL_PAGES"))
				   .andExpect(jsonPath("$.status").value("QUEUED"))
				   .andExpect(jsonPath("$.total_steps").value(4))
				   .andExpect(jsonPath("$.completed_steps").value(2))
				   .andExpect(jsonPath("$.percent").value(50))
				   .andExpect(jsonPath("$.message").value("Published page 2"));

			mockMvc.perform(get("/tasks/" + other.getId()).header("Authorization", adminBearerToken()))
				   .andExpect(status().isNotFound());
			mockMvc.perform(get("/tasks/does-not-exist").header("Authorization", adminBearerToken()))
				   .andExpect(status().isNotFound());
			mockMvc.perform(get("/tasks"))
				   .andExpect(status().isUnauthorized());
		} finally {
			store.remove(own.getId());
			store.remove(other.getId());
		}
	}

	@Test
	void cancelMarksOwnActiveTasksAndDismissRemovesFinishedOnes() throws Exception {
		var queued = store.create("PUBLISH_GALLERY", "Publish gallery", ADMIN_USER_ID, 3);
		var finished = store.create("PUBLISH_GALLERY", "Publish gallery", ADMIN_USER_ID, 0);
		var other = store.create("PUBLISH_GALLERY", "Publish gallery", OTHER_USER_ID, 0);
		ReflectionTestUtils.invokeMethod(finished, "complete", (Object) null);
		ReflectionTestUtils.invokeMethod(other, "complete", (Object) null);

		try {
			mockMvc.perform(post("/tasks/" + queued.getId() + "/cancel").header("Authorization", adminBearerToken()))
				   .andExpect(status().isAccepted())
				   .andExpect(jsonPath("$.task_id").value(queued.getId()))
				   .andExpect(jsonPath("$.cancel_requested").value(true));
			assertTrue(queued.isCancelRequested());
			mockMvc.perform(post("/tasks/" + finished.getId() + "/cancel").header("Authorization", adminBearerToken()))
				   .andExpect(status().isConflict());
			mockMvc.perform(post("/tasks/" + other.getId() + "/cancel").header("Authorization", adminBearerToken()))
				   .andExpect(status().isNotFound());

			mockMvc.perform(delete("/tasks/" + queued.getId()).header("Authorization", adminBearerToken()))
				   .andExpect(status().isConflict());
			mockMvc.perform(delete("/tasks/" + other.getId()).header("Authorization", adminBearerToken()))
				   .andExpect(status().isNotFound());
			mockMvc.perform(delete("/tasks/" + finished.getId()).header("Authorization", adminBearerToken()))
				   .andExpect(status().isNoContent());
			mockMvc.perform(get("/tasks/" + finished.getId()).header("Authorization", adminBearerToken()))
				   .andExpect(status().isNotFound());
		} finally {
			store.remove(queued.getId());
			store.remove(finished.getId());
			store.remove(other.getId());
		}
	}

	private String adminBearerToken() {
		return "Bearer " + jwtUtils.generateJwtTokenForUser("Vempain Administrator", "admin", "admin@nohost.nodomain")
								   .getTokenString();
	}
}
