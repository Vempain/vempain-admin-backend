package fi.poltsi.vempain.admin.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import fi.poltsi.vempain.auth.security.jwt.JwtUtils;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller Test Class (CTC) for the service-to-service user and unit listings of {@code FileIngestAPI}, which the file backend fetches to let its users pick
 * additional ACL grantees before publishing, and for the ACL lookups of an ingested site file or gallery.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FileIngestCTC {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtils jwtUtils;

    @Test
    void listsTheGrantableUsersWithReducedFieldsOnly() throws Exception {
        mockMvc.perform(get("/content-management/file/site-file/users").header("Authorization", adminBearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.login_name == 'admin')]", hasSize(1)))
            .andExpect(jsonPath("$[?(@.login_name == 'admin')].id").value(1))
            .andExpect(jsonPath("$[0].name").exists())
            .andExpect(jsonPath("$[0].nick").exists())
            .andExpect(jsonPath("$[0].email").doesNotExist())
            .andExpect(jsonPath("$[0].password").doesNotExist())
            .andExpect(jsonPath("$[0].birthday").doesNotExist());
    }

    @Test
    void listsTheGrantableUnitsWithReducedFieldsOnly() throws Exception {
        mockMvc.perform(get("/content-management/file/site-file/units").header("Authorization", adminBearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[*].id").exists())
            .andExpect(jsonPath("$[*].name").exists())
            .andExpect(jsonPath("$[*].acls").doesNotExist())
            .andExpect(jsonPath("$[*].user_ids").doesNotExist());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/content-management/file/site-file/users"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/content-management/file/site-file/units"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/content-management/file/site-file/1/acls"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/content-management/file/gallery/1/acls"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void theAclLookupsAnswerNotFoundForUnknownResources() throws Exception {
        mockMvc.perform(get("/content-management/file/site-file/987654321/acls").header("Authorization", adminBearerToken()))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/content-management/file/site-file/by-file-id/987654321/acls").header("Authorization", adminBearerToken()))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/content-management/file/gallery/987654321/acls").header("Authorization", adminBearerToken()))
            .andExpect(status().isNotFound());
    }

    private String adminBearerToken() {
        return "Bearer "
                + jwtUtils.generateJwtTokenForUser("Vempain Administrator", "admin", "admin@nohost.nodomain")
                    .getTokenString();
    }
}
