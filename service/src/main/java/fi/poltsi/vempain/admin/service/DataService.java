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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

	public List<DataSummaryResponse> findAll(String type, String identifierPrefix, String search) {
		List<DataSummaryResponse> summaries = new ArrayList<>();
		var normalizedType = normalizeFilter(type);
		var normalizedIdentifierPrefix = normalizeFilter(identifierPrefix);
		var normalizedSearch = normalizeFilter(search);

		for (DataEntity entity : dataRepository.findAll()) {
			if (!matchesFilters(entity, normalizedType, normalizedIdentifierPrefix, normalizedSearch)) {
				continue;
			}
			summaries.add(entity.toDataSummaryResponse());
		}

		return summaries;
	}

	private boolean matchesFilters(DataEntity entity, String normalizedType, String normalizedIdentifierPrefix, String normalizedSearch) {
		if (normalizedType != null && !normalizeFilter(entity.getType()).equals(normalizedType)) {
			return false;
		}

		if (normalizedIdentifierPrefix != null && !normalizeFilter(entity.getIdentifier()).startsWith(normalizedIdentifierPrefix)) {
			return false;
		}

		if (normalizedSearch == null) {
			return true;
		}

		var haystack = String.join(" ",
				normalizeFilter(entity.getIdentifier()),
				normalizeFilter(entity.getType()),
				normalizeFilter(entity.getDescription()) == null ? "" : normalizeFilter(entity.getDescription())
		);
		return haystack.contains(normalizedSearch);
	}

	private String normalizeFilter(String value) {
		if (value == null) {
			return null;
		}

		var normalized = value.trim().toLowerCase();
		return normalized.isEmpty() ? null : normalized;
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
		var createSql = buildCreateSqlForTable(entity.getCreateSql(), quotedTableName);

		try {
			siteJdbcTemplate.execute("DROP TABLE IF EXISTS " + quotedTableName);
			log.info("Dropped table '{}' if it existed", quotedTableName);

			siteJdbcTemplate.execute(createSql);
			log.info("Created table '{}' in site database", quotedTableName);

			var importedRows = importCsvData(quotedTableName, entity.getCsvData());
			log.info("Imported {} CSV rows into table '{}'", importedRows, quotedTableName);
		} catch (ResponseStatusException e) {
			throw e;
		} catch (Exception e) {
			log.error("Failed to publish data set '{}' to site database", identifier, e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, VempainMessages.INTERNAL_ERROR);
		}

		return entity.toDataResponse();
	}

	private String buildCreateSqlForTable(String createSql, String quotedTableName) {
		validateCreateSql(createSql);

		var openParenthesisIndex = createSql.indexOf('(');
		if (openParenthesisIndex < 0) {
			log.error("create_sql does not contain table definition parenthesis: {}", createSql);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "create_sql must define table columns");
		}

		var columnDefinitionSql = createSql.substring(openParenthesisIndex).trim();
		return "CREATE TABLE " + quotedTableName + " " + columnDefinitionSql;
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

	private int importCsvData(String tableName, String csvData) {
		if (csvData == null || csvData.isBlank()) {
			log.warn("CSV data is empty for table '{}', nothing to import", tableName);
			return 0;
		}

		var lines = csvData.split("\\R");

		if (lines.length < 2) {
			log.warn("CSV data has no data rows for table '{}', only headers or empty", tableName);
			return 0;
		}

		var headers = parseCsvRow(lines[0]);
		if (headers.isEmpty()) {
			log.error("CSV header row is empty for table '{}'", tableName);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV header row is missing");
		}

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
		var columnSqlTypes = resolveColumnSqlTypes(tableName, headers);
		var importedRows = 0;

		for (int i = 1; i < lines.length; i++) {
			var line = lines[i];

			if (line.isBlank()) {
				continue;
			}

			var values = parseCsvRow(line);

			if (values.size() != headers.size()) {
				log.error("CSV row {} has {} columns but expected {}", i, values.size(), headers.size());
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"CSV row " + i + " has " + values.size() + " columns but expected " + headers.size());
			}

			var rowNumber = i;
			siteJdbcTemplate.update(insertSql, ps -> {
				for (int columnIndex = 0; columnIndex < values.size(); columnIndex++) {
					var sqlType = columnSqlTypes.get(columnIndex);
					var typedValue = coerceCsvValue(values.get(columnIndex), sqlType, headers.get(columnIndex), rowNumber);

					if (typedValue == null) {
						ps.setNull(columnIndex + 1, sqlType);
					} else if (typedValue instanceof String) {
						// Keep textual values untyped to avoid over-constraining DB casts for text/date-like columns.
						ps.setObject(columnIndex + 1, typedValue);
					} else {
						ps.setObject(columnIndex + 1, typedValue, sqlType);
					}
				}
			});
			importedRows++;
		}

		return importedRows;
	}

	private List<Integer> resolveColumnSqlTypes(String tableName, List<String> headers) {
		Map<String, Integer> columnTypeByName = siteJdbcTemplate.query("SELECT * FROM " + tableName + " WHERE 1 = 0", rs -> {
			var metadata = rs.getMetaData();
			var types = new HashMap<String, Integer>();

			for (int i = 1; i <= metadata.getColumnCount(); i++) {
				types.put(metadata.getColumnName(i).toLowerCase(Locale.ROOT), metadata.getColumnType(i));
			}

			return types;
		});

		if (columnTypeByName == null || columnTypeByName.isEmpty()) {
			log.warn("Could not resolve SQL column types for table '{}', defaulting CSV binding to VARCHAR", tableName);
			columnTypeByName = Map.of();
		}

		var sqlTypes = new ArrayList<Integer>(headers.size());
		for (String header : headers) {
			sqlTypes.add(columnTypeByName.getOrDefault(header.toLowerCase(Locale.ROOT), java.sql.Types.VARCHAR));
		}

		return sqlTypes;
	}

	private Object coerceCsvValue(String rawValue, int sqlType, String columnName, int rowNumber) {
		if (rawValue == null || rawValue.isBlank()) {
			return null;
		}

		var value = rawValue.trim();

		try {
			return switch (sqlType) {
				case java.sql.Types.TINYINT, java.sql.Types.SMALLINT -> Short.valueOf(value);
				case java.sql.Types.INTEGER -> Integer.valueOf(value);
				case java.sql.Types.BIGINT -> Long.valueOf(value);
				case java.sql.Types.REAL -> Float.valueOf(value);
				case java.sql.Types.FLOAT, java.sql.Types.DOUBLE -> Double.valueOf(value);
				case java.sql.Types.NUMERIC, java.sql.Types.DECIMAL -> new BigDecimal(value);
				case java.sql.Types.BOOLEAN, java.sql.Types.BIT -> parseBoolean(value, columnName, rowNumber);
				default -> value;
			};
		} catch (NumberFormatException e) {
			log.error("Invalid numeric value '{}' for column '{}' at CSV row {}", rawValue, columnName, rowNumber);
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Invalid value '" + rawValue + "' for column '" + columnName + "' at CSV row " + rowNumber);
		}
	}

	private Boolean parseBoolean(String value, String columnName, int rowNumber) {
		var normalized = value.toLowerCase(Locale.ROOT);

		if (normalized.equals("true") || normalized.equals("1") || normalized.equals("yes") || normalized.equals("y")) {
			return true;
		}

		if (normalized.equals("false") || normalized.equals("0") || normalized.equals("no") || normalized.equals("n")) {
			return false;
		}

		log.error("Invalid boolean value '{}' for column '{}' at CSV row {}", value, columnName, rowNumber);
		throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Invalid boolean value '" + value + "' for column '" + columnName + "' at CSV row " + rowNumber);
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
