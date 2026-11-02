package fi.poltsi.vempain.admin.rest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import fi.poltsi.vempain.admin.entity.Subject;
import fi.poltsi.vempain.admin.repository.file.SubjectRepository;
import fi.poltsi.vempain.auth.security.jwt.JwtUtils;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller Test Class (CTC) for {@code GET /content-management/tags}: the tag suggestions of the page editor need a signed-in user, match the search text
 * case-insensitively, treat LIKE wildcards as text and are limited.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TagCTC {

    private static final String[] TAGS = { "Summer", "summer holiday", "Winter", "100% sure", "under_score" };

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private SubjectRepository subjectRepository;

    @BeforeEach
    void seed() {
        for (var tag : TAGS) {
            if (subjectRepository.findSubjectBySubjectName(tag)
                .isEmpty()) {
                subjectRepository.save(Subject.builder()
                    .subjectName(tag)
                    .build());
            }
        }
    }

    @AfterEach
    void cleanUp() {
        for (var tag : TAGS) {
            subjectRepository.findSubjectBySubjectName(tag)
                .ifPresent(subjectRepository::delete);
        }
    }

    private String bearerToken() {
        return "Bearer "
                + jwtUtils.generateJwtTokenForUser("Vempain Administrator", "admin", "admin@nohost.nodomain")
                    .getTokenString();
    }

    @Test
    void aSignedInUserFindsTagsCaseInsensitivelyAndLimited() throws Exception {
        mockMvc.perform(get("/content-management/tags").param("search", "sum"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/content-management/tags").header("Authorization", bearerToken())
            .param("search", "SUM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].tag_name").value("Summer"))
            .andExpect(jsonPath("$[0].id").isNumber())
            .andExpect(jsonPath("$[1].tag_name").value("summer holiday"));

        mockMvc.perform(get("/content-management/tags").header("Authorization", bearerToken())
            .param("search", "sum")
            .param("limit", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].tag_name").value("Summer"));

        // LIKE wildcards in the search text are plain characters
        mockMvc.perform(get("/content-management/tags").header("Authorization", bearerToken())
            .param("search", "%"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].tag_name").value("100% sure"));
        mockMvc.perform(get("/content-management/tags").header("Authorization", bearerToken())
            .param("search", "r_s"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].tag_name").value("under_score"));

        mockMvc.perform(get("/content-management/tags").header("Authorization", bearerToken())
            .param("search", "nothing like this"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }
}
