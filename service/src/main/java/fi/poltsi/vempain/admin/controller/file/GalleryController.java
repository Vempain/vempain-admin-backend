package fi.poltsi.vempain.admin.controller.file;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.ContentTypeEnum;
import fi.poltsi.vempain.admin.api.PublishResultEnum;
import fi.poltsi.vempain.admin.api.QueryDetailEnum;
import fi.poltsi.vempain.admin.api.request.PublishRequest;
import fi.poltsi.vempain.admin.api.request.file.GalleryRequest;
import fi.poltsi.vempain.admin.api.response.DeleteResponse;
import fi.poltsi.vempain.admin.api.response.PublishResponse;
import fi.poltsi.vempain.admin.api.response.file.GalleryResponse;
import fi.poltsi.vempain.admin.entity.PageGallery;
import fi.poltsi.vempain.admin.exception.VempainAclException;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.admin.rest.file.GalleryAPI;
import fi.poltsi.vempain.admin.service.PageGalleryService;
import fi.poltsi.vempain.admin.service.PublishService;
import fi.poltsi.vempain.admin.service.ScheduleService;
import fi.poltsi.vempain.admin.service.file.GalleryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
public class GalleryController implements GalleryAPI {
	private final GalleryService     galleryService;
	private final PublishService     publishService;
	private final PageGalleryService pageGalleryService;
	private final ScheduleService    scheduleService;

	@Override
	public ResponseEntity<List<GalleryResponse>> getGalleries(QueryDetailEnum queryDetailEnum) {
		return ResponseEntity.ok(galleryService.findAllAsResponsesForUser(queryDetailEnum));
	}

	@Override
	public ResponseEntity<List<GalleryResponse>> getGalleriesByPage(long pageId, QueryDetailEnum queryDetailEnum) {
		var pageGalleries = new ArrayList<GalleryResponse>();
		var galleries = pageGalleryService.findPageGalleryByPageId(pageId);

		for (PageGallery pageGallery : galleries) {
			pageGalleries.add(galleryService.findById(pageGallery.getGalleryId()));
		}

		return ResponseEntity.ok(pageGalleries);
	}

	@Override
	public ResponseEntity<GalleryResponse> getGalleryById(long galleryId) {
		return ResponseEntity.ok(galleryService.findById(galleryId));
	}

	@Override
	public ResponseEntity<GalleryResponse> createGallery(GalleryRequest galleryRequest) {
		GalleryResponse galleryResponse;

		try {
			galleryResponse = galleryService.createGallery(galleryRequest);
		} catch (VempainAclException e) {
			log.error("Could not create gallery: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}

		return ResponseEntity.ok(galleryResponse);
	}

	@Override
	public ResponseEntity<DeleteResponse> deleteGallery(long galleryId) {
		var gallery = galleryService.findById(galleryId);

		if (gallery == null) {
			log.error("Could not find gallery with ID {} for deletion", galleryId);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		try {
			galleryService.deleteGallery(galleryId);
		} catch (Exception e) {
			log.error("Could not delete gallery with ID {}: {}", galleryId, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}

		return ResponseEntity.ok(DeleteResponse.builder()
											   .id(galleryId)
											   .count(1L)
											   .name(gallery.getShortName())
											   .httpStatus(HttpStatus.OK)
											   .timestamp(Instant.now())
											   .build());
	}

	@Override
	public ResponseEntity<GalleryResponse> updateGallery(GalleryRequest galleryRequest) {
		log.debug("Received request to update gallery with ID {}: {}", galleryRequest.getId(), galleryRequest);

		if (galleryRequest.getId() == 0) {
			log.error("Gallery ID is missing in the request");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}

		GalleryResponse galleryResponse;

		try {
			galleryResponse = galleryService.updateGallery(galleryRequest);
		} catch (VempainAclException e) {
			log.error("Could not update gallery: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}

		return ResponseEntity.ok(galleryResponse);
	}

	@Override
	public ResponseEntity<List<GalleryResponse>> setPageGalleries(long pageId, List<Long> galleryIdList) {

		var galleryResponses = pageGalleryService.setPageGalleries(pageId, galleryIdList);

		return ResponseEntity.ok(galleryResponses);
	}

	// //////////////////// Publishing actions

	@Override
	public ResponseEntity<PublishResponse> publishAll() {
		PublishResponse response;

		try {
			publishService.publishAllGalleries();
			response = PublishResponse.builder()
									  .result(PublishResultEnum.OK)
									  .message("Successfully published all galleries")
									  .timestamp(Instant.now())
									  .build();
		} catch (VempainEntityNotFoundException e) {
			response = PublishResponse.builder()
									  .result(PublishResultEnum.FAIL)
									  .message("Could not find any galleries")
									  .timestamp(Instant.now())
									  .build();
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
								 .body(response);
		}

		return ResponseEntity.ok(response);
	}

	@Override
	public ResponseEntity<PublishResponse> publishGallery(PublishRequest publishRequest) {
		PublishResponse response;

		if (publishRequest == null || publishRequest.getId() < 1L) {
			log.error(VempainMessages.MALFORMED_ID_IN_REQUEST_MSG, publishRequest);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_ID_IN_REQUEST);
		}

		if (publishRequest.isPublishSchedule()) {
			scheduleService.schedulePublish(publishRequest.getPublishDateTime(), publishRequest.getId(), ContentTypeEnum.PAGE);

			response = PublishResponse.builder()
									  .result(PublishResultEnum.OK)
									  .message("Successfully scheduled page for publishing")
									  .timestamp(Instant.now())
									  .build();
			return ResponseEntity.ok(response);
		}

		try {
			publishService.publishGallery(publishRequest.getId());
			response = PublishResponse.builder()
									  .result(PublishResultEnum.OK)
									  .message("Successfully published gallery")
									  .timestamp(Instant.now())
									  .build();
		} catch (VempainEntityNotFoundException e) {
			response = PublishResponse.builder()
									  .result(PublishResultEnum.FAIL)
									  .message("Could not find gallery")
									  .timestamp(Instant.now())
									  .build();
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
								 .body(response);
		}

		return ResponseEntity.ok(response);
	}
}
