package fi.poltsi.vempain.admin.controller.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fi.poltsi.vempain.admin.api.request.file.SiteFilePagedRequest;
import fi.poltsi.vempain.admin.api.request.file.SiteFilePublishRequest;
import fi.poltsi.vempain.admin.api.response.RefreshResponse;
import fi.poltsi.vempain.admin.api.response.file.SiteFileResponse;
import fi.poltsi.vempain.admin.service.file.FileService;
import fi.poltsi.vempain.auth.api.response.PagedResponse;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileControllerUTC {

    @Mock
    private FileService fileService;
    @Mock
    private fi.poltsi.vempain.admin.service.file.GalleryService galleryService;
    @Mock
    private fi.poltsi.vempain.admin.service.AccessService accessService;
    @Mock
    private fi.poltsi.vempain.admin.service.PublishService publishService;
    @Mock
    private fi.poltsi.vempain.admin.service.DeleteService deleteService;
    @InjectMocks
    private FileController controller;

    @Test
    void publishSelectedSiteFilesAnswersAcceptedWithTheTask() {
        var request = new SiteFilePublishRequest();
        request.setFileIds(java.util.List.of(5L, 6L));
        var accepted = fi.poltsi.vempain.common.api.response.TaskAcceptedResponse.builder()
            .taskId("task-2")
            .type("PUBLISH_SELECTED_FILES")
            .build();
        when(publishService.publishSelectedFilesAsTask(java.util.List.of(5L, 6L))).thenReturn(accepted);

        var response = controller.publishSelectedSiteFiles(request);

        org.junit.jupiter.api.Assertions.assertEquals(202, response.getStatusCode()
            .value());
        assertSame(accepted, response.getBody()
            .getTask());
    }

    @Test
    void deleteSiteFileAnswersTheDeletedFileAndNotFoundForAMissingOne() throws Exception {
        when(deleteService.deleteSiteFileById(21L)).thenReturn(fi.poltsi.vempain.admin.entity.file.SiteFile.builder()
            .id(21L)
            .fileName("a.jpg")
            .build());
        when(deleteService.deleteSiteFileById(22L)).thenThrow(new fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException());

        var deleted = controller.deleteSiteFile(21L);
        org.junit.jupiter.api.Assertions.assertEquals(200, deleted.getStatusCode()
            .value());
        org.junit.jupiter.api.Assertions.assertEquals("a.jpg", deleted.getBody()
            .getName());
        org.junit.jupiter.api.Assertions.assertEquals(21L, deleted.getBody()
            .getId());

        org.junit.jupiter.api.Assertions.assertEquals(404, controller.deleteSiteFile(22L)
            .getStatusCode()
            .value());
    }

    @Test
    void delegatesAllFileEndpoints() {
        var request = new SiteFilePagedRequest();
        var paged = PagedResponse.of(java.util.List.<SiteFileResponse>of(), 0, 25, 0, 0, true, true);
        var refresh = RefreshResponse.builder()
            .build();
        when(fileService.findAllSiteFilesAsPageableResponseFiltered(request)).thenReturn(paged);
        var accepted = fi.poltsi.vempain.common.api.response.TaskAcceptedResponse.builder()
            .taskId("task-1")
            .type("REFRESH_ALL_GALLERY_FILES")
            .build();
        when(fileService.refreshGalleryFiles(3L)).thenReturn(refresh);
        when(fileService.refreshAllGalleryFilesAsTask()).thenReturn(accepted);

        assertSame(paged, controller.getPageableSiteFiles(request)
            .getBody());
        assertSame(refresh, controller.refreshGalleryFiles(3L)
            .getBody());
        var refreshAll = controller.refreshAllGalleryFiles();
        org.junit.jupiter.api.Assertions.assertEquals(202, refreshAll.getStatusCode()
            .value());
        assertSame(accepted, refreshAll.getBody()
            .getTask());
        org.mockito.Mockito.verify(galleryService)
            .requireModify(3L);
        org.mockito.Mockito.verify(accessService)
            .checkAdminAccess();
    }
}
