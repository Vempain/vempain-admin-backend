package fi.poltsi.vempain.admin.rest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import fi.poltsi.vempain.admin.repository.TagBlacklistRepository;
import fi.poltsi.vempain.auth.security.jwt.JwtUtils;
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller Test Class (CTC) for {@code /admin-management/tag-blacklist}: an administrator lists, adds (once, case-insensitively) and removes blacklisted
 * tags; the endpoints need a signed-in user.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TagBlacklistCTC {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private TagBlacklistRepository tagBlacklistRepository;

    @AfterEach
    void cleanUp() {
        tagBlacklistRepository.deleteAll();
    }

    private String adminBearerToken() {
        return "Bearer "
                + jwtUtils.generateJwtTokenForUser("Vempain Administrator", "admin", "admin@nohost.nodomain")
                    .getTokenString();
    }

    @Test
    void administratorManagesTheBlacklist() throws Exception {
        mockMvc.perform(get("/admin-management/tag-blacklist"))
            .andExpect(status().isUnauthorized());

        var created = mockMvc.perform(post("/admin-management/tag-blacklist").header("Authorization", adminBearerToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"tag_name\":\" Private \"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tag_name").value("Private"))
            .andExpect(jsonPath("$.created_by").value(1))
            .andExpect(jsonPath("$.created").isString())
            .andReturn()
            .getResponse()
            .getContentAsString();
        var id = JsonMapper.shared()
            .readTree(created)
            .get("id")
            .asLong();

        mockMvc.perform(post("/admin-management/tag-blacklist").header("Authorization", adminBearerToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"tag_name\":\"PRIVATE\"}"))
            .andExpect(status().isConflict());
        mockMvc.perform(post("/admin-management/tag-blacklist").header("Authorization", adminBearerToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"tag_name\":\" \"}"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/admin-management/tag-blacklist").header("Authorization", adminBearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(id))
            .andExpect(jsonPath("$[0].tag_name").value("Private"));

        mockMvc.perform(delete("/admin-management/tag-blacklist/" + id).header("Authorization", adminBearerToken()))
            .andExpect(status().isNoContent());
        mockMvc.perform(delete("/admin-management/tag-blacklist/" + id).header("Authorization", adminBearerToken()))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/admin-management/tag-blacklist").header("Authorization", adminBearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }
}
