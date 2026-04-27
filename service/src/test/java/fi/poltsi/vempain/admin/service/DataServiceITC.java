package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.api.request.DataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataServiceITC extends AbstractITCTest {
	@Autowired
	private DataService dataService;
	@Autowired
	@Qualifier("siteDataSource")
	private DataSource siteDataSource;

	private JdbcTemplate siteJdbcTemplate;

	@BeforeEach
	void initJdbcTemplate() {
		siteJdbcTemplate = new JdbcTemplate(siteDataSource);
	}

	@Test
	void publishCreatesAndPopulatesTargetTableOk() {
		var request = DataRequest.builder()
								 .identifier("music_data")
								 .type("tabulated")
								 .description("Music data set")
								 .columnDefinitions("[{\"name\":\"title\",\"type\":\"string\"},{\"name\":\"artist\",\"type\":\"string\"}]")
								 .createSql("CREATE TABLE wrong_table_name (title TEXT, artist TEXT)")
								 .fetchAllSql("SELECT title, artist FROM website_data__music_data")
								 .fetchSubsetSql("SELECT title, artist FROM website_data__music_data WHERE artist = :artist")
								 .csvData("title,artist\nAbbey Road,The Beatles\nOK Computer,Radiohead")
								 .build();

		dataService.create(request);
		dataService.publish("music_data");

		var tableExists = siteJdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'website_data__music_data'",
				Integer.class);
		assertEquals(1, tableExists);

		var wrongTableExists = siteJdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'wrong_table_name'",
				Integer.class);
		assertEquals(0, wrongTableExists);

		var rows = siteJdbcTemplate.queryForObject("SELECT COUNT(*) FROM \"website_data__music_data\"", Integer.class);
		assertEquals(2, rows);
	}

	@Test
	void republishReplacesSiteTableContentsOk() {
		var createRequest = DataRequest.builder()
									   .identifier("gps_tracks")
									   .type("time_series")
									   .description("GPS time series")
									   .columnDefinitions("[{\"name\":\"device\",\"type\":\"string\"},{\"name\":\"timestamp\",\"type\":\"string\"}]")
									   .createSql("CREATE TABLE any_table_name (device TEXT, timestamp TEXT)")
									   .fetchAllSql("SELECT device, timestamp FROM website_data__gps_tracks")
									   .fetchSubsetSql("SELECT device, timestamp FROM website_data__gps_tracks WHERE device = :device")
									   .csvData("device,timestamp\ntracker-1,2026-01-01T10:00:00Z\ntracker-2,2026-01-01T10:05:00Z")
									   .build();

		dataService.create(createRequest);
		dataService.publish("gps_tracks");

		var initialRows = siteJdbcTemplate.queryForObject("SELECT COUNT(*) FROM \"website_data__gps_tracks\"", Integer.class);
		assertEquals(2, initialRows);

		var updateRequest = DataRequest.builder()
									   .identifier("gps_tracks")
									   .type("time_series")
									   .description("GPS time series updated")
									   .columnDefinitions("[{\"name\":\"device\",\"type\":\"string\"},{\"name\":\"timestamp\",\"type\":\"string\"}]")
									   .createSql("CREATE TABLE temporary_name (device TEXT, timestamp TEXT)")
									   .fetchAllSql("SELECT device, timestamp FROM website_data__gps_tracks")
									   .fetchSubsetSql("SELECT device, timestamp FROM website_data__gps_tracks WHERE device = :device")
									   .csvData("device,timestamp\ntracker-3,2026-01-01T10:10:00Z")
									   .build();

		dataService.update(updateRequest);
		dataService.publish("gps_tracks");

		var rowsAfterRepublish = siteJdbcTemplate.queryForObject("SELECT COUNT(*) FROM \"website_data__gps_tracks\"", Integer.class);
		assertEquals(1, rowsAfterRepublish);

		var importedDevices = siteJdbcTemplate.queryForList("SELECT device FROM \"website_data__gps_tracks\"", String.class);
		assertEquals(1, importedDevices.size());
		assertFalse(importedDevices.contains("tracker-1"));
		assertEquals("tracker-3", importedDevices.get(0));
	}

	@Test
	void publishCsvIntegerColumnsImportsAsNumericTypesOk() {
		var request = DataRequest.builder()
								 .identifier("music_library")
								 .type("tabulated")
								 .description("Music library")
								 .columnDefinitions("[{\"name\":\"artist\",\"type\":\"string\"},{\"name\":\"year\",\"type\":\"integer\"},{\"name\":\"duration_seconds\",\"type\":\"integer\"}]")
								 .createSql("CREATE TABLE any_name (artist TEXT, year INTEGER, duration_seconds INTEGER)")
								 .fetchAllSql("SELECT artist, year, duration_seconds FROM website_data__music_library")
								 .fetchSubsetSql("SELECT artist, year, duration_seconds FROM website_data__music_library WHERE artist = :artist")
								 .csvData("artist,year,duration_seconds\nMiles Davis,1959,328\nNirvana,1991,301")
								 .build();

		dataService.create(request);
		dataService.publish("music_library");

		var imported = siteJdbcTemplate.queryForList(
				"SELECT artist, year, duration_seconds FROM \"website_data__music_library\" ORDER BY year ASC");

		assertEquals(2, imported.size());
		assertEquals("Miles Davis", imported.get(0).get("artist"));
		assertEquals(1959, imported.get(0).get("year"));
		assertEquals(328, imported.get(0).get("duration_seconds"));
		assertEquals("Nirvana", imported.get(1).get("artist"));
		assertEquals(1991, imported.get(1).get("year"));
	}

	@Test
	void publishCsvInvalidIntegerValueReturnsBadRequest() {
		var request = DataRequest.builder()
								 .identifier("broken_numeric_data")
								 .type("tabulated")
								 .description("Invalid numeric CSV")
								 .columnDefinitions("[{\"name\":\"artist\",\"type\":\"string\"},{\"name\":\"year\",\"type\":\"integer\"}]")
								 .createSql("CREATE TABLE temp_name (artist TEXT, year INTEGER)")
								 .fetchAllSql("SELECT artist, year FROM website_data__broken_numeric_data")
								 .fetchSubsetSql("SELECT artist, year FROM website_data__broken_numeric_data WHERE artist = :artist")
								 .csvData("artist,year\nBad Data,not_a_year")
								 .build();

		dataService.create(request);

		var exception = assertThrows(ResponseStatusException.class, () -> dataService.publish("broken_numeric_data"));
		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
	}

	@Test
	void publishCsvTimestampColumnImportsIsoUtcValuesOk() {
		var request = DataRequest.builder()
								 .identifier("gps_points")
								 .type("time_series")
								 .description("GPS points")
								 .columnDefinitions("[{\"name\":\"timestamp\",\"type\":\"string\"},{\"name\":\"latitude\",\"type\":\"decimal\"}]")
								 .createSql("CREATE TABLE any_name (timestamp TIMESTAMP, latitude DECIMAL(15,5))")
								 .fetchAllSql("SELECT timestamp, latitude FROM website_data__gps_points")
								 .fetchSubsetSql("SELECT timestamp, latitude FROM website_data__gps_points WHERE timestamp >= :from")
								 .csvData("timestamp,latitude\n2016-03-18T11:25:55Z,60.31724\n2016-03-18T11:42:19Z,60.35231")
								 .build();

		dataService.create(request);
		dataService.publish("gps_points");

		var rows = siteJdbcTemplate.queryForObject("SELECT COUNT(*) FROM \"website_data__gps_points\"", Integer.class);
		assertEquals(2, rows);

		var firstLatitude = siteJdbcTemplate.queryForObject(
				"SELECT latitude FROM \"website_data__gps_points\" ORDER BY timestamp ASC LIMIT 1",
				Double.class);
		assertEquals(60.31724, firstLatitude, 0.00001);
	}

	@Test
	void publishCsvInvalidTimestampValueReturnsBadRequest() {
		var request = DataRequest.builder()
								 .identifier("broken_timestamp_data")
								 .type("time_series")
								 .description("Invalid timestamp CSV")
								 .columnDefinitions("[{\"name\":\"timestamp\",\"type\":\"string\"},{\"name\":\"latitude\",\"type\":\"decimal\"}]")
								 .createSql("CREATE TABLE any_name (timestamp TIMESTAMP, latitude DECIMAL(15,5))")
								 .fetchAllSql("SELECT timestamp, latitude FROM website_data__broken_timestamp_data")
								 .fetchSubsetSql("SELECT timestamp, latitude FROM website_data__broken_timestamp_data WHERE timestamp >= :from")
								 .csvData("timestamp,latitude\nnot_a_timestamp,60.31724")
								 .build();

		dataService.create(request);

		var exception = assertThrows(ResponseStatusException.class, () -> dataService.publish("broken_timestamp_data"));
		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
	}

	@Test
	void findAllGpsTimeSeriesBySearchReturnsLegacyAndPrefixedIdentifiersOk() {
		dataService.create(DataRequest.builder()
								  .identifier("matkailu_etiopia_2016")
								  .type("time_series")
								  .description("GPS time-series for Ethiopia")
								  .columnDefinitions("[{\"name\":\"timestamp\",\"type\":\"string\"}]")
								  .createSql("CREATE TABLE tmp_legacy (timestamp TIMESTAMP)")
								  .fetchAllSql("SELECT timestamp FROM website_data__matkailu_etiopia_2016")
								  .fetchSubsetSql("SELECT timestamp FROM website_data__matkailu_etiopia_2016 WHERE timestamp >= :from")
								  .csvData("timestamp\n2016-03-18T11:25:55Z")
								  .build());

		dataService.create(DataRequest.builder()
								  .identifier("gps_timeseries_legacy_track")
								  .type("time_series")
								  .description("GPS legacy route")
								  .columnDefinitions("[{\"name\":\"timestamp\",\"type\":\"string\"}]")
								  .createSql("CREATE TABLE tmp_prefixed (timestamp TIMESTAMP)")
								  .fetchAllSql("SELECT timestamp FROM website_data__gps_timeseries_legacy_track")
								  .fetchSubsetSql("SELECT timestamp FROM website_data__gps_timeseries_legacy_track WHERE timestamp >= :from")
								  .csvData("timestamp\n2016-03-18T11:42:19Z")
								  .build());

		dataService.create(DataRequest.builder()
								  .identifier("weather_series")
								  .type("time_series")
								  .description("Weather observations")
								  .columnDefinitions("[{\"name\":\"timestamp\",\"type\":\"string\"}]")
								  .createSql("CREATE TABLE tmp_weather (timestamp TIMESTAMP)")
								  .fetchAllSql("SELECT timestamp FROM website_data__weather_series")
								  .fetchSubsetSql("SELECT timestamp FROM website_data__weather_series WHERE timestamp >= :from")
								  .csvData("timestamp\n2016-03-18T11:55:00Z")
								  .build());

		var results = dataService.findAll("time_series", null, "gps");

		assertEquals(2, results.size());
		assertEquals(List.of("gps_timeseries_legacy_track", "matkailu_etiopia_2016"),
					 results.stream().map(result -> result.getIdentifier()).sorted().toList());
	}
}

