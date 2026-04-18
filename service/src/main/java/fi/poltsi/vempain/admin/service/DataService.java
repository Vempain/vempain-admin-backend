package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.request.DataRequest;
import fi.poltsi.vempain.admin.api.response.DataResponse;
import fi.poltsi.vempain.admin.api.response.DataSummaryResponse;
import fi.poltsi.vempain.admin.entity.DataEntity;
import fi.poltsi.vempain.admin.repository.DataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DataService {
	private static final String  IDENTIFIER_REGEX   = "^[a-z][a-z0-9_]*$";
	private static final Pattern IDENTIFIER_PATTERN = Pattern.compile(IDENTIFIER_REGEX);
	private static final String  TABLE_PREFIX       = "website_data__";
	private static final String  COLUMN_NAME_REGEX  = "^[a-zA-Z_][a-zA-Z0-9_]*$";

	private final DataRepository dataRepository;
	private final JdbcTemplate   siteJdbcTemplate;

	public DataService(DataRepository dataRepository, @Qualifier("siteDataSource") DataSource siteDataSource) {
		this.dataRepository = dataRepository;
		this.siteJdbcTemplate = new JdbcTemplate(siteDataSource);
	}

	public List<DataSummaryResponse> findAll() {
		List<DataSummaryResponse> summaries = new ArrayList<>();

		for (DataEntity entity : dataRepository.findAll()) {
			summaries.add(entity.toDataSummaryResponse());
		}

		return summaries;
	}

	public DataResponse findByIdentifier(String identifier) {
		var optional = dataRepository.findByIdentifier(identifier);

		if (optional.isEmpty()) {
			log.error("Data set not found for identifier: {}", identifier);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, VempainMessages.OBJECT_NOT_FOUND);
		}

		return optional.get().toDataResponse();
	}

	@Transactional
	public DataResponse create(DataRequest request) {
		validateRequest(request);

		if (dataRepository.existsByIdentifier(request.getIdentifier())) {
			log.error("Data set with identifier '{}' already exists", request.getIdentifier());
			throw new ResponseStatusException(HttpStatus.CONFLICT, VempainMessages.OBJECT_NAME_ALREADY_EXISTS);
		}

		var now = Instant.now();
		var entity = DataEntity.builder()
							   .identifier(request.getIdentifier())
							   .type(request.getType())
							   .description(request.getDescription())
							   .columnDefinitions(request.getColumnDefinitions())
							   .createSql(request.getCreateSql())
							   .fetchAllSql(request.getFetchAllSql())
							   .fetchSubsetSql(request.getFetchSubsetSql())
							   .generated(request.getGenerated() != null ? request.getGenerated() : now)
							   .csvData(request.getCsvData())
							   .createdAt(now)
							   .updatedAt(now)
							   .build();

		var saved = dataRepository.save(entity);
		log.info("Created data set with identifier '{}'", saved.getIdentifier());
		return saved.toDataResponse();
	}

	@Transactional
	public DataResponse update(DataRequest request) {
		validateRequest(request);

		var optional = dataRepository.findByIdentifier(request.getIdentifier());

		if (optional.isEmpty()) {
			log.error("Data set with identifier '{}' not found for update", request.getIdentifier());
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, VempainMessages.OBJECT_NOT_FOUND);
		}

		var entity = optional.get();
		entity.setType(request.getType());
		entity.setDescription(request.getDescription());
		entity.setColumnDefinitions(request.getColumnDefinitions());
		entity.setCreateSql(request.getCreateSql());
		entity.setFetchAllSql(request.getFetchAllSql());
		entity.setFetchSubsetSql(request.getFetchSubsetSql());
		entity.setGenerated(request.getGenerated() != null ? request.getGenerated() : entity.getGenerated());
		entity.setCsvData(request.getCsvData());
		entity.setUpdatedAt(Instant.now());

		var saved = dataRepository.save(entity);
		log.info("Updated data set with identifier '{}'", saved.getIdentifier());
		return saved.toDataResponse();
	}

	@Transactional
	public DataResponse publish(String identifier) {
		var optional = dataRepository.findByIdentifier(identifier);

		if (optional.isEmpty()) {
			log.error("Data set with identifier '{}' not found for publishing", identifier);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, VempainMessages.OBJECT_NOT_FOUND);
		}

		var entity = optional.get();
		validateCreateSql(entity.getCreateSql());

		// The identifier has already been validated at creation/update time using IDENTIFIER_PATTERN,
		// so it only contains [a-z][a-z0-9_]* characters. The table name is double-quoted for safety.
		var safeIdentifier = entity.getIdentifier();
		var quotedTableName = "\"" + TABLE_PREFIX + safeIdentifier + "\"";

		try {
			siteJdbcTemplate.execute("DROP TABLE IF EXISTS " + quotedTableName);
			log.info("Dropped table '{}' if it existed", quotedTableName);

			siteJdbcTemplate.execute(entity.getCreateSql());
			log.info("Created table '{}' in site database", quotedTableName);

			importCsvData(quotedTableName, entity.getCsvData());
			log.info("Imported CSV data into table '{}'", quotedTableName);
		} catch (ResponseStatusException e) {
			throw e;
		} catch (Exception e) {
			log.error("Failed to publish data set '{}' to site database", identifier, e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, VempainMessages.INTERNAL_ERROR);
		}

		return entity.toDataResponse();
	}

	private void validateCreateSql(String createSql) {
		if (createSql == null) {
			return;
		}

		var trimmed = createSql.trim().toUpperCase();

		if (!trimmed.startsWith("CREATE TABLE")) {
			log.error("create_sql does not start with CREATE TABLE: {}", createSql);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "create_sql must be a CREATE TABLE statement");
		}
	}

	private void importCsvData(String tableName, String csvData) {
		if (csvData == null || csvData.isBlank()) {
			log.warn("CSV data is empty for table '{}', nothing to import", tableName);
			return;
		}

		var lines = csvData.split("\n");

		if (lines.length < 2) {
			log.warn("CSV data has no data rows for table '{}', only headers or empty", tableName);
			return;
		}

		var headers = parseCsvRow(lines[0]);

		for (String header : headers) {
			if (!header.matches(COLUMN_NAME_REGEX)) {
				log.error("CSV header '{}' contains invalid characters", header);
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid column name in CSV header: " + header);
			}
		}

		var columnList = String.join(", ", headers.stream().map(h -> "\"" + h + "\"").toList());
		var placeholders = "?, ".repeat(headers.size());
		placeholders = placeholders.substring(0, placeholders.length() - 2);
		var insertSql = "INSERT INTO " + tableName + " (" + columnList + ") VALUES (" + placeholders + ")";

		for (int i = 1; i < lines.length; i++) {
			var line = lines[i];

			if (line.isBlank()) {
				continue;
			}

			var values = parseCsvRow(line);

			if (values.size() != headers.size()) {
				log.warn("CSV row {} has {} columns but expected {}; skipping", i, values.size(), headers.size());
				continue;
			}

			siteJdbcTemplate.update(insertSql, values.toArray());
		}
	}

	private List<String> parseCsvRow(String row) {
		var result = new ArrayList<String>();
		var inQuotes = false;
		var current = new StringBuilder();

		for (int i = 0; i < row.length(); i++) {
			var c = row.charAt(i);

			if (c == '"') {
				if (inQuotes && i + 1 < row.length() && row.charAt(i + 1) == '"') {
					current.append('"');
					i++;
				} else {
					inQuotes = !inQuotes;
				}
			} else if (c == ',' && !inQuotes) {
				result.add(current.toString().trim());
				current.setLength(0);
			} else {
				current.append(c);
			}
		}

		result.add(current.toString().trim());
		return result;
	}

	private void validateRequest(DataRequest request) {
		if (request == null) {
			log.error("Data request is null");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_OBJECT_IN_REQUEST);
		}

		if (request.getIdentifier() == null || request.getIdentifier().isBlank()
			|| !IDENTIFIER_PATTERN.matcher(request.getIdentifier()).matches()) {
			log.error("Invalid identifier in data request: '{}'", request.getIdentifier());
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
											  "Identifier must start with a letter and contain only lowercase letters, numbers, and underscores");
		}

		if (request.getType() == null || request.getType().isBlank()) {
			log.error("Missing type in data request");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_OBJECT_IN_REQUEST);
		}

		if (request.getColumnDefinitions() == null || request.getColumnDefinitions().isBlank()) {
			log.error("Missing column definitions in data request");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_OBJECT_IN_REQUEST);
		}

		if (request.getCreateSql() == null || request.getCreateSql().isBlank()) {
			log.error("Missing create SQL in data request");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_OBJECT_IN_REQUEST);
		}

		if (request.getFetchAllSql() == null || request.getFetchAllSql().isBlank()) {
			log.error("Missing fetch-all SQL in data request");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_OBJECT_IN_REQUEST);
		}

		if (request.getFetchSubsetSql() == null || request.getFetchSubsetSql().isBlank()) {
			log.error("Missing fetch-subset SQL in data request");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_OBJECT_IN_REQUEST);
		}

		if (request.getCsvData() == null || request.getCsvData().isBlank()) {
			log.error("Missing CSV data in data request");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_OBJECT_IN_REQUEST);
		}
	}
}
