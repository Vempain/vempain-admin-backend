package fi.poltsi.vempain.admin.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.TaskTypeEnum;
import fi.poltsi.vempain.admin.api.request.DataPagedRequest;
import fi.poltsi.vempain.admin.api.request.DataRequest;
import fi.poltsi.vempain.admin.api.response.DataResponse;
import fi.poltsi.vempain.admin.api.response.DataSetAclResponse;
import fi.poltsi.vempain.admin.api.response.DataSummaryResponse;
import fi.poltsi.vempain.admin.entity.DataEntity;
import fi.poltsi.vempain.admin.repository.DataRepository;
import fi.poltsi.vempain.auth.api.response.PagedResponse;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.service.AclService;
import fi.poltsi.vempain.common.api.response.TaskAcceptedResponse;
import fi.poltsi.vempain.common.task.TaskProgress;
import fi.poltsi.vempain.common.task.TaskRunner;
import fi.poltsi.vempain.tools.ReleaseState;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DataService {

    private static final String IDENTIFIER_REGEX = "^[a-z][a-z0-9_]*$";
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile(IDENTIFIER_REGEX);
    private static final String TABLE_PREFIX = "website_data__";
    private static final String COLUMN_NAME_REGEX = "^[a-zA-Z_][a-zA-Z0-9_]*$";

    private final DataRepository dataRepository;
    private final JdbcTemplate siteJdbcTemplate;
    private final TaskRunner taskRunner;
    private final ApplicationContext applicationContext;
    private final AccessService accessService;
    private final AclService aclService;
    private final AclGranteeService aclGranteeService;

    public DataService(DataRepository dataRepository, @Qualifier("siteDataSource") DataSource siteDataSource, TaskRunner taskRunner,
            ApplicationContext applicationContext, AccessService accessService, AclService aclService, AclGranteeService aclGranteeService) {
        this.dataRepository = dataRepository;
        this.siteJdbcTemplate = new JdbcTemplate(siteDataSource);
        this.taskRunner = taskRunner;
        this.applicationContext = applicationContext;
        this.accessService = accessService;
        this.aclService = aclService;
        this.aclGranteeService = aclGranteeService;
    }

    /**
     * Starts a background task that publishes the data set to the site database. The data set must exist and the caller needs the modify privilege on it; the
     * publish itself runs through the transactional proxy on a worker thread and the finished task carries the {@link DataResponse}.
     */
    public TaskAcceptedResponse publishAsTask(String identifier) {
        requireModify(findEntity(identifier));

        var progress = taskRunner.submitDurable(TaskTypeEnum.PUBLISH_DATA_SET.name(), "Publish data set " + identifier, 1, Map.of("identifier", identifier),
                task -> publishNow(identifier, task));
        return progress.toAcceptedResponse();
    }

    /**
     * Task body of {@link TaskTypeEnum#PUBLISH_DATA_SET}.
     */
    public DataResponse publishNow(String identifier, TaskProgress progress) {
        progress.checkpoint();
        var response = applicationContext.getBean(DataService.class)
            .publish(identifier);
        progress.advance("Published data set " + identifier);
        return response;
    }

    public List<DataSummaryResponse> findAll(String type, String identifierPrefix, String search) {
        List<DataSummaryResponse> summaries = new ArrayList<>();
        var normalizedType = normalizeFilter(type);
        var normalizedIdentifierPrefix = normalizeFilter(identifierPrefix);
        var normalizedSearch = normalizeFilter(search);

        // Only the data sets the caller may read are listed, like every other ACL-bearing resource
        for (DataEntity entity : dataRepository.findAll()) {
            if (!matchesFilters(entity, normalizedType, normalizedIdentifierPrefix, normalizedSearch)
                || !accessService.hasReadPermission(entity.getAclId())) {
                continue;
            }
            summaries.add(withAcls(entity).toDataSummaryResponse());
        }

        return summaries;
    }

    /**
     * The readable data sets as a page, for the data set table of the admin SPA: filtered by the optional type, the modified-after-publish rule
     * ({@link ReleaseState}) and the search text (over identifier, type and description, or the one {@code filter_column}), sorted by {@code sort_by}
     * (identifier by default) and cut to the requested page.
     */
    public PagedResponse<DataSummaryResponse> findPaged(DataPagedRequest request) {
        var normalizedType = normalizeFilter(request.getType());
        var onlyModifiedAfterPublish = Boolean.TRUE.equals(request.getModifiedAfterPublish());
        var caseSensitive = Boolean.TRUE.equals(request.getCaseSensitive());
        var search = request.getSearch() == null
                     || request.getSearch()
                         .isBlank() ? null
                                 : (caseSensitive ? request.getSearch()
                                     .trim() : normalizeFilter(request.getSearch()));
        var filterColumn = normalizeFilter(request.getFilterColumn());

        var entities = new ArrayList<DataEntity>();
        for (DataEntity entity : dataRepository.findAll()) {
            if (!accessService.hasReadPermission(entity.getAclId())
                || (normalizedType != null
                    && !normalizedType.equals(normalizeFilter(entity.getType())))
                || (onlyModifiedAfterPublish
                    && !ReleaseState.isModifiedAfterPublish(entity.getPublished(), entity.getModified()))
                || (search != null
                    && !matchesSearch(entity, search, filterColumn, caseSensitive))) {
                continue;
            }
            entities.add(entity);
        }

        var comparator = dataSetComparator(request.getSortBy());
        if (request.getDirection() == org.springframework.data.domain.Sort.Direction.DESC) {
            comparator = comparator.reversed();
        }
        entities.sort(comparator);

        var summaries = entities.stream()
            .map(entity -> withAcls(entity).toDataSummaryResponse())
            .toList();
        return PagedResponseService.create(summaries, request);
    }

    /** The search text against identifier, type and description, or against the one column the request names */
    private static boolean matchesSearch(DataEntity entity, String search, String filterColumn, boolean caseSensitive) {
        var candidates = switch (filterColumn == null ? "" : filterColumn) {
            case "identifier" -> List.of(entity.getIdentifier());
            case "type" -> List.of(entity.getType());
            case "description" -> java.util.Collections.singletonList(entity.getDescription());
            default -> java.util.Arrays.asList(entity.getIdentifier(), entity.getType(), entity.getDescription());
        };
        for (var candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            var value = caseSensitive ? candidate : candidate.toLowerCase();
            if (value.contains(search)) {
                return true;
            }
        }
        return false;
    }

    private static Comparator<DataEntity> dataSetComparator(String sortBy) {
        Comparator<Instant> instants = Comparator.nullsFirst(Comparator.naturalOrder());
        Comparator<Long> longs = Comparator.nullsFirst(Comparator.naturalOrder());
        return switch (sortBy == null ? "identifier" : sortBy) {
            case "id" -> Comparator.comparing(DataEntity::getId, longs);
            case "acl_id" -> Comparator.comparingLong(DataEntity::getAclId);
            case "type" -> Comparator.comparing(DataEntity::getType, String.CASE_INSENSITIVE_ORDER);
            case "description" -> Comparator.comparing(DataEntity::getDescription, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER));
            case "generated" -> Comparator.comparing(DataEntity::getGenerated, instants);
            case "published" -> Comparator.comparing(DataEntity::getPublished, instants);
            case "created" -> Comparator.comparing(DataEntity::getCreated, instants);
            case "modified" -> Comparator.comparing(DataEntity::getModified, instants);
            case "creator" -> Comparator.comparing(DataEntity::getCreator, longs);
            case "modifier" -> Comparator.comparing(DataEntity::getModifier, longs);
            default -> Comparator.comparing(DataEntity::getIdentifier, String.CASE_INSENSITIVE_ORDER);
        };
    }

    /** Every data set, unfiltered, for the ACL consistency repair */
    public Iterable<DataEntity> findAllEntities() {
        return dataRepository.findAll();
    }

    /** Stores a repaired data set (the ACL consistency schedule) */
    public DataEntity save(DataEntity entity) {
        return dataRepository.save(entity);
    }

    private boolean matchesFilters(DataEntity entity, String normalizedType, String normalizedIdentifierPrefix, String normalizedSearch) {
        if (normalizedType != null
            && !normalizeFilter(entity.getType()).equals(normalizedType)) {
            return false;
        }

        if (normalizedIdentifierPrefix != null
            && !normalizeFilter(entity.getIdentifier()).startsWith(normalizedIdentifierPrefix)) {
            return false;
        }

        if (normalizedSearch == null) {
            return true;
        }

        var haystack = String.join(" ", normalizeFilter(entity.getIdentifier()), normalizeFilter(entity.getType()),
                normalizeFilter(entity.getDescription()) == null ? "" : normalizeFilter(entity.getDescription()));
        return haystack.contains(normalizedSearch);
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }

        var normalized = value.trim()
            .toLowerCase();
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * The data set with its ACL rows; the caller needs the read privilege.
     *
     * @throws ResponseStatusException 404 when the data set does not exist, 403 when the caller may not read it
     */
    public DataResponse findByIdentifier(String identifier) {
        var entity = findEntity(identifier);
        requireRead(entity);
        return withAcls(entity).toDataResponse();
    }

    /**
     * The additional grantees of a data set in request shape (the creating account's own row is left out), for the service that created the set to show and
     * resend; the caller needs the read privilege.
     */
    public DataSetAclResponse getAcls(String identifier) {
        var entity = findEntity(identifier);
        requireRead(entity);
        return DataSetAclResponse.builder()
            .identifier(entity.getIdentifier())
            .aclId(entity.getAclId())
            .acls(aclGranteeService.additionalGrantees(entity.getAclId(), accessService.getValidUserId()))
            .build();
    }

    /**
     * Creates a data set owned by the caller: a new ACL grants the caller every privilege and the request's additional grantees are added next to it.
     *
     * @throws ResponseStatusException 400 for an invalid request or grantee list, 409 when the identifier exists
     */
    @Transactional
    public DataResponse create(DataRequest request) {
        validateRequest(request);
        validateAcls(request.getAcls());

        if (dataRepository.existsByIdentifier(request.getIdentifier())) {
            log.error("Data set with identifier '{}' already exists", request.getIdentifier());
            throw new ResponseStatusException(HttpStatus.CONFLICT, VempainMessages.OBJECT_NAME_ALREADY_EXISTS);
        }

        var userId = accessService.getValidUserId();
        var now = Instant.now();
        long aclId;
        try {
            aclId = aclService.createNewAcl(userId, null, true, true, true, true);
        } catch (VempainAclException e) {
            log.error("Failed to create the ACL of data set '{}'", request.getIdentifier(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, VempainMessages.INTERNAL_ERROR);
        }
        var entity = DataEntity.builder()
            .aclId(aclId)
            .creator(userId)
            .created(now)
            .identifier(request.getIdentifier())
            .type(request.getType())
            .description(request.getDescription())
            .columnDefinitions(request.getColumnDefinitions())
            .createSql(request.getCreateSql())
            .fetchAllSql(request.getFetchAllSql())
            .fetchSubsetSql(request.getFetchSubsetSql())
            .generated(request.getGenerated() != null ? request.getGenerated() : now)
            .csvData(request.getCsvData())
            .build();

        var saved = dataRepository.save(entity);
        aclGranteeService.grant(aclId, request.getAcls(), userId);
        log.info("Created data set with identifier '{}' owned by user {} (ACL {})", saved.getIdentifier(), userId, aclId);
        return withAcls(saved).toDataResponse();
    }

    /**
     * Replaces the content of a data set; the caller needs the modify privilege. A non-null grantee list becomes the complete set of additional grantees (an
     * empty list removes them), null keeps the current ones.
     *
     * @throws ResponseStatusException 400 for an invalid request or grantee list, 403 without the modify privilege, 404 for an unknown identifier
     */
    @Transactional
    public DataResponse update(DataRequest request) {
        validateRequest(request);
        validateAcls(request.getAcls());

        var entity = findEntity(request.getIdentifier());
        requireModify(entity);

        var userId = accessService.getValidUserId();
        entity.setType(request.getType());
        entity.setDescription(request.getDescription());
        entity.setColumnDefinitions(request.getColumnDefinitions());
        entity.setCreateSql(request.getCreateSql());
        entity.setFetchAllSql(request.getFetchAllSql());
        entity.setFetchSubsetSql(request.getFetchSubsetSql());
        entity.setGenerated(request.getGenerated() != null ? request.getGenerated() : entity.getGenerated());
        entity.setCsvData(request.getCsvData());
        entity.setModifier(userId);
        entity.setModified(Instant.now());

        var saved = dataRepository.save(entity);
        aclGranteeService.replace(saved.getAclId(), request.getAcls(), entity.getCreator() != null ? entity.getCreator() : userId);
        log.info("Updated data set with identifier '{}'", saved.getIdentifier());
        return withAcls(saved).toDataResponse();
    }

    /**
     * Creates or replaces the site table of the data set and imports its CSV content. Runs as the body of the publish task and from the schedule; the
     * authorization happened when the task was submitted.
     */
    @Transactional
    public DataResponse publish(String identifier) {
        var entity = findEntity(identifier);
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

        // The site table is in place, record when the data set was last released to the website
        entity.setPublished(Instant.now());
        dataRepository.save(entity);
        return withAcls(entity).toDataResponse();
    }

    /**
     * Deletes a data set, its ACL and its published table in the site database, if any; the caller needs the delete privilege.
     */
    @Transactional
    public void delete(String identifier) {
        var entity = findEntity(identifier);
        if (!accessService.hasDeletePermission(entity.getAclId())) {
            log.warn("User {} may not delete data set '{}'", accessService.getValidUserId(), identifier);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, VempainMessages.UNAUTHORIZED_ACCESS);
        }

        var quotedTableName = "\"" + TABLE_PREFIX + entity.getIdentifier() + "\"";

        try {
            siteJdbcTemplate.execute("DROP TABLE IF EXISTS " + quotedTableName);
            log.info("Dropped table '{}' if it existed", quotedTableName);
        } catch (Exception e) {
            log.error("Failed to drop site table '{}' while deleting data set '{}'", quotedTableName, identifier, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, VempainMessages.INTERNAL_ERROR);
        }

        dataRepository.delete(entity);
        try {
            aclService.deleteByAclId(entity.getAclId());
        } catch (fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException e) {
            log.warn("ACL {} of deleted data set '{}' was already missing", entity.getAclId(), identifier);
        }
        log.info("Deleted data set with identifier '{}'", identifier);
    }

    private DataEntity findEntity(String identifier) {
        var optional = dataRepository.findByIdentifier(identifier);

        if (optional.isEmpty()) {
            log.error("Data set not found for identifier: {}", identifier);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, VempainMessages.OBJECT_NOT_FOUND);
        }

        return optional.get();
    }

    private void requireRead(DataEntity entity) {
        if (!accessService.hasReadPermission(entity.getAclId())) {
            log.warn("User {} may not read data set '{}'", accessService.getValidUserId(), entity.getIdentifier());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, VempainMessages.UNAUTHORIZED_ACCESS);
        }
    }

    private void requireModify(DataEntity entity) {
        if (!accessService.hasModifyPermission(entity.getAclId())) {
            log.warn("User {} may not modify data set '{}'", accessService.getValidUserId(), entity.getIdentifier());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, VempainMessages.UNAUTHORIZED_ACCESS);
        }
    }

    /** Attaches the ACL rows of the data set for its responses */
    private DataEntity withAcls(DataEntity entity) {
        entity.setAcls(entity.getAclId() > 0 ? aclService.findAclByAclId(entity.getAclId()) : List.of());
        return entity;
    }

    /** The additional grantees of a request must be valid before anything is stored; an invalid list is a 400 */
    private void validateAcls(List<fi.poltsi.vempain.admin.api.request.file.FileIngestAclRequest> acls) {
        try {
            aclGranteeService.validate(acls);
        } catch (IllegalArgumentException e) {
            log.error("Invalid ACL grantees in data request: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private String buildCreateSqlForTable(String createSql, String quotedTableName) {
        validateCreateSql(createSql);

        var openParenthesisIndex = createSql.indexOf('(');
        if (openParenthesisIndex < 0) {
            log.error("create_sql does not contain table definition parenthesis: {}", createSql);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "create_sql must define table columns");
        }

        var columnDefinitionSql = createSql.substring(openParenthesisIndex)
            .trim();
        return "CREATE TABLE " + quotedTableName + " " + columnDefinitionSql;
    }

    private void validateCreateSql(String createSql) {
        if (createSql == null) {
            return;
        }

        var trimmed = createSql.trim()
            .toUpperCase();

        if (!trimmed.startsWith("CREATE TABLE")) {
            log.error("create_sql does not start with CREATE TABLE: {}", createSql);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "create_sql must be a CREATE TABLE statement");
        }
    }

    private int importCsvData(String tableName, String csvData) {
        if (csvData == null
            || csvData.isBlank()) {
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

        var columnList = String.join(", ", headers.stream()
            .map(h -> "\"" + h + "\"")
            .toList());
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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV row " + i + " has " + values.size() + " columns but expected " + headers.size());
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
                types.put(metadata.getColumnName(i)
                    .toLowerCase(Locale.ROOT), metadata.getColumnType(i));
            }

            return types;
        });

        if (columnTypeByName == null
            || columnTypeByName.isEmpty()) {
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
        if (rawValue == null
            || rawValue.isBlank()) {
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
                case java.sql.Types.DATE -> parseDate(value, columnName, rowNumber);
                case java.sql.Types.TIME, java.sql.Types.TIME_WITH_TIMEZONE -> parseTime(value, columnName, rowNumber);
                case java.sql.Types.TIMESTAMP, java.sql.Types.TIMESTAMP_WITH_TIMEZONE -> parseTimestamp(value, columnName, rowNumber);
                default -> value;
            };
        } catch (NumberFormatException e) {
            log.error("Invalid numeric value '{}' for column '{}' at CSV row {}", rawValue, columnName, rowNumber);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid value '" + rawValue + "' for column '" + columnName + "' at CSV row " + rowNumber);
        }
    }

    private Date parseDate(String value, String columnName, int rowNumber) {
        try {
            return Date.valueOf(LocalDate.parse(value));
        } catch (DateTimeParseException e) {
            log.error("Invalid date value '{}' for column '{}' at CSV row {}", value, columnName, rowNumber);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid date value '" + value + "' for column '" + columnName + "' at CSV row " + rowNumber);
        }
    }

    private Time parseTime(String value, String columnName, int rowNumber) {
        try {
            return Time.valueOf(LocalTime.parse(value));
        } catch (DateTimeParseException e) {
            log.error("Invalid time value '{}' for column '{}' at CSV row {}", value, columnName, rowNumber);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid time value '" + value + "' for column '" + columnName + "' at CSV row " + rowNumber);
        }
    }

    private Timestamp parseTimestamp(String value, String columnName, int rowNumber) {
        try {
            // Accept full ISO-8601 values with zone information (e.g. 2016-03-18T11:25:55Z).
            return Timestamp.from(Instant.parse(value));
        } catch (DateTimeParseException ignored) {
            // Fall through to support local date-time format without timezone.
        }

        try {
            return Timestamp.from(OffsetDateTime.parse(value)
                .toInstant());
        } catch (DateTimeParseException ignored) {
            // Fall through to local timestamp parsing.
        }

        try {
            return Timestamp.valueOf(LocalDateTime.parse(value));
        } catch (DateTimeParseException e) {
            log.error("Invalid timestamp value '{}' for column '{}' at CSV row {}", value, columnName, rowNumber);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid timestamp value '" + value + "' for column '" + columnName + "' at CSV row " + rowNumber);
        }
    }

    private Boolean parseBoolean(String value, String columnName, int rowNumber) {
        var normalized = value.toLowerCase(Locale.ROOT);

        if (normalized.equals("true")
            || normalized.equals("1")
            || normalized.equals("yes")
            || normalized.equals("y")) {
            return true;
        }

        if (normalized.equals("false")
            || normalized.equals("0")
            || normalized.equals("no")
            || normalized.equals("n")) {
            return false;
        }

        log.error("Invalid boolean value '{}' for column '{}' at CSV row {}", value, columnName, rowNumber);
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid boolean value '" + value + "' for column '" + columnName + "' at CSV row " + rowNumber);
    }

    private List<String> parseCsvRow(String row) {
        var result = new ArrayList<String>();
        var inQuotes = false;
        var current = new StringBuilder();

        for (int i = 0; i < row.length(); i++) {
            var c = row.charAt(i);

            if (c == '"') {
                if (inQuotes
                    && i + 1 < row.length()
                    && row.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ','
                       && !inQuotes) {
                result.add(current.toString()
                    .trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        result.add(current.toString()
            .trim());
        return result;
    }

    private void validateRequest(DataRequest request) {
        if (request == null) {
            log.error("Data request is null");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_OBJECT_IN_REQUEST);
        }

        if (request.getIdentifier() == null
            || request.getIdentifier()
                .isBlank()
            || !IDENTIFIER_PATTERN.matcher(request.getIdentifier())
                .matches()) {
            log.error("Invalid identifier in data request: '{}'", request.getIdentifier());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Identifier must start with a letter and contain only lowercase letters, numbers, and underscores");
        }

        if (request.getType() == null
            || request.getType()
                .isBlank()) {
            log.error("Missing type in data request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_OBJECT_IN_REQUEST);
        }

        if (request.getColumnDefinitions() == null
            || request.getColumnDefinitions()
                .isBlank()) {
            log.error("Missing column definitions in data request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_OBJECT_IN_REQUEST);
        }

        if (request.getCreateSql() == null
            || request.getCreateSql()
                .isBlank()) {
            log.error("Missing create SQL in data request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_OBJECT_IN_REQUEST);
        }

        if (request.getFetchAllSql() == null
            || request.getFetchAllSql()
                .isBlank()) {
            log.error("Missing fetch-all SQL in data request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_OBJECT_IN_REQUEST);
        }

        if (request.getFetchSubsetSql() == null
            || request.getFetchSubsetSql()
                .isBlank()) {
            log.error("Missing fetch-subset SQL in data request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_OBJECT_IN_REQUEST);
        }

        if (request.getCsvData() == null
            || request.getCsvData()
                .isBlank()) {
            log.error("Missing CSV data in data request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_OBJECT_IN_REQUEST);
        }
    }
}
