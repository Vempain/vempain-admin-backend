package fi.poltsi.vempain.admin.rest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import fi.poltsi.vempain.admin.api.ContentTypeEnum;
import fi.poltsi.vempain.admin.service.ScheduleService;
import fi.poltsi.vempain.auth.security.jwt.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class ScheduleCTC {

    private static final String SCHEDULE_PREFIX = "/schedule-management";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ScheduleService scheduleService;

    private final JsonMapper jsonMapper = JsonMapper.builder()
        .build();

    @Test
    void getPublishingSchedulesOk() throws Exception {
        MvcResult result = mockMvc.perform(get(SCHEDULE_PREFIX + "/publishing").header("Authorization", adminBearerToken())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        assertNotNull(result);
        assertNotNull(result.getResponse());
    }

    @Test
    void getPublishingScheduleByIdOk() throws Exception {
        MvcResult result = mockMvc.perform(get(SCHEDULE_PREFIX + "/publishing/1").header("Authorization", adminBearerToken())
            .contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        assertNotNull(result);
    }

    @Test
    void getFileImportSchedulesOk() throws Exception {
        MvcResult result = mockMvc.perform(get(SCHEDULE_PREFIX + "/file-imports").header("Authorization", adminBearerToken())
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
        MvcResult result = mockMvc.perform(get(SCHEDULE_PREFIX + "/file-imports/1").header("Authorization", adminBearerToken())
            .contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        assertNotNull(result);
    }

    @Test
    void getSystemSchedulesOk() throws Exception {
        MvcResult result = mockMvc.perform(get(SCHEDULE_PREFIX + "/system-schedules").header("Authorization", adminBearerToken())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        assertNotNull(result);
        assertNotNull(result.getResponse());
    }

    private String adminBearerToken() {
        return "Bearer "
                + jwtUtils.generateJwtTokenForUser("Vempain Administrator", "admin", "admin@nohost.nodomain")
                    .getTokenString();
    }

    @Test
    void systemSchedulesAreFoundByNameAndUnknownNamesAnswerNotFound() throws Exception {
        var listed = mockMvc.perform(get(SCHEDULE_PREFIX + "/system-schedules").header("Authorization", adminBearerToken()))
            .andExpect(status().isOk())
            .andReturn();
        var schedules = jsonMapper.readTree(listed.getResponse()
            .getContentAsString());
        assertFalse(schedules.isEmpty(), "the context registers the scheduled jobs");
        var name = schedules.get(0)
            .get("schedule_name")
            .asString();

        mockMvc.perform(get(SCHEDULE_PREFIX + "/system-schedules/{name}", name).header("Authorization", adminBearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.schedule_name").value(name))
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get(SCHEDULE_PREFIX + "/system-schedules/{name}", "no.such.Schedule").header("Authorization", adminBearerToken()))
            .andExpect(status().isNotFound());
    }

    @Test
    void systemSchedulesAreTriggeredByNameAfterADelay() throws Exception {
        var listed = mockMvc.perform(get(SCHEDULE_PREFIX + "/system-schedules").header("Authorization", adminBearerToken()))
            .andExpect(status().isOk())
            .andReturn();
        var name = jsonMapper.readTree(listed.getResponse()
            .getContentAsString())
            .get(0)
            .get("schedule_name")
            .asString();

        // A long delay: the run is scheduled far after the test context closes
        mockMvc.perform(post(SCHEDULE_PREFIX + "/system-schedules").header("Authorization", adminBearerToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"schedule_name\": \"" + name + "\", \"delay\": 86400}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.schedule_name").value(name));

        mockMvc.perform(post(SCHEDULE_PREFIX + "/system-schedules").header("Authorization", adminBearerToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"schedule_name\": \"no.such.Schedule\", \"delay\": 1}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void publishingSchedulesAreFetchedAndTriggeredById() throws Exception {
        var publishTime = Instant.now()
            .plus(1, ChronoUnit.DAYS)
            .truncatedTo(ChronoUnit.SECONDS);
        var schedule = scheduleService.schedulePublish(publishTime, 1L, ContentTypeEnum.PAGE, "CTC schedule");

        mockMvc.perform(get(SCHEDULE_PREFIX + "/publishing/{id}", schedule.getId()).header("Authorization", adminBearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publish_id").value(1))
            .andExpect(jsonPath("$.publish_type").value("PAGE"));

        mockMvc.perform(get(SCHEDULE_PREFIX + "/publishing/{id}", 999_999L).header("Authorization", adminBearerToken()))
            .andExpect(status().isNotFound());

        mockMvc.perform(post(SCHEDULE_PREFIX + "/publishing").header("Authorization", adminBearerToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"publish_id\": " + schedule.getId() + ", \"publish_time\": \"" + publishTime.plusSeconds(3600) + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publish_status").value("NOT_PUBLISHED"));

        mockMvc.perform(post(SCHEDULE_PREFIX + "/publishing").header("Authorization", adminBearerToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"publish_id\": 999999, \"publish_time\": \"" + publishTime + "\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void schedulesRequireAuthentication() throws Exception {
        mockMvc.perform(get(SCHEDULE_PREFIX + "/system-schedules"))
            .andExpect(status().isUnauthorized());
    }
}
