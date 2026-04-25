package fi.poltsi.vempain.admin.rest;

import fi.poltsi.vempain.admin.entity.DataEntity;
import fi.poltsi.vempain.admin.repository.DataRepository;
import fi.poltsi.vempain.auth.security.jwt.JwtUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"vempain.test=false"})
class DataRTC {
	private static final String IDENTIFIER = "publishable_music";
	private static final String TARGET_TABLE = "website_data__" + IDENTIFIER;

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private DataRepository dataRepository;
	@Autowired
	private JwtUtils jwtUtils;
	@Autowired
	@Qualifier("siteDataSource")
	private DataSource siteDataSource;

	private JdbcTemplate siteJdbcTemplate;

	@BeforeEach
	void setUp() {
		siteJdbcTemplate = new JdbcTemplate(siteDataSource);
		dataRepository.deleteAll();
		siteJdbcTemplate.execute("DROP TABLE IF EXISTS \"" + TARGET_TABLE + "\"");
		dataRepository.save(DataEntity.builder()
								 .identifier(IDENTIFIER)
								 .type("tabulated")
								 .description("Music data set for release flow")
								 .columnDefinitions("[{\"name\":\"title\",\"type\":\"string\"},{\"name\":\"artist\",\"type\":\"string\"}]")
								 .createSql("CREATE TABLE temporary_table_name (title TEXT, artist TEXT)")
								 .fetchAllSql("SELECT title, artist FROM website_data__publishable_music")
								 .fetchSubsetSql("SELECT title, artist FROM website_data__publishable_music WHERE artist = :artist")
								 .generated(Instant.now())
								 .csvData("title,artist\nAbbey Road,The Beatles\nOK Computer,Radiohead")
								 .createdAt(Instant.now())
								 .updatedAt(Instant.now())
								 .build());
	}

	@AfterEach
	void tearDown() {
		dataRepository.deleteAll();
		siteJdbcTemplate.execute("DROP TABLE IF EXISTS \"" + TARGET_TABLE + "\"");
	}

	@Test
	void publishEndpointCreatesAndPopulatesSiteTableOk() throws Exception {
		mockMvc.perform(post("/content-management/data/{identifier}/publish", IDENTIFIER)
						.header("Authorization", adminBearerToken()))
			   .andExpect(status().isOk())
			   .andExpect(jsonPath("$.identifier").value(IDENTIFIER))
			   .andExpect(jsonPath("$.type").value("tabulated"));

		var tableExists = siteJdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = ?",
				Integer.class,
				TARGET_TABLE);
		assertEquals(1, tableExists);

		var rowCount = siteJdbcTemplate.queryForObject("SELECT COUNT(*) FROM \"website_data__publishable_music\"", Integer.class);
		assertEquals(2, rowCount);
	}

	private String adminBearerToken() {
		return "Bearer " + jwtUtils.generateJwtTokenForUser("Vempain Administrator", "admin", "admin@nohost.nodomain")
								 .getTokenString();
	}
}

