package fi.poltsi.vempain.admin.controller.file;

import fi.poltsi.vempain.admin.api.response.file.GalleryResponse;
import fi.poltsi.vempain.admin.service.PageGalleryService;
import fi.poltsi.vempain.admin.service.PublishService;
import fi.poltsi.vempain.admin.service.ScheduleService;
import fi.poltsi.vempain.admin.service.file.GalleryService;
import fi.poltsi.vempain.auth.api.request.PagedRequest;
import fi.poltsi.vempain.auth.api.response.PagedResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GalleryControllerUTC {
	@Mock
	private GalleryService     galleryService;
	@Mock
	private PublishService     publishService;
	@Mock
	private PageGalleryService pageGalleryService;
	@Mock
	private ScheduleService    scheduleService;

	@InjectMocks
	private GalleryController galleryController;

	@Test
	void getPagedGalleriesWithoutFilesDelegatesToService() {
		var request = new PagedRequest();
		var response = PagedResponse.of(java.util.List.<GalleryResponse>of(), 0, 25, 0, 0, true, true);
		when(galleryService.findPagedByUserWithoutFiles(request)).thenReturn(response);

		var result = galleryController.getPagedGalleriesWithoutFiles(request);

		assertSame(response, result.getBody());
	}
}
