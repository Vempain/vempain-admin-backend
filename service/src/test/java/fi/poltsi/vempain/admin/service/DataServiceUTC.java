package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.request.DataRequest;
import fi.poltsi.vempain.admin.api.response.DataResponse;
import fi.poltsi.vempain.admin.api.response.DataSummaryResponse;
import fi.poltsi.vempain.admin.entity.DataEntity;
import fi.poltsi.vempain.admin.repository.DataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataServiceUTC {
	private static final String TEST_IDENTIFIER = "test_data";

	@Mock
	private DataRepository dataRepository;

	private DataService dataService;
	private JdbcTemplate mockJdbcTemplate;

	@BeforeEach
	void setUp() {
		var mockDataSource = mock(DataSource.class);
		dataService = new DataService(dataRepository, mockDataSource);

		mockJdbcTemplate = mock(JdbcTemplate.class);
		// Inject the mock JdbcTemplate via reflection to control site DB interaction
		try {
			var field = DataService.class.getDeclaredField("siteJdbcTemplate");
			field.setAccessible(true);
			field.set(dataService, mockJdbcTemplate);
		} catch (Exception e) {
			throw new RuntimeException("Failed to inject mock JdbcTemplate", e);
		}
	}

	// findAll

	@Test
	void findAllOk() {
		var entities = buildEntityList(3);
		when(dataRepository.findAll()).thenReturn(entities);

		List<DataSummaryResponse> result = dataService.findAll();

		assertNotNull(result);
		assertEquals(3, result.size());
	}

	@Test
	void findAllEmptyOk() {
		when(dataRepository.findAll()).thenReturn(new ArrayList<>());

		List<DataSummaryResponse> result = dataService.findAll();

		assertNotNull(result);
		assertEquals(0, result.size());
	}

	// findByIdentifier

	@Test
	void findByIdentifierOk() {
		var entity = buildEntity(1L);
		when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.of(entity));

		DataResponse response = dataService.findByIdentifier(TEST_IDENTIFIER);

		assertNotNull(response);
		assertEquals(TEST_IDENTIFIER, response.getIdentifier());
	}

	@Test
	void findByIdentifierNotFoundFail() {
		when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.empty());

		try {
			dataService.findByIdentifier(TEST_IDENTIFIER);
			fail("Should have thrown ResponseStatusException");
		} catch (ResponseStatusException e) {
			assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
		}
	}

	// create

	@Test
	void createOk() {
		var request = buildValidRequest();
		var entity = buildEntity(1L);
		when(dataRepository.existsByIdentifier(TEST_IDENTIFIER)).thenReturn(false);
		when(dataRepository.save(any(DataEntity.class))).thenReturn(entity);

		DataResponse response = dataService.create(request);

		assertNotNull(response);
		assertEquals(TEST_IDENTIFIER, response.getIdentifier());
	}

	@Test
	void createDuplicateIdentifierFail() {
		var request = buildValidRequest();
		when(dataRepository.existsByIdentifier(TEST_IDENTIFIER)).thenReturn(true);

		try {
			dataService.create(request);
			fail("Should have thrown ResponseStatusException for duplicate identifier");
		} catch (ResponseStatusException e) {
			assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
		}
	}

	@Test
	void createNullRequestFail() {
		try {
			dataService.create(null);
			fail("Should have thrown ResponseStatusException for null request");
		} catch (ResponseStatusException e) {
			assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
		}
	}

	@Test
	void createInvalidIdentifierNullFail() {
		var request = buildValidRequest();
		request.setIdentifier(null);
		assertCreateBadRequest(request);
	}

	@Test
	void createInvalidIdentifierEmptyFail() {
		var request = buildValidRequest();
		request.setIdentifier("");
		assertCreateBadRequest(request);
	}

	@Test
	void createInvalidIdentifierStartsWithDigitFail() {
		var request = buildValidRequest();
		request.setIdentifier("1invalid");
		assertCreateBadRequest(request);
	}

	@Test
	void createInvalidIdentifierUppercaseFail() {
		var request = buildValidRequest();
		request.setIdentifier("Invalid");
		assertCreateBadRequest(request);
	}

	@Test
	void createInvalidIdentifierHyphenFail() {
		var request = buildValidRequest();
		request.setIdentifier("invalid-identifier");
		assertCreateBadRequest(request);
	}

	@Test
	void createMissingTypeFail() {
		var request = buildValidRequest();
		request.setType(null);
		assertCreateBadRequest(request);
	}

	@Test
	void createMissingColumnDefinitionsFail() {
		var request = buildValidRequest();
		request.setColumnDefinitions(null);
		assertCreateBadRequest(request);
	}

	@Test
	void createMissingCreateSqlFail() {
		var request = buildValidRequest();
		request.setCreateSql(null);
		assertCreateBadRequest(request);
	}

	@Test
	void createMissingFetchAllSqlFail() {
		var request = buildValidRequest();
		request.setFetchAllSql(null);
		assertCreateBadRequest(request);
	}

	@Test
	void createMissingFetchSubsetSqlFail() {
		var request = buildValidRequest();
		request.setFetchSubsetSql(null);
		assertCreateBadRequest(request);
	}

	@Test
	void createMissingCsvDataFail() {
		var request = buildValidRequest();
		request.setCsvData(null);
		assertCreateBadRequest(request);
	}

	// update

	@Test
	void updateOk() {
		var request = buildValidRequest();
		var entity = buildEntity(1L);
		when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.of(entity));
		when(dataRepository.save(any(DataEntity.class))).thenReturn(entity);

		DataResponse response = dataService.update(request);

		assertNotNull(response);
		assertEquals(TEST_IDENTIFIER, response.getIdentifier());
	}

	@Test
	void updateNotFoundFail() {
		var request = buildValidRequest();
		when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.empty());

		try {
			dataService.update(request);
			fail("Should have thrown ResponseStatusException");
		} catch (ResponseStatusException e) {
			assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
		}
	}

	@Test
	void updateNullRequestFail() {
		try {
			dataService.update(null);
			fail("Should have thrown ResponseStatusException");
		} catch (ResponseStatusException e) {
			assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
		}
	}

	// publish

	@Test
	void publishOk() {
		var entity = buildEntity(1L);
		when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.of(entity));

		DataResponse response = dataService.publish(TEST_IDENTIFIER);

		assertNotNull(response);
		assertEquals(TEST_IDENTIFIER, response.getIdentifier());
		verify(mockJdbcTemplate).execute("DROP TABLE IF EXISTS \"website_data__test_data\"");
		verify(mockJdbcTemplate).execute(entity.getCreateSql());
	}

	@Test
	void publishInvalidCreateSqlFail() {
		var entity = buildEntity(1L);
		entity.setCreateSql("DROP TABLE some_table");
		when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.of(entity));

		try {
			dataService.publish(TEST_IDENTIFIER);
			fail("Should have thrown ResponseStatusException for invalid create SQL");
		} catch (ResponseStatusException e) {
			assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
		}
	}

	@Test
	void publishNotFoundFail() {
		when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.empty());

		try {
			dataService.publish(TEST_IDENTIFIER);
			fail("Should have thrown ResponseStatusException");
		} catch (ResponseStatusException e) {
			assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
		}
	}

	@Test
	void publishSiteDbFailureThrowsInternalError() {
		var entity = buildEntity(1L);
		when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.of(entity));
		doThrow(new RuntimeException("DB failure")).when(mockJdbcTemplate).execute(anyString());

		try {
			dataService.publish(TEST_IDENTIFIER);
			fail("Should have thrown ResponseStatusException");
		} catch (ResponseStatusException e) {
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
		}
	}

	// Helpers

	private void assertCreateBadRequest(DataRequest request) {
		try {
			dataService.create(request);
			fail("Should have thrown ResponseStatusException for invalid request");
		} catch (ResponseStatusException e) {
			assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
		}
	}

	private DataRequest buildValidRequest() {
		return DataRequest.builder()
						  .identifier(TEST_IDENTIFIER)
						  .type("tabulated")
						  .description("Test data set")
						  .columnDefinitions("[{\"name\":\"title\",\"type\":\"string\"}]")
						  .createSql("CREATE TABLE website_data__test_data (id BIGSERIAL PRIMARY KEY, title VARCHAR(255))")
						  .fetchAllSql("SELECT * FROM website_data__test_data")
						  .fetchSubsetSql("SELECT * FROM website_data__test_data WHERE title = :title")
						  .dataTimestamp(Instant.now())
						  .csvData("title\nTest Album")
						  .build();
	}

	private DataEntity buildEntity(Long id) {
		var now = Instant.now();
		return DataEntity.builder()
						 .id(id)
						 .identifier(TEST_IDENTIFIER)
						 .type("tabulated")
						 .description("Test data set")
						 .columnDefinitions("[{\"name\":\"title\",\"type\":\"string\"}]")
						 .createSql("CREATE TABLE website_data__test_data (id BIGSERIAL PRIMARY KEY, title VARCHAR(255))")
						 .fetchAllSql("SELECT * FROM website_data__test_data")
						 .fetchSubsetSql("SELECT * FROM website_data__test_data WHERE title = :title")
						 .dataTimestamp(now)
						 .csvData("title\nTest Album")
						 .createdAt(now)
						 .updatedAt(now)
						 .build();
	}

	private List<DataEntity> buildEntityList(int count) {
		var entities = new ArrayList<DataEntity>();
		for (int i = 0; i < count; i++) {
			var now = Instant.now();
			entities.add(DataEntity.builder()
								   .id((long) (i + 1))
								   .identifier("data_set_" + i)
								   .type("tabulated")
								   .description("Test data set " + i)
								   .columnDefinitions("[{\"name\":\"value\",\"type\":\"string\"}]")
								   .createSql("CREATE TABLE website_data__data_set_" + i + " (id BIGSERIAL PRIMARY KEY, value TEXT)")
								   .fetchAllSql("SELECT * FROM website_data__data_set_" + i)
								   .fetchSubsetSql("SELECT * FROM website_data__data_set_" + i + " WHERE value = :value")
								   .dataTimestamp(now)
								   .csvData("value\ntest")
								   .createdAt(now)
								   .updatedAt(now)
								   .build());
		}
		return entities;
	}
}
