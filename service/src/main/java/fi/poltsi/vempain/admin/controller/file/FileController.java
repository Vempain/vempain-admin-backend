package fi.poltsi.vempain.admin.controller.file;

import fi.poltsi.vempain.admin.api.request.file.SiteFilePagedRequest;
import fi.poltsi.vempain.admin.api.response.RefreshResponse;
import fi.poltsi.vempain.admin.api.response.file.SiteFileResponse;
import fi.poltsi.vempain.admin.rest.file.FileAPI;
import fi.poltsi.vempain.admin.service.file.FileService;
import fi.poltsi.vempain.auth.api.response.PagedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class FileController implements FileAPI {
	private final FileService fileService;

	@Override
	public ResponseEntity<PagedResponse<SiteFileResponse>> getPageableSiteFiles(SiteFilePagedRequest request) {
		var pageResponse = fileService.findAllSiteFilesAsPageableResponseFiltered(request);
		return ResponseEntity.ok(pageResponse);
	}

	@Override
	public ResponseEntity<RefreshResponse> refreshGalleryFiles(long galleryId) {
		log.debug("Received request to refresh gallery files with ID: {}", galleryId);
		var refreshResponse = fileService.refreshGalleryFiles(galleryId);
		return ResponseEntity.ok(refreshResponse);
	}

	@Override
	public ResponseEntity<RefreshResponse> refreshAllGalleryFiles() {
		log.debug("Received request to refresh all gallery files");
		var refreshResponse = fileService.refreshAllGalleryFiles();

		return ResponseEntity.ok(refreshResponse);
	}

}
