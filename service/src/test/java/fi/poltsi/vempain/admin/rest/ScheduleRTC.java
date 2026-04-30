package fi.poltsi.vempain.admin.rest;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class ScheduleRTC {

	private static final String SCHEDULE_PREFIX = "/schedule-management";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void getPublishingSchedulesOk() throws Exception {
		MvcResult result = mockMvc.perform(get(SCHEDULE_PREFIX + "/publishing")
												   .contentType(MediaType.APPLICATION_JSON))
								  .andExpect(status().isOk())
								  .andExpect(content().contentType(MediaType.APPLICATION_JSON))
								  .andReturn();
		assertNotNull(result);
		assertNotNull(result.getResponse());
	}

	@Test
	void getPublishingScheduleByIdOk() throws Exception {
		MvcResult result = mockMvc.perform(get(SCHEDULE_PREFIX + "/publishing/1")
												   .contentType(MediaType.APPLICATION_JSON))
								  .andReturn();
		assertNotNull(result);
	}

	@Test
	void getFileImportSchedulesOk() throws Exception {
		MvcResult result = mockMvc.perform(get(SCHEDULE_PREFIX + "/file-imports")
												   .contentType(MediaType.APPLICATION_JSON))
								  .andExpect(status().isOk())
								  .andExpect(content().contentType(MediaType.APPLICATION_JSON))
								  .andReturn();
		assertNotNull(result);
		assertNotNull(result.getResponse());
	}

	@Disabled("Requires a valid file import schedule to exist")
	@Test
	void getFileImportScheduleByIdOk() throws Exception {
		MvcResult result = mockMvc.perform(get(SCHEDULE_PREFIX + "/file-imports/1")
												   .contentType(MediaType.APPLICATION_JSON))
								  .andReturn();
		assertNotNull(result);
	}

	@Test
	void getSystemSchedulesOk() throws Exception {
		MvcResult result = mockMvc.perform(get(SCHEDULE_PREFIX + "/system-schedules")
												   .contentType(MediaType.APPLICATION_JSON))
								  .andExpect(status().isOk())
								  .andExpect(content().contentType(MediaType.APPLICATION_JSON))
								  .andReturn();
		assertNotNull(result);
		assertNotNull(result.getResponse());
	}
}
