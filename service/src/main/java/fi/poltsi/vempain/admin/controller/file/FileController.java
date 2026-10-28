package fi.poltsi.vempain.admin.controller.file;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import fi.poltsi.vempain.admin.api.PublishResultEnum;
import fi.poltsi.vempain.admin.api.request.file.SiteFilePagedRequest;
import fi.poltsi.vempain.admin.api.request.file.SiteFilePublishRequest;
import fi.poltsi.vempain.admin.api.response.DeleteResponse;
import fi.poltsi.vempain.admin.api.response.PublishResponse;
import fi.poltsi.vempain.admin.api.response.RefreshResponse;
import fi.poltsi.vempain.admin.api.response.file.SiteFileResponse;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.rest.file.FileAPI;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.admin.service.DeleteService;
import fi.poltsi.vempain.admin.service.PublishService;
import fi.poltsi.vempain.admin.service.file.FileService;
import fi.poltsi.vempain.admin.service.file.GalleryService;
import fi.poltsi.vempain.auth.api.response.PagedResponse;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
public class FileController implements FileAPI {

    private final FileService fileService;
    private final GalleryService galleryService;
    private final AccessService accessService;
    private final PublishService publishService;
    private final DeleteService deleteService;

    @Override
    public ResponseEntity<PagedResponse<SiteFileResponse>> getPageableSiteFiles(SiteFilePagedRequest request) {
        var pageResponse = fileService.findAllSiteFilesAsPageableResponseFiltered(request);
        return ResponseEntity.ok(pageResponse);
    }

    @Override
    public ResponseEntity<PublishResponse> publishSelectedSiteFiles(SiteFilePublishRequest request) {
        var task = publishService.publishSelectedFilesAsTask(request.getFileIds());
        var response = PublishResponse.builder()
            .result(PublishResultEnum.OK)
            .message("Publishing of the selected files started")
            .timestamp(Instant.now())
            .task(task)
            .build();
        return ResponseEntity.accepted()
            .body(response);
    }

    @Override
    public ResponseEntity<DeleteResponse> deleteSiteFile(long siteFileId) {
        log.debug("Received request to delete site file with ID: {}", siteFileId);
        SiteFile siteFile;

        try {
            siteFile = deleteService.deleteSiteFileById(siteFileId);
        } catch (VempainEntityNotFoundException e) {
            log.error("Could not find site file with ID {} for deletion", siteFileId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .build();
        }

        return ResponseEntity.ok(DeleteResponse.builder()
            .id(siteFileId)
            .count(1L)
            .name(siteFile.getFileName())
            .httpStatus(HttpStatus.OK)
            .timestamp(Instant.now())
            .build());
    }

    @Override
    public ResponseEntity<RefreshResponse> refreshGalleryFiles(long galleryId) {
        log.debug("Received request to refresh gallery files with ID: {}", galleryId);
        // Regenerating the gallery's derived files changes the gallery, so the modify privilege on the gallery is required
        galleryService.requireModify(galleryId);
        var refreshResponse = fileService.refreshGalleryFiles(galleryId);
        return ResponseEntity.ok(refreshResponse);
    }

    @Override
    public ResponseEntity<RefreshResponse> refreshAllGalleryFiles() {
        log.debug("Received request to refresh all gallery files");
        // Touches every gallery regardless of ownership, therefore reserved to the administrator ACL
        accessService.checkAdminAccess();
        var task = fileService.refreshAllGalleryFilesAsTask();
        var refreshResponse = RefreshResponse.builder()
            .result(PublishResultEnum.OK)
            .refreshedItems(0)
            .failedItems(0)
            .details(List.of())
            .task(task)
            .build();

        return ResponseEntity.accepted()
            .body(refreshResponse);
    }
}
