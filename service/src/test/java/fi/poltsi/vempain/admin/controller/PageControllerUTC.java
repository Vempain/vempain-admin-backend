package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.ContentTypeEnum;
import fi.poltsi.vempain.admin.api.PublishResultEnum;
import fi.poltsi.vempain.admin.api.request.PagePagedRequest;
import fi.poltsi.vempain.admin.api.request.PageRequest;
import fi.poltsi.vempain.admin.api.request.PublishRequest;
import fi.poltsi.vempain.admin.api.response.PageResponse;
import fi.poltsi.vempain.admin.api.response.PublishScheduleResponse;
import fi.poltsi.vempain.admin.entity.Page;
import fi.poltsi.vempain.admin.service.DeleteService;
import fi.poltsi.vempain.admin.service.PageService;
import fi.poltsi.vempain.admin.service.PublishService;
import fi.poltsi.vempain.admin.service.ScheduleService;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import fi.poltsi.vempain.auth.api.response.AclResponse;
import fi.poltsi.vempain.auth.api.response.PagedResponse;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.auth.service.AclService;
import fi.poltsi.vempain.common.api.response.TaskAcceptedResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class PageControllerUTC {
	@Mock
	private PageService    pageService;
	@Mock
	private AclService     aclService;
	@Mock
	private PublishService publishService;
	@Mock
	private DeleteService  deleteService;
	@Mock
	private ScheduleService scheduleService;

	@InjectMocks
	private PageController pageController;

	@Test
	void getPagesOk() {
		var pages = TestUTCTools.generatePageList(5L);
		var request = new PagePagedRequest();
		request.setPage(0);
		request.setSize(5);

		var acls = TestUTCTools.generateAclResponses(1L, 1L);

		var pageResponses = new ArrayList<PageResponse>();
		for (Page page : pages) {
			var pageResponse = page.toResponse();
			pageResponse.setPublished(Instant.now()
											 .minus(20, ChronoUnit.MINUTES));
			pageResponse.setAcls(acls);
			pageResponses.add(pageResponse);
		}
		when(pageService.findPagedByUser(request)).thenReturn(PagedResponse.of(pageResponses, 0, 5, 5, 1, true, true));

		try {
			ResponseEntity<PagedResponse<PageResponse>> responseEntity = pageController.getPages(request);
			assertNotNull(responseEntity);
			PagedResponse<PageResponse> pagedResponse = responseEntity.getBody();
			assertNotNull(pagedResponse);
			assertEquals(5L, pagedResponse.getContent()
			                              .size());

			for (PageResponse pageResponse : pagedResponse.getContent()) {
				assertNotNull(pageResponse);
				assertNotNull(pageResponse.getAcls());
				assertFalse(pageResponse.getAcls()
										.isEmpty());
			}
		} catch (Exception e) {
			fail("Should not have received an exception: " + e);
		}
	}

	@Test
	void getPagesEmptyListOk() {
		var request = new PagePagedRequest();
		request.setPage(0);
		request.setSize(5);
		when(pageService.findPagedByUser(request)).thenReturn(PagedResponse.of(new ArrayList<>(), 0, 5, 0, 0, true, true));
		try {
			ResponseEntity<PagedResponse<PageResponse>> responseEntity = pageController.getPages(request);
			assertNotNull(responseEntity);
			PagedResponse<PageResponse> response = responseEntity.getBody();
			assertNotNull(response);
			assertEquals(0, response.getContent()
			                        .size());
		} catch (Exception e) {
			fail("Should not have received an exception: " + e);
		}
	}

	@Test
	void addPageOk() {
		var page = TestUTCTools.generatePage(1L);
		var pageRequest = TestUTCTools.generatePageRequestFromPage(page);
		var acls = TestUTCTools.generateAclResponses(1L, 1L);

		when(pageService.saveFromPageRequest(pageRequest)).thenReturn(page);

		var populatedPageResponse = page.toResponse();
		populatedPageResponse.setPublished(Instant.now()
												  .minus(20, ChronoUnit.MINUTES));
		populatedPageResponse.setAcls(acls);
		when(pageService.populateResponse(any())).thenReturn(populatedPageResponse);

		try {
			ResponseEntity<PageResponse> responseEntity = pageController.addPage(pageRequest);
			assertNotNull(responseEntity);
			PageResponse pageResponse = responseEntity.getBody();
			assertNotNull(pageResponse);

			assertEquals(page.getPagePath(), pageResponse.getPagePath());
			assertEquals(page.getBody(), pageResponse.getBody());
			assertEquals(page.getTitle(), pageResponse.getTitle());
			assertEquals(page.getHeader(), pageResponse.getHeader());

			List<AclResponse> aclResponses = pageResponse.getAcls();
			assertNotNull(aclResponses);
			assertFalse(aclResponses.isEmpty());
			assertEquals(2, aclResponses.size());
		} catch (Exception e) {
			fail("Should not have received an exception: " + e);
		}
	}

	@Test
	void getPageByIdNotFoundReturns404() {
		when(pageService.findByIdByUser(999999L)).thenReturn(null);

		try {
			var response = pageController.getPageById(999999L);
			assertNotNull(response);
			assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
			assertEquals(null, response.getBody());
		} catch (Exception e) {
			fail("Should not have received an exception: " + e);
		}
	}

	@Test
	void addPageNullRequestFail() {
		malformedPageRequest(null);
	}

	@Test
	void addPageInvalidPathFail() {
		String[] invalidPaths = {null, "", " ", "\t"};

		for (String invalidPath : invalidPaths) {
			PageRequest pageRequest = TestUTCTools.generatePageRequest(1L);
			pageRequest.setPagePath(invalidPath);
			malformedPageRequest(pageRequest);
		}
	}

	@Test
	void addPageInvalidTitleFail() {
		String[] invalidTitles = {null, "", " ", "\t"};

		for (String invalidTitle : invalidTitles) {
			PageRequest pageRequest = TestUTCTools.generatePageRequest(1L);
			pageRequest.setTitle(invalidTitle);
			malformedPageRequest(pageRequest);
		}
	}

	@Test
	void addPageInvalidHeaderFail() {
		String[] invalidHeaders = {null, "", " ", "\t"};

		for (String invalidHeader : invalidHeaders) {
			PageRequest pageRequest = TestUTCTools.generatePageRequest(1L);
			pageRequest.setHeader(invalidHeader);
			malformedPageRequest(pageRequest);
		}
	}

	@Test
	void addPageNullAclListFail() {
		PageRequest pageRequest = TestUTCTools.generatePageRequest(1L);
		pageRequest.setAcls(null);
		malformedPageRequest(pageRequest);
	}

	@Test
	void addPageEmptyAclListFail() {
		PageRequest pageRequest = TestUTCTools.generatePageRequest(1L);
		pageRequest.setAcls(new ArrayList<>());
		malformedPageRequest(pageRequest);
	}

	private void malformedPageRequest(PageRequest pageRequest) {
		try {
			pageController.addPage(pageRequest);
			fail("Call with malformed page request should have failed with ResponseStatusException");
		} catch (ResponseStatusException e) {
			assertEquals("400 BAD_REQUEST \"Malformed page creation request\"", e.getMessage());
			assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
		} catch (Exception e) {
			fail("Should have received ResponseStatusException exception: " + e);
		}
	}

	@Test
	void addPageSaveServiceUnauthorizedExceptionFail() {
		Page page = TestUTCTools.generatePage(1L);
		PageRequest pageRequest = TestUTCTools.generatePageRequestFromPage(page);

		doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, VempainMessages.INVALID_USER_SESSION)).when(pageService)
																										   .saveFromPageRequest(pageRequest);

		try {
			pageController.addPage(pageRequest);
			fail("PageService should have thrown a ResponseStatusException");
		} catch (ResponseStatusException e) {
			assertEquals("401 UNAUTHORIZED \"User session is not valid\"", e.getMessage());
			assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
		} catch (Exception e) {
			fail("Should have received ResponseStatusException exception: " + e);
		}
	}

	@Test
	void addPageSaveServiceConflictExceptionFail() {
		Page page = TestUTCTools.generatePage(1L);
		PageRequest pageRequest = TestUTCTools.generatePageRequestFromPage(page);

		doThrow(new ResponseStatusException(HttpStatus.CONFLICT, VempainMessages.OBJECT_NAME_ALREADY_EXISTS)).when(pageService)
																											 .saveFromPageRequest(pageRequest);

		try {
			pageController.addPage(pageRequest);
			fail("PageService should have thrown a ResponseStatusException");
		} catch (ResponseStatusException e) {
			assertEquals("409 CONFLICT \"" + VempainMessages.OBJECT_NAME_ALREADY_EXISTS + "\"", e.getMessage());
			assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
		} catch (Exception e) {
			fail("Should have received ResponseStatusException exception: " + e);
		}
	}

	@Test
	void updatePageOk() {
		var page = TestUTCTools.generatePage(1L);
		var pageRequest = TestUTCTools.generatePageRequestFromPage(page);
		when(pageService.updateFromRequest(pageRequest)).thenReturn(page);

		var acls = TestUTCTools.generateAclResponses(1L, 1L);

		var populatedPageResponse = page.toResponse();
		populatedPageResponse.setPublished(Instant.now()
												  .minus(20, ChronoUnit.MINUTES));
		populatedPageResponse.setAcls(acls);
		when(pageService.populateResponse(any())).thenReturn(populatedPageResponse);

		try {
			var response = pageController.updatePage(pageRequest);
			assertNotNull(response);
			var pageResponse = response.getBody();
			assertNotNull(pageResponse);
			assertNotNull(pageResponse.getAcls());
			assertFalse(pageResponse.getAcls()
									.isEmpty());
		} catch (Exception e) {
			fail("Should not have received an exception: " + e);
		}
	}

	@Test
	void updatePageMalformedIdFail() {
		Page page = TestUTCTools.generatePage(1L);
		PageRequest pageRequest = TestUTCTools.generatePageRequestFromPage(page);
		pageRequest.setId(-1);

		try {
			ResponseEntity<PageResponse> response = pageController.updatePage(pageRequest);
			fail("Should have received an exception when using a malformed ID in update call");
		} catch (ResponseStatusException e) {
			assertEquals("400 BAD_REQUEST \"" + VempainMessages.MALFORMED_ID_IN_REQUEST + "\"", e.getMessage());
			assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exception: " + e);
		}
	}

	@Test
	void removePageByIdOk() {
		try {
			pageController.deletePage(1L);
		} catch (Exception e) {
			fail("Should not have received an exception: " + e);
		}
	}

	@Test
	void deletePageMalformedIdFail() {
		Page page = TestUTCTools.generatePage(1L);
		PageRequest pageRequest = TestUTCTools.generatePageRequestFromPage(page);

		try {
			pageController.deletePage(-1);
			fail("Should have received an exception when using a malformed ID in delete call");
		} catch (ResponseStatusException e) {
			assertEquals("400 BAD_REQUEST \"" + VempainMessages.MALFORMED_ID_IN_REQUEST + "\"", e.getMessage());
			assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exception: " + e);
		}
	}

	@Test
	void suggestPagePathDelegatesToTheService() {
		var suggestion = fi.poltsi.vempain.admin.api.response.PagePathSuggestionResponse.builder()
																						.prefix("so")
																						.suggestion("some/path")
																						.matches(2)
																						.build();
		when(pageService.suggestPath("so")).thenReturn(suggestion);

		var response = pageController.suggestPagePath("so");

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(suggestion, response.getBody());
	}

	// ---- lookups by form and id, deletion failures, publishing and scheduling ----

	@Test
	void getPagesByFormIdPopulatesEveryReadablePage() {
		var pages = TestUTCTools.generatePageList(2L);
		when(pageService.findAllByFormIdForUser(5L)).thenReturn(pages);
		when(pageService.populateResponse(any(Page.class))).thenAnswer(invocation -> invocation.getArgument(0, Page.class)
																							   .toResponse());

		var response = pageController.getPagesByFormId(5L);

		assertEquals(2, response.getBody()
								.size());
	}

	@Test
	void getPageByIdAnswersThePopulatedPage() {
		var page = TestUTCTools.generatePage(1L);
		when(pageService.findByIdByUser(1L)).thenReturn(page);
		when(pageService.populateResponse(page)).thenReturn(page.toResponse());

		var response = pageController.getPageById(1L);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(1L, response.getBody()
								 .getId());
	}

	@Test
	void deletePageReportsAMissingPageAsAnInternalError() throws Exception {
		doThrow(new VempainEntityNotFoundException("Page not found by id: 9", "page")).when(deleteService)
																					  .deletePageById(9L);

		var thrown = assertThrows(ResponseStatusException.class, () -> pageController.deletePage(9L));

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, thrown.getStatusCode());
	}

	@Test
	void publishAllWithADateSchedulesOnlyThePagesTheUserMayPublish() {
		var allowed = TestUTCTools.generatePage(1L);
		var denied = TestUTCTools.generatePage(2L);
		var publishDate = Instant.now()
								 .plus(1, ChronoUnit.DAYS);
		when(pageService.findAllByUser()).thenReturn(List.of(allowed, denied));
		when(publishService.canPublishPage(allowed)).thenReturn(true);
		when(publishService.canPublishPage(denied)).thenReturn(false);
		when(scheduleService.schedulePublish(publishDate, 1L, ContentTypeEnum.PAGE, "Publish all pages")).thenReturn(PublishScheduleResponse.builder()
																																			.id(1L)
																																			.build());

		var response = pageController.publishAll(publishDate);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(PublishResultEnum.OK, response.getBody()
												   .getResult());
		assertNull(response.getBody()
						   .getTask());
		verify(scheduleService, never()).schedulePublish(any(), eq(2L), any(), any());
	}

	@Test
	void publishAllWithoutADateStartsATask() {
		var task = TaskAcceptedResponse.builder()
									   .taskId("t-1")
									   .build();
		when(publishService.publishAllPagesAsTask()).thenReturn(task);

		var response = pageController.publishAll(null);

		assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
		assertEquals(task, response.getBody()
								   .getTask());
	}

	@Test
	void publishPageRejectsAMissingOrMalformedRequest() {
		assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class, () -> pageController.publishPage(null)).getStatusCode());
		assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class, () -> pageController.publishPage(PublishRequest.builder()
																																		.id(0L)
																																		.build())).getStatusCode());
	}

	@Test
	void publishPageStartsATaskOrAnswersNotFound() throws Exception {
		var task = TaskAcceptedResponse.builder()
									   .taskId("t-2")
									   .build();
		when(publishService.publishPageAsTask(1L)).thenReturn(task);
		var accepted = pageController.publishPage(PublishRequest.builder()
																.id(1L)
																.build());
		assertEquals(HttpStatus.ACCEPTED, accepted.getStatusCode());
		assertEquals(task, accepted.getBody()
								   .getTask());

		when(publishService.publishPageAsTask(2L)).thenThrow(new VempainEntityNotFoundException("Page not found by id: 2", "page"));
		var notFound = pageController.publishPage(PublishRequest.builder()
																.id(2L)
																.build());
		assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());
		assertEquals(PublishResultEnum.FAIL, notFound.getBody()
													 .getResult());
	}

	@Test
	void publishPageWithAScheduleVerifiesThePrivilegesAndStoresTheSchedule() throws Exception {
		var publishDate = Instant.now()
								 .plus(1, ChronoUnit.DAYS);
		var request = PublishRequest.builder()
									.id(1L)
									.publishSchedule(true)
									.publishDateTime(publishDate)
									.publishMessage("Later")
									.build();
		when(scheduleService.schedulePublish(publishDate, 1L, ContentTypeEnum.PAGE, "Later")).thenReturn(PublishScheduleResponse.builder()
																																.id(5L)
																																.build());

		var scheduled = pageController.publishPage(request);

		assertEquals(HttpStatus.OK, scheduled.getStatusCode());
		assertEquals("Successfully scheduled page for publishing", scheduled.getBody()
																			.getMessage());
		verify(publishService).authorizePagePublish(1L);
		verify(publishService, never()).publishPageAsTask(1L);

		when(scheduleService.schedulePublish(publishDate, 1L, ContentTypeEnum.PAGE, "Later")).thenReturn(null);
		var failed = pageController.publishPage(request);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, failed.getStatusCode());
		assertEquals(PublishResultEnum.FAIL, failed.getBody()
												   .getResult());
	}

	@Test
	void deletePublishedPageRemovesTheSitePageOrRejectsAMalformedId() {
		assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class, () -> pageController.deletePublishedPage(0L)).getStatusCode());

		var response = pageController.deletePublishedPage(3L);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(PublishResultEnum.OK, response.getBody()
												   .getResult());
		verify(pageService).deleteByUser(3L);
	}
}
