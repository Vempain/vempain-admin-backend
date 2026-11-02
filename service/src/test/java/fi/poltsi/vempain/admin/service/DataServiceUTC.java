package fi.poltsi.vempain.admin.service;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.web.server.ResponseStatusException;

import fi.poltsi.vempain.admin.api.request.DataPagedRequest;
import fi.poltsi.vempain.admin.api.request.DataRequest;
import fi.poltsi.vempain.admin.api.request.file.FileIngestAclRequest;
import fi.poltsi.vempain.admin.api.response.DataResponse;
import fi.poltsi.vempain.admin.api.response.DataSummaryResponse;
import fi.poltsi.vempain.admin.api.response.file.FileIngestAclResponse;
import fi.poltsi.vempain.admin.entity.DataEntity;
import fi.poltsi.vempain.admin.repository.DataRepository;
import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.service.AclService;
import fi.poltsi.vempain.common.api.TaskStatusEnum;
import fi.poltsi.vempain.common.task.TaskProgressStore;
import fi.poltsi.vempain.common.task.TaskRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataServiceUTC {

    private static final String TEST_IDENTIFIER = "test_data";

    @Mock
    private DataRepository dataRepository;
    @Mock
    private AccessService accessService;
    @Mock
    private AclService aclService;
    @Mock
    private AclGranteeService aclGranteeService;

    private DataService dataService;
    private JdbcTemplate mockJdbcTemplate;

    @BeforeEach
    void setUp() throws Exception {
        var mockDataSource = mock(DataSource.class);
        dataService = new DataService(dataRepository, mockDataSource, null, null, accessService, aclService, aclGranteeService);
        // Most tests run as the administrator (user 1) holding every privilege on the ACL 10 of the test entity; the denial tests override these
        lenient().when(accessService.getValidUserId())
            .thenReturn(1L);
        lenient().when(accessService.hasReadPermission(anyLong()))
            .thenReturn(true);
        lenient().when(accessService.hasModifyPermission(anyLong()))
            .thenReturn(true);
        lenient().when(accessService.hasDeletePermission(anyLong()))
            .thenReturn(true);
        lenient().when(aclService.createNewAcl(eq(1L), isNull(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
            .thenReturn(10L);

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

        List<DataSummaryResponse> result = dataService.findAll(null, null, null);

        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void findAllEmptyOk() {
        when(dataRepository.findAll()).thenReturn(new ArrayList<>());

        List<DataSummaryResponse> result = dataService.findAll(null, null, null);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void findAllFiltersByTypeAndIdentifierPrefixOk() {
        var music = buildEntity(1L);
        music.setIdentifier("music_library");
        music.setDescription("Music library");
        var gps = buildEntity(2L);
        gps.setIdentifier("gps_timeseries_holidays");
        gps.setType("time_series");
        gps.setDescription("GPS trip track");
        when(dataRepository.findAll()).thenReturn(List.of(music, gps));

        List<DataSummaryResponse> result = dataService.findAll("time_series", "gps_timeseries_", null);

        assertEquals(1, result.size());
        assertEquals("gps_timeseries_holidays", result.get(0)
            .getIdentifier());
    }

    @Test
    void findAllFiltersBySearchOk() {
        var music = buildEntity(1L);
        music.setIdentifier("music_library");
        music.setDescription("Complete music library");
        var gps = buildEntity(2L);
        gps.setIdentifier("gps_timeseries_holidays");
        gps.setType("time_series");
        gps.setDescription("GPS trip track");
        when(dataRepository.findAll()).thenReturn(List.of(music, gps));

        List<DataSummaryResponse> result = dataService.findAll(null, null, "trip");

        assertEquals(1, result.size());
        assertEquals("gps_timeseries_holidays", result.get(0)
            .getIdentifier());
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
        verify(mockJdbcTemplate).execute("CREATE TABLE \"website_data__test_data\" (id BIGSERIAL PRIMARY KEY, title VARCHAR(255))");
        // The release time is recorded once the site table is in place
        assertNotNull(entity.getPublished());
        assertEquals(entity.getPublished(), response.getPublished());
        verify(dataRepository).save(entity);
    }

    @Test
    void publishRewritesCreateSqlTableNameOk() {
        var entity = buildEntity(1L);
        entity.setCreateSql("CREATE TABLE wrong_table_name (id BIGSERIAL PRIMARY KEY, title VARCHAR(255))");
        when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.of(entity));

        DataResponse response = dataService.publish(TEST_IDENTIFIER);

        assertNotNull(response);
        verify(mockJdbcTemplate).execute("CREATE TABLE \"website_data__test_data\" (id BIGSERIAL PRIMARY KEY, title VARCHAR(255))");
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
    void publishCsvColumnCountMismatchFail() {
        var entity = buildEntity(1L);
        entity.setCsvData("title,year\nOnlyTitle");
        when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.of(entity));

        try {
            dataService.publish(TEST_IDENTIFIER);
            fail("Should have thrown ResponseStatusException for invalid CSV row width");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        }
    }

    @Test
    void publishSiteDbFailureThrowsInternalError() {
        var entity = buildEntity(1L);
        when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.of(entity));
        doThrow(new RuntimeException("DB failure")).when(mockJdbcTemplate)
            .execute(anyString());

        try {
            dataService.publish(TEST_IDENTIFIER);
            fail("Should have thrown ResponseStatusException");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
        }

        // A failed release leaves the data set unpublished
        assertNull(entity.getPublished());
        verify(dataRepository, never()).save(any());
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
            .generated(Instant.now())
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
            .generated(now)
            .csvData("title\nTest Album")
            .aclId(10L)
            .creator(1L)
            .created(now)
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
                .generated(now)
                .csvData("value\ntest")
                .aclId(10L + i)
                .creator(1L)
                .created(now)
                .build());
        }
        return entities;
    }

    // delete

    @Test
    void deleteDropsSiteTableAndRemovesTheEntityOk() {
        var entity = buildEntity(1L);
        when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.of(entity));

        dataService.delete(TEST_IDENTIFIER);

        verify(mockJdbcTemplate).execute("DROP TABLE IF EXISTS \"website_data__" + TEST_IDENTIFIER + "\"");
        verify(dataRepository).delete(entity);
    }

    @Test
    void deleteNotFoundFail() {
        when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.empty());

        try {
            dataService.delete(TEST_IDENTIFIER);
            fail("Should have thrown ResponseStatusException");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
        verify(dataRepository, never()).delete(any());
    }

    @Test
    void deleteFailsWhenTheSiteTableCannotBeDropped() {
        var entity = buildEntity(1L);
        when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.of(entity));
        doThrow(new RuntimeException("db down")).when(mockJdbcTemplate)
            .execute(anyString());

        try {
            dataService.delete(TEST_IDENTIFIER);
            fail("Should have thrown ResponseStatusException");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
        }
        verify(dataRepository, never()).delete(any());
    }

    // ---- filters, the publish task, CSV parsing and type coercion ----

    @Test
    void findAllFiltersByIdentifierPrefixAndToleratesMissingDescriptionsAndBlankFilters() {
        var entities = buildEntityList(2);
        entities.getFirst()
            .setDescription(null);
        when(dataRepository.findAll()).thenReturn(entities);

        assertEquals(0, dataService.findAll(null, "other_", null)
            .size());
        assertEquals(2, dataService.findAll("  ", "data_set", "data")
            .size());
        assertEquals(1, dataService.findAll(null, null, "test data set 1")
            .size());
    }

    @Test
    void publishAsTaskRejectsAnUnknownDataSetAndOtherwisePublishesThroughTheProxy() {
        var applicationContext = mock(ApplicationContext.class);
        var taskStore = new TaskProgressStore();
        var taskService = new DataService(dataRepository, mock(DataSource.class), new TaskRunner(taskStore, Runnable::run), applicationContext, accessService,
                aclService, aclGranteeService);
        when(dataRepository.findByIdentifier("missing")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> taskService.publishAsTask("missing"));

        var entity = buildEntity(1L);
        when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.of(entity));
        var proxy = mock(DataService.class);
        when(proxy.publish(TEST_IDENTIFIER)).thenReturn(entity.toDataResponse());
        when(applicationContext.getBean(DataService.class)).thenReturn(proxy);

        var accepted = taskService.publishAsTask(TEST_IDENTIFIER);

        var task = taskStore.find(accepted.getTaskId())
            .orElseThrow();
        assertEquals(TaskStatusEnum.COMPLETED, task.getStatus());
        assertEquals(TEST_IDENTIFIER, ((DataResponse) task.getResult()).getIdentifier());
        verify(proxy).publish(TEST_IDENTIFIER);
    }

    @Test
    void publishRejectsACreateStatementWithoutColumnsAndSkipsEmptyCsvData() {
        var entity = buildEntity(1L);
        entity.setCreateSql("CREATE TABLE website_data__test_data");
        when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.of(entity));
        var thrown = assertThrows(ResponseStatusException.class, () -> dataService.publish(TEST_IDENTIFIER));
        assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode());

        entity.setCreateSql("CREATE TABLE website_data__test_data (id BIGSERIAL PRIMARY KEY, title VARCHAR(255))");
        entity.setCsvData("   ");
        dataService.publish(TEST_IDENTIFIER);
        entity.setCsvData("title");
        dataService.publish(TEST_IDENTIFIER);
        verify(mockJdbcTemplate, never()).update(anyString(), any(PreparedStatementSetter.class));
    }

    @Test
    void publishRejectsInvalidColumnNamesInTheCsvHeader() {
        var entity = buildEntity(1L);
        entity.setCsvData("bad-name\nvalue");
        when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.of(entity));

        var thrown = assertThrows(ResponseStatusException.class, () -> dataService.publish(TEST_IDENTIFIER));

        assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode());
    }

    @SuppressWarnings("unchecked")
    private void stubColumnTypes(Map<String, Integer> types) {
        when(mockJdbcTemplate.query(anyString(), any(ResultSetExtractor.class))).thenReturn(types);
    }

    private PreparedStatement runInsertsAgainst(PreparedStatement statement) {
        doAnswer(invocation -> {
            invocation.getArgument(1, PreparedStatementSetter.class)
                .setValues(statement);
            return 1;
        }).when(mockJdbcTemplate)
            .update(anyString(), any(PreparedStatementSetter.class));
        return statement;
    }

    @Test
    void publishCoercesCsvValuesToTheColumnTypesAndBindsNullsForBlanks() throws Exception {
        var entity = buildEntity(1L);
        entity.setCsvData("""
                small,whole,big,single,dbl,dec,flag,day,moment,stamp,text
                1,2,3,1.5,2.5,3.25,yes,2024-06-01,10:15:30,2024-06-01T10:15:30Z,"quoted ""name"", with comma"

                ,,,,,,,,,,
                7,8,9,1.5,2.5,3.25,n,2024-06-02,11:00:00,2024-06-02T11:00:00+02:00,plain
                7,8,9,1.5,2.5,3.25,0,2024-06-02,11:00:00,2024-06-02T11:00:00,plain
                """);
        when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.of(entity));
        stubColumnTypes(Map.ofEntries(Map.entry("small", Types.SMALLINT), Map.entry("whole", Types.INTEGER), Map.entry("big", Types.BIGINT),
                Map.entry("single", Types.REAL), Map.entry("dbl", Types.DOUBLE), Map.entry("dec", Types.NUMERIC), Map.entry("flag", Types.BOOLEAN),
                Map.entry("day", Types.DATE), Map.entry("moment", Types.TIME), Map.entry("stamp", Types.TIMESTAMP), Map.entry("text", Types.VARCHAR)));
        var statement = runInsertsAgainst(mock(PreparedStatement.class));

        dataService.publish(TEST_IDENTIFIER);

        verify(mockJdbcTemplate, times(4)).update(anyString(), any(PreparedStatementSetter.class));
        verify(statement).setObject(11, "quoted \"name\", with comma");
        verify(statement).setObject(1, (short) 1, Types.SMALLINT);
        verify(statement).setObject(2, 2, Types.INTEGER);
        verify(statement).setObject(3, 3L, Types.BIGINT);
        verify(statement, times(3)).setObject(4, 1.5f, Types.REAL);
        verify(statement, times(3)).setObject(5, 2.5d, Types.DOUBLE);
        verify(statement, times(3)).setObject(6, new java.math.BigDecimal("3.25"), Types.NUMERIC);
        verify(statement).setObject(7, true, Types.BOOLEAN);
        verify(statement, times(2)).setObject(7, false, Types.BOOLEAN);
        verify(statement).setObject(8, java.sql.Date.valueOf("2024-06-01"), Types.DATE);
        verify(statement).setObject(9, java.sql.Time.valueOf("10:15:30"), Types.TIME);
        verify(statement, times(3)).setObject(org.mockito.ArgumentMatchers.eq(10), any(java.sql.Timestamp.class),
                org.mockito.ArgumentMatchers.eq(Types.TIMESTAMP));
        verify(statement).setNull(1, Types.SMALLINT);
        verify(statement).setNull(11, Types.VARCHAR);
    }

    private void assertPublishBadRequest(String header, String value, int sqlType) {
        var entity = buildEntity(1L);
        entity.setCsvData(header + "\n" + value);
        when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.of(entity));
        stubColumnTypes(Map.of(header, sqlType));
        runInsertsAgainst(mock(PreparedStatement.class));

        var thrown = assertThrows(ResponseStatusException.class, () -> dataService.publish(TEST_IDENTIFIER));

        assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode(), header + "=" + value);
    }

    @Test
    void publishRejectsValuesThatDoNotFitTheirColumnType() {
        assertPublishBadRequest("flag", "maybe", Types.BOOLEAN);
        assertPublishBadRequest("day", "yesterday", Types.DATE);
        assertPublishBadRequest("moment", "noon", Types.TIME);
        assertPublishBadRequest("stamp", "2024-06-01 10:15", Types.TIMESTAMP);
        assertPublishBadRequest("whole", "1.5", Types.INTEGER);
    }

    // ACL: data sets are ACL-bearing resources

    @Test
    void createOwnsTheDataSetAndGrantsTheAdditionalGrantees() {
        var grantee = FileIngestAclRequest.builder()
            .userId(5L)
            .readPrivilege(true)
            .build();
        var request = buildValidRequest();
        request.setAcls(List.of(grantee));
        when(dataRepository.existsByIdentifier(TEST_IDENTIFIER)).thenReturn(false);
        when(dataRepository.save(any(DataEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aclService.findAclByAclId(10L)).thenReturn(List.of(Acl.builder()
            .aclId(10L)
            .userId(1L)
            .readPrivilege(true)
            .build()));

        var response = dataService.create(request);

        // the caller owns the new ACL, the grantees are validated first and added next to the owner, the rows come back in the response
        verify(aclGranteeService).validate(List.of(grantee));
        verify(aclGranteeService).grant(10L, List.of(grantee), 1L);
        assertEquals(10L, response.getAclId());
        assertEquals(1L, response.getCreator());
        assertNotNull(response.getCreated());
        assertEquals(1, response.getAcls()
            .size());
    }

    @Test
    void createRejectsInvalidGranteesBeforeAnythingIsStored() {
        var request = buildValidRequest();
        request.setAcls(List.of(FileIngestAclRequest.builder()
            .userId(404L)
            .readPrivilege(true)
            .build()));
        doThrow(new IllegalArgumentException("ACL entry refers to an unknown or inactive user 404")).when(aclGranteeService)
            .validate(any());

        var exception = assertThrows(ResponseStatusException.class, () -> dataService.create(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(dataRepository, never()).save(any());
    }

    @Test
    void updateStampsTheModifierAndReplacesTheGrantees() {
        var request = buildValidRequest();
        request.setAcls(List.of());
        var entity = buildEntity(1L);
        when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.of(entity));
        when(dataRepository.save(any(DataEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = dataService.update(request);

        assertEquals(1L, response.getModifier());
        assertNotNull(response.getModified());
        // an empty list removes every additional grantee; the owner of the set keeps its row
        verify(aclGranteeService).replace(10L, List.of(), 1L);
    }

    @Test
    void updateWithoutGranteesLeavesTheAclUntouched() {
        var request = buildValidRequest();
        var entity = buildEntity(1L);
        when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.of(entity));
        when(dataRepository.save(any(DataEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dataService.update(request);

        verify(aclGranteeService).replace(10L, null, 1L);
    }

    @Test
    void readModifyAndDeleteAreDeniedWithoutThePrivilege() {
        var entity = buildEntity(1L);
        when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.of(entity));
        when(accessService.hasReadPermission(10L)).thenReturn(false);
        when(accessService.hasModifyPermission(10L)).thenReturn(false);
        when(accessService.hasDeletePermission(10L)).thenReturn(false);

        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class, () -> dataService.findByIdentifier(TEST_IDENTIFIER)).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class, () -> dataService.getAcls(TEST_IDENTIFIER)).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class, () -> dataService.update(buildValidRequest())).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class, () -> dataService.publishAsTask(TEST_IDENTIFIER)).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class, () -> dataService.delete(TEST_IDENTIFIER)).getStatusCode());
        verify(dataRepository, never()).save(any());
        verify(dataRepository, never()).delete(any());
    }

    @Test
    void findAllListsOnlyTheReadableDataSets() {
        var entities = buildEntityList(3);
        when(dataRepository.findAll()).thenReturn(entities);
        when(accessService.hasReadPermission(11L)).thenReturn(false);

        var result = dataService.findAll(null, null, null);

        assertEquals(List.of("data_set_0", "data_set_2"), result.stream()
            .map(DataSummaryResponse::getIdentifier)
            .toList());
    }

    @Test
    void getAclsAnswersTheAdditionalGranteesInRequestShape() {
        var entity = buildEntity(1L);
        when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.of(entity));
        var grantee = FileIngestAclResponse.builder()
            .unitId(3L)
            .readPrivilege(true)
            .build();
        when(aclGranteeService.additionalGrantees(10L, 1L)).thenReturn(List.of(grantee));

        var response = dataService.getAcls(TEST_IDENTIFIER);

        assertEquals(TEST_IDENTIFIER, response.getIdentifier());
        assertEquals(10L, response.getAclId());
        assertEquals(List.of(grantee), response.getAcls());
    }

    @Test
    void deleteRemovesTheAclOfTheDataSet() throws Exception {
        var entity = buildEntity(1L);
        when(dataRepository.findByIdentifier(TEST_IDENTIFIER)).thenReturn(Optional.of(entity));

        dataService.delete(TEST_IDENTIFIER);

        verify(dataRepository).delete(entity);
        verify(aclService).deleteByAclId(10L);
        assertTrue(true);
    }

    // paged listing

    private static DataPagedRequest pagedRequest(int page, int size, String sortBy, org.springframework.data.domain.Sort.Direction direction) {
        var request = new DataPagedRequest();
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setDirection(direction);
        return request;
    }

    private List<DataEntity> pagedFixture() {
        var now = Instant.parse("2026-01-10T10:00:00Z");
        var music = buildEntity(1L);
        music.setIdentifier("music_library");
        music.setType("tabulated");
        music.setDescription("Music library");
        music.setCreated(now.minusSeconds(3000));
        music.setPublished(now.minusSeconds(2000));
        music.setModified(now.minusSeconds(1000));
        var trip = buildEntity(2L);
        trip.setAclId(11L);
        trip.setIdentifier("gps_timeseries_trip");
        trip.setType("time_series");
        trip.setDescription("GPS time-series for the trip");
        trip.setCreated(now.minusSeconds(500));
        trip.setPublished(now.minusSeconds(100));
        var weather = buildEntity(3L);
        weather.setAclId(12L);
        weather.setIdentifier("weather");
        weather.setType("time_series");
        weather.setDescription(null);
        weather.setCreated(now);
        return List.of(music, trip, weather);
    }

    private static List<String> identifiers(fi.poltsi.vempain.auth.api.response.PagedResponse<DataSummaryResponse> response) {
        return response.getContent()
            .stream()
            .map(DataSummaryResponse::getIdentifier)
            .toList();
    }

    @Test
    void findPagedSortsByIdentifierByDefaultAndByTheRequestedColumnOtherwise() {
        when(dataRepository.findAll()).thenReturn(pagedFixture());

        assertEquals(List.of("gps_timeseries_trip", "music_library", "weather"), identifiers(dataService.findPaged(pagedRequest(0, 10, null, null))));
        assertEquals(List.of("weather", "music_library", "gps_timeseries_trip"),
                identifiers(dataService.findPaged(pagedRequest(0, 10, "identifier", org.springframework.data.domain.Sort.Direction.DESC))));
        assertEquals(List.of("music_library", "gps_timeseries_trip", "weather"),
                identifiers(dataService.findPaged(pagedRequest(0, 10, "created", org.springframework.data.domain.Sort.Direction.ASC))));
        // null timestamps sort first ascending: weather (never published), then the older release
        assertEquals(List.of("weather", "music_library", "gps_timeseries_trip"),
                identifiers(dataService.findPaged(pagedRequest(0, 10, "published", org.springframework.data.domain.Sort.Direction.ASC))));
        assertEquals(List.of("music_library", "gps_timeseries_trip", "weather"),
                identifiers(dataService.findPaged(pagedRequest(0, 10, "type", org.springframework.data.domain.Sort.Direction.ASC))));
    }

    @Test
    void findPagedCutsThePageAndReportsTheTotals() {
        when(dataRepository.findAll()).thenReturn(pagedFixture());

        var page = dataService.findPaged(pagedRequest(1, 2, null, null));

        assertEquals(List.of("weather"), identifiers(page));
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertEquals(1, page.getPage());
        assertTrue(page.isLast());
    }

    @Test
    void findPagedFiltersByTypeSearchColumnReleaseStateAndReadPermission() {
        when(dataRepository.findAll()).thenReturn(pagedFixture());

        var byType = pagedRequest(0, 10, null, null);
        byType.setType("TIME_SERIES");
        assertEquals(List.of("gps_timeseries_trip", "weather"), identifiers(dataService.findPaged(byType)));

        // the search covers identifier, type and description, case insensitively unless asked otherwise
        var search = pagedRequest(0, 10, null, null);
        search.setSearch("GPS");
        assertEquals(List.of("gps_timeseries_trip"), identifiers(dataService.findPaged(search)));
        search.setCaseSensitive(true);
        assertTrue(identifiers(dataService.findPaged(search)).contains("gps_timeseries_trip"), "the description carries the upper-case GPS");
        search.setSearch("Trip");
        assertTrue(identifiers(dataService.findPaged(search)).isEmpty(), "case sensitive: the identifier is lower case");

        // filter_column restricts the search to one field; a data set without a description does not match on it
        var column = pagedRequest(0, 10, null, null);
        column.setSearch("series");
        column.setFilterColumn("description");
        assertEquals(List.of("gps_timeseries_trip"), identifiers(dataService.findPaged(column)));
        column.setFilterColumn("type");
        assertEquals(List.of("gps_timeseries_trip", "weather"), identifiers(dataService.findPaged(column)));

        // only the sets released once and modified after that release
        var outdated = pagedRequest(0, 10, null, null);
        outdated.setModifiedAfterPublish(true);
        assertEquals(List.of("music_library"), identifiers(dataService.findPaged(outdated)));

        // unreadable sets are never listed
        when(accessService.hasReadPermission(12L)).thenReturn(false);
        assertEquals(List.of("gps_timeseries_trip", "music_library"), identifiers(dataService.findPaged(pagedRequest(0, 10, null, null))));
    }
}
