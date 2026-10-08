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
	@Mock
	private fi.poltsi.vempain.admin.service.file.GalleryService galleryService;
	@Mock
	private fi.poltsi.vempain.admin.service.AccessService       accessService;
	@InjectMocks
	private FileController controller;

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
