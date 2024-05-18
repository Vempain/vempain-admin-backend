package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.PublishResultEnum;
import fi.poltsi.vempain.admin.api.QueryDetailEnum;
import fi.poltsi.vempain.admin.api.request.PageRequest;
import fi.poltsi.vempain.admin.api.response.DeleteResponse;
import fi.poltsi.vempain.admin.api.response.PageResponse;
import fi.poltsi.vempain.admin.api.response.PublishResponse;
import fi.poltsi.vempain.admin.entity.Page;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.admin.rest.PageAPI;
import fi.poltsi.vempain.admin.service.DeleteService;
import fi.poltsi.vempain.admin.service.PageService;
import fi.poltsi.vempain.admin.service.PublishService;
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
public class PageController implements PageAPI {
	private final PageService    pageService;
	private final PublishService publishService;
	private final DeleteService  deleteService;

	@Override
	public ResponseEntity<List<PageResponse>> getPages(QueryDetailEnum queryDetailEnum) {
		var pageList  = pageService.findAllByUser();
		var responses = new ArrayList<PageResponse>();

		if (queryDetailEnum == QueryDetailEnum.FULL) {
			for (Page page : pageList) {
				responses.add(pageService.populateResponse(page));
			}
		} else if (queryDetailEnum == QueryDetailEnum.MINIMAL) {
			for (Page page : pageList) {
				responses.add(PageResponse.builder()
										  .id(page.getId())
										  .path(page.getPath())
										  .build());
			}
		} else if (queryDetailEnum == QueryDetailEnum.UNPOPULATED) {
			for (Page page : pageList) {
				page.setPublished(pageService.whenIsPagePublished(page.getId()));
				responses.add(page.toResponse());
			}
		} else {
			log.error("Unknown query detail enum: {}", queryDetailEnum);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown query detail enum");
		}

		return ResponseEntity.ok(responses);
	}

	@Override
	public ResponseEntity<List<PageResponse>> getPagesByFormId(long formId) {
		var pages = pageService.findAllByFormId(formId);

		ArrayList<PageResponse> responses = new ArrayList<>();

		for (Page page : pages) {
			responses.add(pageService.populateResponse(page));
		}

		return ResponseEntity.ok(responses);
	}

	@Override
	public ResponseEntity<PageResponse> getPageById(long pageId) {
		var page = pageService.findById(pageId);
		log.debug("Found page with ID {}: {}", pageId, page);

		if (page == null) {
			log.error("Could not retrieve page with ID {}", pageId);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		}

		var pageResponse = pageService.populateResponse(page);
		log.debug("Returning page response: {}", pageResponse);
		return ResponseEntity.ok(pageResponse);
	}

	@Override
	public ResponseEntity<PageResponse> addPage(PageRequest pageRequest) {
		log.debug("Received call to add a page with: {}", pageRequest);
		verifyPageRequest(pageRequest);
		log.debug("Request verified, saving page.");
		var page = pageService.saveFromPageRequest(pageRequest);
		return ResponseEntity.ok(pageService.populateResponse(page));
	}

	@Override
	public ResponseEntity<PageResponse> updatePage(PageRequest pageRequest) {
		if (pageRequest == null || pageRequest.getId() < 1L) {
			log.error(VempainMessages.MALFORMED_ID_IN_REQUEST_MSG, pageRequest);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_ID_IN_REQUEST);
		}

		verifyPageRequest(pageRequest);

		var page         = pageService.updateFromRequest(pageRequest);
		var pageResponse = pageService.populateResponse(page);
		return ResponseEntity.ok(pageResponse);
	}

	@Override
	public ResponseEntity<DeleteResponse> deletePage(long pageId) {
		if (pageId < 1L) {
			log.error(VempainMessages.MALFORMED_ID_IN_REQUEST_MSG, pageId);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_ID_IN_REQUEST);
		}

		try {
			deleteService.deletePageById(pageId);
			return ResponseEntity.ok(DeleteResponse.builder()
												   .count(1)
												   .id(pageId)
												   .name("Form")
												   .timestamp(Instant.now())
												   .httpStatus(HttpStatus.OK)
												   .build());
		} catch (VempainEntityNotFoundException e) {
			log.error("Failed to delete a page:\n{}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, VempainMessages.INTERNAL_ERROR);
		}
	}

	// //////////////////// Publishing actions

	@Override
	public ResponseEntity<PublishResponse> publishAll() {
		PublishResponse response;

		try {
			publishService.publishAllPages();
			response = PublishResponse.builder()
									  .result(PublishResultEnum.OK)
									  .message("Successfully published all pages")
									  .timestamp(Instant.now())
									  .build();
		} catch (VempainEntityNotFoundException e) {
			response = PublishResponse.builder()
									  .result(PublishResultEnum.FAIL)
									  .message("Could not find any pages")
									  .timestamp(Instant.now())
									  .build();
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
		}

		return ResponseEntity.ok(response);
	}

	@Override
	public ResponseEntity<PublishResponse> publishPage(Long pageId) {
		PublishResponse response;

		try {
			publishService.publishPage(pageId);
			response = PublishResponse.builder()
									  .result(PublishResultEnum.OK)
									  .message("Successfully published page")
									  .timestamp(Instant.now())
									  .build();
		} catch (VempainEntityNotFoundException e) {
			response = PublishResponse.builder()
									  .result(PublishResultEnum.FAIL)
									  .message("Could not find page")
									  .timestamp(Instant.now())
									  .build();
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
		}

		return ResponseEntity.ok(response);
	}

	@Override
	public ResponseEntity<PublishResponse> deletePublishedPage(long pageId) {
		if (pageId < 1L) {
			log.error(VempainMessages.MALFORMED_ID_IN_REQUEST_MSG, pageId);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_ID_IN_REQUEST);
		}

		pageService.deleteByUser(pageId);

		PublishResponse response = PublishResponse.builder()
												  .result(PublishResultEnum.OK)
												  .message("Successfully deleted page")
												  .timestamp(Instant.now())
												  .build();
		return ResponseEntity.ok(response);
	}

	private void verifyPageRequest(PageRequest pageRequest) {
		if (pageRequest == null ||
			(pageRequest.getPath() == null ||
			 pageRequest.getPath().isBlank()) ||
			(pageRequest.getTitle() == null ||
			 pageRequest.getTitle().isBlank()) ||
			(pageRequest.getHeader() == null ||
			 pageRequest.getHeader().isBlank()) ||
			(pageRequest.getAcls() == null ||
			 pageRequest.getAcls().isEmpty())) {
			log.error("Malformed request when creating new page: {}", pageRequest);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed page creation request");
		}
	}
}
