package fi.poltsi.vempain.admin.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import fi.poltsi.vempain.auth.security.jwt.JwtUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller Test Class (CTC) for {@code GET /admin-management/statistics}: the system wide resource counts need a signed-in user and answer snake_case JSON.
 */
@SpringBootTest
@AutoConfigureMockMvc
class StatisticsCTC {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtils jwtUtils;

    @Test
    void answersTheStatisticsToASignedInUserOnly() throws Exception {
        mockMvc.perform(get("/admin-management/statistics"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/admin-management/statistics").header("Authorization", adminBearerToken()))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.generated_at").isString())
            .andExpect(jsonPath("$.resource_counts[0].name").value("page"))
            .andExpect(jsonPath("$.resource_counts.length()").value(6))
            .andExpect(jsonPath("$.publish_states.length()").value(3))
            .andExpect(jsonPath("$.files_by_type").isArray())
            .andExpect(jsonPath("$.published_by_month").isArray())
            .andExpect(jsonPath("$.created_by_month").isArray())
            .andExpect(jsonPath("$.resources_by_user").isArray())
            .andExpect(jsonPath("$.pages_by_form").isArray())
            .andExpect(jsonPath("$.forms_by_layout").isArray())
            .andExpect(jsonPath("$.forms_by_component").isArray())
            .andExpect(jsonPath("$.pages_by_path").isArray())
            .andExpect(jsonPath("$.files_by_path").isArray());
    }

    private String adminBearerToken() {
        return "Bearer "
                + jwtUtils.generateJwtTokenForUser("Vempain Administrator", "admin", "admin@nohost.nodomain")
                    .getTokenString();
    }
}
