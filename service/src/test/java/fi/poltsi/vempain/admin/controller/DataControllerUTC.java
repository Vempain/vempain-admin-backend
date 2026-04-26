package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.request.DataRequest;
import fi.poltsi.vempain.admin.api.response.DataResponse;
import fi.poltsi.vempain.admin.api.response.DataSummaryResponse;
import fi.poltsi.vempain.admin.service.DataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataControllerUTC {
	private static final String TEST_IDENTIFIER = "test_data";

	@Mock
	private DataService dataService;

	@InjectMocks
	private DataController dataController;

	private DataRequest validRequest;
	private DataResponse dataResponse;
	private DataSummaryResponse summaryResponse;

	@BeforeEach
	void setUp() {
		validRequest = buildValidRequest();
		dataResponse = buildDataResponse();
		summaryResponse = buildSummaryResponse();
	}

	// GET all

	@Test
	void getAllDataSetsOk() {
		when(dataService.findAll(null, null, null)).thenReturn(List.of(summaryResponse));

		ResponseEntity<List<DataSummaryResponse>> response = dataController.getAllDataSets(null, null, null);

		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(1, response.getBody().size());
	}

	@Test
	void getAllDataSetsEmptyOk() {
		when(dataService.findAll(null, null, null)).thenReturn(new ArrayList<>());

		ResponseEntity<List<DataSummaryResponse>> response = dataController.getAllDataSets(null, null, null);

		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(0, response.getBody().size());
	}

	@Test
	void getAllDataSetsFilteredOk() {
		when(dataService.findAll("time_series", "gps_timeseries_", "trip")).thenReturn(List.of(summaryResponse));

		ResponseEntity<List<DataSummaryResponse>> response = dataController.getAllDataSets("time_series", "gps_timeseries_", "trip");

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(1, response.getBody().size());
	}

	// GET by identifier

	@Test
	void getDataSetByIdentifierOk() {
		when(dataService.findByIdentifier(TEST_IDENTIFIER)).thenReturn(dataResponse);

		ResponseEntity<DataResponse> response = dataController.getDataSetByIdentifier(TEST_IDENTIFIER);

		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(TEST_IDENTIFIER, response.getBody().getIdentifier());
	}

	@Test
	void getDataSetByIdentifierNotFoundFail() {
		when(dataService.findByIdentifier(TEST_IDENTIFIER))
				.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, VempainMessages.OBJECT_NOT_FOUND));

		try {
			dataController.getDataSetByIdentifier(TEST_IDENTIFIER);
			fail("Should have thrown ResponseStatusException");
		} catch (ResponseStatusException e) {
			assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
		}
	}

	// POST create

	@Test
	void createDataSetOk() {
		when(dataService.create(validRequest)).thenReturn(dataResponse);

		ResponseEntity<DataResponse> response = dataController.createDataSet(validRequest);

		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(TEST_IDENTIFIER, response.getBody().getIdentifier());
	}

	@Test
	void createDataSetConflictFail() {
		when(dataService.create(validRequest))
				.thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, VempainMessages.OBJECT_NAME_ALREADY_EXISTS));

		try {
			dataController.createDataSet(validRequest);
			fail("Should have thrown ResponseStatusException");
		} catch (ResponseStatusException e) {
			assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
		}
	}

	@Test
	void createDataSetBadRequestFail() {
		when(dataService.create(validRequest))
				.thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_OBJECT_IN_REQUEST));

		try {
			dataController.createDataSet(validRequest);
			fail("Should have thrown ResponseStatusException");
		} catch (ResponseStatusException e) {
			assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
		}
	}

	// PUT update

	@Test
	void updateDataSetOk() {
		when(dataService.update(validRequest)).thenReturn(dataResponse);

		ResponseEntity<DataResponse> response = dataController.updateDataSet(validRequest);

		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(TEST_IDENTIFIER, response.getBody().getIdentifier());
	}

	@Test
	void updateDataSetNotFoundFail() {
		when(dataService.update(validRequest))
				.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, VempainMessages.OBJECT_NOT_FOUND));

		try {
			dataController.updateDataSet(validRequest);
			fail("Should have thrown ResponseStatusException");
		} catch (ResponseStatusException e) {
			assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
		}
	}

	@Test
	void updateDataSetBadRequestFail() {
		when(dataService.update(validRequest))
				.thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_OBJECT_IN_REQUEST));

		try {
			dataController.updateDataSet(validRequest);
			fail("Should have thrown ResponseStatusException");
		} catch (ResponseStatusException e) {
			assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
		}
	}

	// POST publish

	@Test
	void publishDataSetOk() {
		when(dataService.publish(TEST_IDENTIFIER)).thenReturn(dataResponse);

		ResponseEntity<DataResponse> response = dataController.publishDataSet(TEST_IDENTIFIER);

		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(TEST_IDENTIFIER, response.getBody().getIdentifier());
	}

	@Test
	void publishDataSetNotFoundFail() {
		when(dataService.publish(TEST_IDENTIFIER))
				.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, VempainMessages.OBJECT_NOT_FOUND));

		try {
			dataController.publishDataSet(TEST_IDENTIFIER);
			fail("Should have thrown ResponseStatusException");
		} catch (ResponseStatusException e) {
			assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
		}
	}

	@Test
	void publishDataSetInternalErrorFail() {
		when(dataService.publish(TEST_IDENTIFIER))
				.thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, VempainMessages.INTERNAL_ERROR));

		try {
			dataController.publishDataSet(TEST_IDENTIFIER);
			fail("Should have thrown ResponseStatusException");
		} catch (ResponseStatusException e) {
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
		}
	}

	// Helpers

	private DataRequest buildValidRequest() {
		return DataRequest.builder()
						  .identifier(TEST_IDENTIFIER)
						  .type("tabulated")
						  .description("Test data set")
						  .columnDefinitions("[{\"name\":\"title\",\"type\":\"string\"},{\"name\":\"year\",\"type\":\"integer\"}]")
						  .createSql("CREATE TABLE website_data__test_data (id BIGSERIAL PRIMARY KEY, title VARCHAR(255), year INTEGER)")
						  .fetchAllSql("SELECT * FROM website_data__test_data")
						  .fetchSubsetSql("SELECT * FROM website_data__test_data WHERE year = :year")
						  .generated(Instant.now())
						  .csvData("title,year\nTest Album,2024")
						  .build();
	}

	private DataResponse buildDataResponse() {
		return DataResponse.builder()
						   .id(1L)
						   .identifier(TEST_IDENTIFIER)
						   .type("tabulated")
						   .description("Test data set")
						   .columnDefinitions("[{\"name\":\"title\",\"type\":\"string\"},{\"name\":\"year\",\"type\":\"integer\"}]")
						   .createSql("CREATE TABLE website_data__test_data (id BIGSERIAL PRIMARY KEY, title VARCHAR(255), year INTEGER)")
						   .fetchAllSql("SELECT * FROM website_data__test_data")
						   .fetchSubsetSql("SELECT * FROM website_data__test_data WHERE year = :year")
						   .generated(Instant.now())
						   .csvData("title,year\nTest Album,2024")
						   .createdAt(Instant.now())
						   .updatedAt(Instant.now())
						   .build();
	}

	private DataSummaryResponse buildSummaryResponse() {
		return DataSummaryResponse.builder()
								  .id(1L)
								  .identifier(TEST_IDENTIFIER)
								  .type("tabulated")
								  .description("Test data set")
								  .columnDefinitions("[{\"name\":\"title\",\"type\":\"string\"},{\"name\":\"year\",\"type\":\"integer\"}]")
								  .createSql("CREATE TABLE website_data__test_data (id BIGSERIAL PRIMARY KEY, title VARCHAR(255), year INTEGER)")
								  .fetchAllSql("SELECT * FROM website_data__test_data")
								  .fetchSubsetSql("SELECT * FROM website_data__test_data WHERE year = :year")
								  .generated(Instant.now())
								  .createdAt(Instant.now())
								  .updatedAt(Instant.now())
								  .build();
	}
}
