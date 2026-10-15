package fi.poltsi.vempain.admin.controller.file;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import fi.poltsi.vempain.admin.api.request.file.FileIngestRequest;
import fi.poltsi.vempain.admin.api.response.file.FileIngestResponse;
import fi.poltsi.vempain.admin.api.response.file.FileIngestUnitResponse;
import fi.poltsi.vempain.admin.api.response.file.FileIngestUserResponse;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.admin.service.file.FileIngestService;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The service-to-service ingest endpoints: the administrator ACL is required, the JSON part is parsed before anything is stored and the failures of the ingest
 * are translated to 400, 401 and 500.
 */
@ExtendWith(MockitoExtension.class)
class FileIngestControllerUTC {

    private static final String REQUEST_JSON = "{\"file_id\":7,\"file_name\":\"a.jpg\",\"mime_type\":\"image/jpeg\",\"sha256sum\":\"abc\"}";

    @Mock
    private FileIngestService fileIngestService;
    @Mock
    private AccessService accessService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private FileIngestController controller;

    private final MockMultipartFile multipartFile = new MockMultipartFile("file", "a.jpg", "image/jpeg", "bytes".getBytes());

    private FileIngestRequest parsedRequest() {
        var request = FileIngestRequest.builder()
            .fileId(7L)
            .fileName("a.jpg")
            .mimeType("image/jpeg")
            .sha256sum("abc")
            .build();
        when(objectMapper.readValue(REQUEST_JSON, FileIngestRequest.class)).thenReturn(request);
        return request;
    }

    @Test
    void aSuccessfulIngestAnswersTheStoredIds() throws Exception {
        var request = parsedRequest();
        var result = FileIngestResponse.builder()
            .siteFileId(42L)
            .galleryId(7L)
            .build();
        when(fileIngestService.ingest(request, multipartFile)).thenReturn(result);

        var response = controller.ingest(REQUEST_JSON, multipartFile);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(result, response.getBody());
        verify(accessService).checkAdminAccess();
    }

    @Test
    void unparseableRequestJsonIsABadRequestAndNothingIsIngested() throws Exception {
        when(objectMapper.readValue("not json", FileIngestRequest.class)).thenThrow(new IllegalArgumentException("not json"));

        var response = controller.ingest("not json", multipartFile);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(fileIngestService, never()).ingest(any(), any());
    }

    @Test
    void ingestFailuresAreTranslatedToStatusCodes() throws Exception {
        var request = parsedRequest();

        doThrow(new IllegalArgumentException("Missing payload")).when(fileIngestService)
            .ingest(request, multipartFile);
        assertEquals(HttpStatus.BAD_REQUEST, controller.ingest(REQUEST_JSON, multipartFile)
            .getStatusCode());

        doThrow(new AccessDeniedException("Unauthorized S2S call")).when(fileIngestService)
            .ingest(request, multipartFile);
        assertEquals(HttpStatus.UNAUTHORIZED, controller.ingest(REQUEST_JSON, multipartFile)
            .getStatusCode());

        doThrow(new IllegalStateException("disk full")).when(fileIngestService)
            .ingest(request, multipartFile);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, controller.ingest(REQUEST_JSON, multipartFile)
            .getStatusCode());
    }

    @Test
    void theGrantableUsersAreListedForAdministrators() {
        var users = List.of(FileIngestUserResponse.builder()
            .id(1L)
            .loginName("admin")
            .build());
        when(fileIngestService.listIngestUsers()).thenReturn(users);

        var response = controller.listIngestUsers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(users, response.getBody());
        verify(accessService).checkAdminAccess();
    }

    @Test
    void theGrantableUnitsAreListedForAdministrators() {
        var units = List.of(FileIngestUnitResponse.builder()
            .id(3L)
            .name("Editors")
            .build());
        when(fileIngestService.listIngestUnits()).thenReturn(units);

        var response = controller.listIngestUnits();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(units, response.getBody());
        verify(accessService).checkAdminAccess();
    }

    @Test
    void anIngestedSiteFileIsDeletedWithNoContent() {
        var response = controller.deleteSiteFile(42L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(accessService).checkAdminAccess();
        verify(fileIngestService).deleteIngestedSiteFile(42L);
    }

    @Test
    void everyEndpointRequiresTheAdministratorAcl() {
        doThrow(new AccessDeniedException("not an administrator")).when(accessService)
            .checkAdminAccess();

        assertThrows(AccessDeniedException.class, () -> controller.ingest(REQUEST_JSON, multipartFile));
        assertThrows(AccessDeniedException.class, () -> controller.listIngestUsers());
        assertThrows(AccessDeniedException.class, () -> controller.listIngestUnits());
        assertThrows(AccessDeniedException.class, () -> controller.deleteSiteFile(1L));
        verify(fileIngestService, never()).deleteIngestedSiteFile(eq(1L));
    }
}
