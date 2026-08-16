package fi.poltsi.vempain.admin.controller.file;

import fi.poltsi.vempain.admin.api.request.PublishRequest;
import fi.poltsi.vempain.admin.api.request.file.GalleryPublishRequest;
import fi.poltsi.vempain.admin.api.request.file.GalleryRequest;
import fi.poltsi.vempain.admin.api.response.file.FileGroupListResponse;
import fi.poltsi.vempain.admin.api.response.file.GalleryResponse;
import fi.poltsi.vempain.admin.entity.PageGallery;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
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

	@Test
	void getPagedGalleryListDelegatesToService() {
		var request = new PagedRequest();
		var response = PagedResponse.of(java.util.List.<FileGroupListResponse>of(), 0, 25, 0, 0, true, true);
		when(galleryService.findPagedGalleryListByUser(request)).thenReturn(response);

		var result = galleryController.getPagedGalleryList(request);

		assertSame(response, result.getBody());
		verify(galleryService).findPagedGalleryListByUser(request);
	}

	@Test
	void getGalleryListByPageDelegatesToService() {
		var gallery = fi.poltsi.vempain.admin.api.response.file.FileGroupListResponse.builder()
		                                                                             .id(7L)
		                                                                             .build();
		var pageGallery = PageGallery.builder()
		                             .galleryId(7L)
		                             .build();
		when(pageGalleryService.findPageGalleryByPageId(3L)).thenReturn(java.util.List.of(pageGallery));
		when(galleryService.findGalleryListById(7L)).thenReturn(gallery);

		var result = galleryController.getGalleryListByPage(3L);

		assertSame(gallery, result.getBody()
		                          .getFirst());
		verify(galleryService).findGalleryListById(7L);
	}

	@Test
	void delegatesReadWriteAndPageEndpoints() throws Exception {
		var gallery = GalleryResponse.builder()
		                             .id(7L)
		                             .shortName("summer")
		                             .build();
		var request = new GalleryRequest();
		var pagedRequest = new PagedRequest();
		var pageGallery = PageGallery.builder()
		                             .galleryId(7L)
		                             .build();
		when(galleryService.findAllAsResponsesForUser(null)).thenReturn(java.util.List.of(gallery));
		when(galleryService.findPagedByUser(pagedRequest)).thenReturn(null);
		when(pageGalleryService.findPageGalleryByPageId(3L)).thenReturn(java.util.List.of(pageGallery));
		when(galleryService.findById(7L)).thenReturn(gallery);
		when(galleryService.createGallery(request)).thenReturn(gallery);
		when(galleryService.updateGallery(request)).thenReturn(gallery);
		when(pageGalleryService.setPageGalleries(3L, java.util.List.of(7L))).thenReturn(java.util.List.of(gallery));

		assertEquals(gallery, galleryController.getGalleries(null)
		                                       .getBody()
		                                       .getFirst());
		assertSame(null, galleryController.getPagedGalleries(pagedRequest)
		                                  .getBody());
		assertSame(gallery, galleryController.getGalleryById(7L)
		                                     .getBody());
		assertEquals(gallery, galleryController.getGalleriesByPage(3L, null)
		                                       .getBody()
		                                       .getFirst());
		assertSame(gallery, galleryController.createGallery(request)
		                                     .getBody());
		request.setId(7L);
		assertSame(gallery, galleryController.updateGallery(request)
		                                     .getBody());
		assertSame(gallery, galleryController.setPageGalleries(3L, java.util.List.of(7L))
		                                     .getBody()
		                                     .getFirst());
		verify(galleryService).findPagedByUser(pagedRequest);
	}

	@Test
	void publishesImmediatelyAndReturnsDeleteResponse() {
		var gallery = GalleryResponse.builder()
		                             .id(7L)
		                             .shortName("summer")
		                             .build();
		when(galleryService.findById(7L)).thenReturn(gallery);

		assertEquals(200, galleryController.deleteGallery(7L)
		                                   .getStatusCode()
		                                   .value());
		assertEquals(200, galleryController.publishAll(null)
		                                   .getStatusCode()
		                                   .value());
		assertEquals(200, galleryController.publishGallery(PublishRequest.builder()
		                                                                 .id(7L)
		                                                                 .build())
		                                   .getStatusCode()
		                                   .value());
		var publishRequest = new GalleryPublishRequest();
		publishRequest.setGalleryIds(java.util.List.of(7L));
		assertEquals(200, galleryController.publishSelectedGalleries(publishRequest)
		                                   .getStatusCode()
		                                   .value());
	}
}
