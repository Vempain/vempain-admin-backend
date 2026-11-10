package fi.poltsi.vempain.admin.rest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import fi.poltsi.vempain.admin.repository.ApiTokenRepository;
import fi.poltsi.vempain.auth.security.jwt.JwtUtils;
import fi.poltsi.vempain.common.api.ApiTokenScopeEnum;
import fi.poltsi.vempain.common.api.request.ApiTokenRequest;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class McpReadCTC {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private ApiTokenRepository apiTokenRepository;

    @AfterEach
    void cleanUp() {
        apiTokenRepository.deleteAll();
    }

    @Test
    void mcpReadTokenGetsPagedSystemWideDataAndJwtIsForbidden() throws Exception {
        var request = ApiTokenRequest.builder()
            .description("vempain-mcp")
            .network("10.0.9.0/24")
            .expiresAt(Instant.now()
                .plus(1, ChronoUnit.DAYS))
            .scope(ApiTokenScopeEnum.MCP_READ)
            .build();
        var tokenJson = mockMvc.perform(post("/admin-management/api-tokens").header("Authorization", adminBearerToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        var token = objectMapper.readTree(tokenJson)
            .path("token")
            .asString();

        mockMvc.perform(get("/mcp/pages").param("page", "0")
            .param("size", "1")
            .header("X-Vempain-Api-Token", token)
            .with(req -> {
                req.setRemoteAddr("10.0.9.5");
                return req;
            }))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.total_elements").exists());

        mockMvc.perform(get("/mcp/pages").header("Authorization", adminBearerToken()))
            .andExpect(status().isForbidden());
    }

    private String adminBearerToken() {
        return "Bearer "
                + jwtUtils.generateJwtTokenForUser("Vempain Administrator", "admin", "admin@nohost.nodomain")
                    .getTokenString();
    }
}
