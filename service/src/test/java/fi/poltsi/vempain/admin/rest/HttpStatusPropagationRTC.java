package fi.poltsi.vempain.admin.rest;

import fi.poltsi.vempain.admin.entity.DataEntity;
import fi.poltsi.vempain.admin.repository.DataRepository;
import fi.poltsi.vempain.admin.service.DataService;
import fi.poltsi.vempain.auth.security.jwt.JwtUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"vempain.test=false"})
class HttpStatusPropagationRTC {
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private DataRepository dataRepository;
  @Autowired
  private JwtUtils jwtUtils;
  @MockitoSpyBean
  private DataService dataService;

  @BeforeEach
void setUp() {
    dataRepository.deleteAll();
    dataRepository.save(DataEntity.builder()
                  .identifier("existing_dataset")
                  .type("tabulated")
                  .description("Existing data set")
                  .columnDefinitions("[{\"name\":\"value\",\"type\":\"string\"}]")
                  .createSql("CREATE TABLE website_data__existing_dataset (value VARCHAR(255))")
                  .fetchAllSql("SELECT value FROM website_data__existing_dataset")
                  .fetchSubsetSql("SELECT value FROM website_data__existing_dataset WHERE value = :value")
                  .generated(Instant.now())
                  .csvData("value\nhello")
                  .createdAt(Instant.now())
                  .updatedAt(Instant.now())
                  .build());
}

  @AfterEach
void tearDown() {
    Mockito.reset(dataService);
}

  @Test
@DisplayName("Existing data set request returns 200")
void existingDataSetReturns200() throws Exception {
    mockMvc.perform(get("/content-management/data/existing_dataset")
                .header("Authorization", adminBearerToken()))
         .andExpect(status().isOk())
         .andExpect(jsonPath("$.identifier").value("existing_dataset"));
}

  @Test
@DisplayName("Missing data set thrown as ResponseStatusException returns 404")
void missingDataSetReturns404() throws Exception {
    mockMvc.perform(get("/content-management/data/does_not_exist")
                .header("Authorization", adminBearerToken()))
         .andExpect(status().isNotFound());
}

  @Test
@DisplayName("Missing form returns 404 instead of 403")
void missingFormReturns404() throws Exception {
    mockMvc.perform(get("/content-management/forms/999999")
                .header("Authorization", adminBearerToken()))
         .andExpect(status().isNotFound());
}

  @Test
@DisplayName("Direct ResponseEntity notFound path still returns 404")
void directNotFoundResponseStillReturns404() throws Exception {
    mockMvc.perform(get("/content-management/pages/999999")
                .header("Authorization", adminBearerToken()))
         .andExpect(status().isNotFound());
}

  @Test
@DisplayName("Unauthenticated request returns 401")
void unauthenticatedRequestReturns401() throws Exception {
    mockMvc.perform(get("/content-management/data"))
         .andExpect(status().isUnauthorized());
}

  @Test
@DisplayName("Authenticated access denied still returns 403")
void accessDeniedStillReturns403() throws Exception {
    doThrow(new AccessDeniedException("Forbidden"))
        .when(dataService)
        .findByIdentifier("forbidden_dataset");

    mockMvc.perform(get("/content-management/data/forbidden_dataset")
                .header("Authorization", adminBearerToken()))
         .andExpect(status().isForbidden());
}

  private String adminBearerToken() {
    return "Bearer " + jwtUtils.generateJwtTokenForUser("Vempain Administrator", "admin", "admin@nohost.nodomain")
                   .getTokenString();
}
}
