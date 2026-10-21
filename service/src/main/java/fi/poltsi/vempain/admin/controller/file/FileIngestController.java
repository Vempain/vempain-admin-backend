package fi.poltsi.vempain.admin.controller.file;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import fi.poltsi.vempain.admin.api.request.file.FileIngestRequest;
import fi.poltsi.vempain.admin.api.response.file.FileIngestGalleryAclResponse;
import fi.poltsi.vempain.admin.api.response.file.FileIngestResponse;
import fi.poltsi.vempain.admin.api.response.file.FileIngestSiteFileAclResponse;
import fi.poltsi.vempain.admin.api.response.file.FileIngestUnitResponse;
import fi.poltsi.vempain.admin.api.response.file.FileIngestUserResponse;
import fi.poltsi.vempain.admin.rest.file.FileIngestAPI;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.admin.service.file.FileIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FileIngestController implements FileIngestAPI {

    private final FileIngestService fileIngestService;
    private final AccessService accessService;
    private final ObjectMapper objectMapper;

    @Override
    public ResponseEntity<FileIngestResponse> ingest(String fileIngestRequestJSON, MultipartFile multipartFile) {
        accessService.checkAdminAccess();
        // First we use object mapper to convert the fileIngestRequestJSON into FileIngestRequest
        FileIngestRequest fileIngestRequest;

        try {
            fileIngestRequest = objectMapper.readValue(fileIngestRequestJSON, FileIngestRequest.class);
            log.debug("Received file ingest request: fileId={}, fileName={}, metadataPresent={}, metadataLength={}", fileIngestRequest.getFileId(),
                    fileIngestRequest.getFileName(), fileIngestRequest.getMetadata() != null, fileIngestRequest.getMetadata() != null
                            ? fileIngestRequest.getMetadata()
                                .length()
                            : null);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid file ingest request JSON: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .build();
        }

        try {
            var result = fileIngestService.ingest(fileIngestRequest, multipartFile);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Bad request in file ingest: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .build();
        } catch (AccessDeniedException e) {
            log.warn("Unauthorized S2S call: {}", e.getMessage());
            return ResponseEntity.status(401)
                .build();
        } catch (Exception e) {
            log.error("File ingest failed", e);
            return ResponseEntity.internalServerError()
                .build();
        }
    }

    @Override
    public ResponseEntity<List<FileIngestUserResponse>> listIngestUsers() {
        accessService.checkAdminAccess();
        return ResponseEntity.ok(fileIngestService.listIngestUsers());
    }

    @Override
    public ResponseEntity<List<FileIngestUnitResponse>> listIngestUnits() {
        accessService.checkAdminAccess();
        return ResponseEntity.ok(fileIngestService.listIngestUnits());
    }

    @Override
    public ResponseEntity<Void> deleteSiteFile(long siteFileId) {
        accessService.checkAdminAccess();
        log.debug("Received request to delete ingested site file {}", siteFileId);
        fileIngestService.deleteIngestedSiteFile(siteFileId);
        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<FileIngestSiteFileAclResponse> getSiteFileAcls(long siteFileId) {
        accessService.checkAdminAccess();
        return ResponseEntity.ok(fileIngestService.getSiteFileAcls(siteFileId));
    }

    @Override
    public ResponseEntity<FileIngestSiteFileAclResponse> getSiteFileAclsByFileId(long fileId) {
        accessService.checkAdminAccess();
        return ResponseEntity.ok(fileIngestService.getSiteFileAclsByFileId(fileId));
    }

    @Override
    public ResponseEntity<FileIngestGalleryAclResponse> getGalleryAcls(long galleryId) {
        accessService.checkAdminAccess();
        return ResponseEntity.ok(fileIngestService.getGalleryAcls(galleryId));
    }
}
