package fi.poltsi.vempain.admin.controller.file;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import fi.poltsi.vempain.admin.api.ContentTypeEnum;
import fi.poltsi.vempain.admin.api.PublishResultEnum;
import fi.poltsi.vempain.admin.api.request.PublishRequest;
import fi.poltsi.vempain.admin.api.request.file.GalleryPagedRequest;
import fi.poltsi.vempain.admin.api.request.file.GalleryPublishRequest;
import fi.poltsi.vempain.admin.api.request.file.GalleryRequest;
import fi.poltsi.vempain.admin.api.response.PublishScheduleResponse;
import fi.poltsi.vempain.admin.api.response.file.FileGroupListResponse;
import fi.poltsi.vempain.admin.api.response.file.GalleryResponse;
import fi.poltsi.vempain.admin.entity.PageGallery;
import fi.poltsi.vempain.admin.entity.file.Gallery;
import fi.poltsi.vempain.admin.service.PageGalleryService;
import fi.poltsi.vempain.admin.service.PublishService;
import fi.poltsi.vempain.admin.service.ScheduleService;
import fi.poltsi.vempain.admin.service.file.GalleryService;
import fi.poltsi.vempain.auth.api.request.PagedRequest;
import fi.poltsi.vempain.auth.api.response.PagedResponse;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GalleryControllerUTC {

    @Mock
    private GalleryService galleryService;
    @Mock
    private PublishService publishService;
    @Mock
    private PageGalleryService pageGalleryService;
    @Mock
    private ScheduleService scheduleService;

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
        var request = new GalleryPagedRequest();
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
        assertEquals(202, galleryController.publishAll(null)
            .getStatusCode()
            .value());
        assertEquals(202, galleryController.publishGallery(PublishRequest.builder()
            .id(7L)
            .build())
            .getStatusCode()
            .value());
        var publishRequest = new GalleryPublishRequest();
        publishRequest.setGalleryIds(java.util.List.of(7L));
        assertEquals(202, galleryController.publishSelectedGalleries(publishRequest)
            .getStatusCode()
            .value());
    }

    // ---- failure answers of the write endpoints, scheduled publishing ----

    @Test
    void createAndUpdateAnswerBadRequestOnAclFailuresAndAMissingId() throws Exception {
        var request = new GalleryRequest();
        when(galleryService.createGallery(request)).thenThrow(new VempainAclException("bad acl"));
        assertEquals(HttpStatus.BAD_REQUEST, galleryController.createGallery(request)
            .getStatusCode());

        assertEquals(HttpStatus.BAD_REQUEST, galleryController.updateGallery(request)
            .getStatusCode());

        request.setId(7L);
        when(galleryService.updateGallery(request)).thenThrow(new VempainAclException("bad acl"));
        assertEquals(HttpStatus.BAD_REQUEST, galleryController.updateGallery(request)
            .getStatusCode());
    }

    @Test
    void deleteAnswersNotFoundForAMissingGalleryAndInternalErrorWhenTheDeletionFails() {
        when(galleryService.findById(8L)).thenReturn(null);
        assertEquals(HttpStatus.NOT_FOUND, galleryController.deleteGallery(8L)
            .getStatusCode());

        when(galleryService.findById(9L)).thenReturn(GalleryResponse.builder()
            .id(9L)
            .build());
        doThrow(new IllegalStateException("constraint")).when(galleryService)
            .deleteGallery(9L);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, galleryController.deleteGallery(9L)
            .getStatusCode());
    }

    @Test
    void publishAllWithADateSchedulesOnlyTheGalleriesTheUserMayPublish() {
        var publishDate = Instant.now()
            .plus(1, ChronoUnit.DAYS);
        when(galleryService.findAll()).thenReturn(List.of(Gallery.builder()
            .id(1L)
            .build(),
                Gallery.builder()
                    .id(2L)
                    .build()));
        when(publishService.canPublishGallery(1L)).thenReturn(true);
        when(publishService.canPublishGallery(2L)).thenReturn(false);
        when(scheduleService.schedulePublish(publishDate, 1L, ContentTypeEnum.GALLERY, "Publish all galleries")).thenReturn(PublishScheduleResponse.builder()
            .id(1L)
            .build());

        var response = galleryController.publishAll(publishDate);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(PublishResultEnum.OK, response.getBody()
            .getResult());
        verify(scheduleService, never()).schedulePublish(any(), eq(2L), any(), any());
    }

    @Test
    void publishGalleryRejectsAMalformedRequestAndAnswersNotFoundForAMissingGallery() throws Exception {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class, () -> galleryController.publishGallery(null)).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class, () -> galleryController.publishGallery(PublishRequest.builder()
            .id(0L)
            .build())).getStatusCode());

        when(publishService.publishGalleryAsTask(2L)).thenThrow(new VempainEntityNotFoundException());
        var notFound = galleryController.publishGallery(PublishRequest.builder()
            .id(2L)
            .build());
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());
        assertEquals(PublishResultEnum.FAIL, notFound.getBody()
            .getResult());
    }

    @Test
    void publishGalleryWithAScheduleVerifiesThePrivilegesAndStoresTheSchedule() throws Exception {
        var publishDate = Instant.now()
            .plus(1, ChronoUnit.DAYS);
        var request = PublishRequest.builder()
            .id(1L)
            .publishSchedule(true)
            .publishDateTime(publishDate)
            .publishMessage("Later")
            .build();
        when(scheduleService.schedulePublish(publishDate, 1L, ContentTypeEnum.GALLERY, "Later")).thenReturn(PublishScheduleResponse.builder()
            .id(5L)
            .build());

        var scheduled = galleryController.publishGallery(request);

        assertEquals(HttpStatus.OK, scheduled.getStatusCode());
        assertEquals("Successfully scheduled gallery for publishing", scheduled.getBody()
            .getMessage());
        verify(publishService).authorizeGalleryPublish(1L);
        verify(publishService, never()).publishGalleryAsTask(1L);

        when(scheduleService.schedulePublish(publishDate, 1L, ContentTypeEnum.GALLERY, "Later")).thenReturn(null);
        var failed = galleryController.publishGallery(request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, failed.getStatusCode());
        assertEquals(PublishResultEnum.FAIL, failed.getBody()
            .getResult());
    }
}
