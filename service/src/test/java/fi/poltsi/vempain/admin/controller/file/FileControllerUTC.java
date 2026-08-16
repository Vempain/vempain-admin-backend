package fi.poltsi.vempain.admin.controller.file;

import fi.poltsi.vempain.admin.api.request.file.SiteFilePagedRequest;
import fi.poltsi.vempain.admin.api.response.RefreshResponse;
import fi.poltsi.vempain.admin.api.response.file.SiteFileResponse;
import fi.poltsi.vempain.admin.service.file.FileService;
import fi.poltsi.vempain.auth.api.response.PagedResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileControllerUTC {
	@Mock
	private FileService    fileService;
	@InjectMocks
	private FileController controller;

	@Test
	void delegatesAllFileEndpoints() {
		var request = new SiteFilePagedRequest();
		var paged = PagedResponse.of(java.util.List.<SiteFileResponse>of(), 0, 25, 0, 0, true, true);
		var refresh = RefreshResponse.builder()
		                             .build();
		when(fileService.findAllSiteFilesAsPageableResponseFiltered(request)).thenReturn(paged);
		when(fileService.refreshGalleryFiles(3L)).thenReturn(refresh);
		when(fileService.refreshAllGalleryFiles()).thenReturn(refresh);

		assertSame(paged, controller.getPageableSiteFiles(request)
		                            .getBody());
		assertSame(refresh, controller.refreshGalleryFiles(3L)
		                              .getBody());
		assertSame(refresh, controller.refreshAllGalleryFiles()
		                              .getBody());
	}
}
