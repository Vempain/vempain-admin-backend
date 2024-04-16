package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.QueryDetailEnum;
import fi.poltsi.vempain.admin.api.request.PageRequest;
import fi.poltsi.vempain.admin.api.response.AclResponse;
import fi.poltsi.vempain.admin.api.response.PageResponse;
import fi.poltsi.vempain.admin.entity.Page;
import fi.poltsi.vempain.admin.exception.ProcessingFailedException;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.admin.service.AclService;
import fi.poltsi.vempain.admin.service.DeleteService;
import fi.poltsi.vempain.admin.service.PageService;
import fi.poltsi.vempain.admin.service.PublishService;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Slf4j
class PageControllerUTC {
	@Mock
	private PageService    pageService;
	@Mock
	private AclService     aclService;
	@Mock
	private PublishService publishService;
	@Mock
	private DeleteService  deleteService;

	private PageController pageController;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		pageController = new PageController(pageService, publishService, deleteService);
	}

	@Test
	void getPagesOk() {
		var pages = TestUTCTools.generatePageList(5L);
		when(pageService.findAllByUser()).thenReturn(pages);

		var acls = TestUTCTools.generateAclResponses(1L, 1L);
		when(aclService.getAclResponses(anyLong())).thenReturn(acls);

		for (Page page : pages) {
			var pageResponse = page.toResponse();
			pageResponse.setPublished(Instant.now().minus(20, ChronoUnit.MINUTES));
			pageResponse.setAcls(acls);
			when(pageService.populateResponse(page)).thenReturn(pageResponse);
		}

		try {
			ResponseEntity<List<PageResponse>> responseEntity = pageController.getPages(QueryDetailEnum.FULL);
			assertNotNull(responseEntity);
			List<PageResponse> pageResponses = responseEntity.getBody();
			assertNotNull(pageResponses);
			assertEquals(5L, pageResponses.size());

			for (PageResponse pageResponse : pageResponses) {
				assertNotNull(pageResponse);
				assertNotNull(pageResponse.getAcls());
				assertFalse(pageResponse.getAcls().isEmpty());
			}
		} catch (Exception e) {
			fail("Should not have received an exception: " + e);
		}
	}

	@Test
	void getPagesEmptyListOk() {
		when(pageService.findAll()).thenReturn(new ArrayList<>());
		List<AclResponse> acls = TestUTCTools.generateAclResponses(1L, 1L);
		when(aclService.getAclResponses(any())).thenReturn(acls);

		try {
			ResponseEntity<List<PageResponse>> responseEntity = pageController.getPages(QueryDetailEnum.FULL);
			assertNotNull(responseEntity);
			List<PageResponse> pageResponses = responseEntity.getBody();
			assertNotNull(pageResponses);
			assertEquals(0, pageResponses.size());
		} catch (Exception e) {
			fail("Should not have received an exception: " + e);
		}
	}

	@Test
	void addPageOk() {
		var page        = TestUTCTools.generatePage(1L);
		var pageRequest = TestUTCTools.generatePageRequestFromPage(page);
		var acls        = TestUTCTools.generateAclResponses(1L, 1L);

		when(pageService.saveFromPageRequest(pageRequest)).thenReturn(page);
		when(aclService.getAclResponses(page.getAclId())).thenReturn(acls);

		var populatedPageResponse = page.toResponse();
		populatedPageResponse.setPublished(Instant.now().minus(20, ChronoUnit.MINUTES));
		populatedPageResponse.setAcls(acls);
		when(pageService.populateResponse(any())).thenReturn(populatedPageResponse);

		try {
			ResponseEntity<PageResponse> responseEntity = pageController.addPage(pageRequest);
			assertNotNull(responseEntity);
			PageResponse pageResponse = responseEntity.getBody();
			assertNotNull(pageResponse);

			assertEquals(page.getPath(), pageResponse.getPath());
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
	void addPageNullRequestFail() {
		malformedPageRequest(null);
	}

	@Test
	void addPageInvalidPathFail() {
		String[] invalidPaths = {null, "", " ", "\t"};

		for (String invalidPath : invalidPaths) {
			PageRequest pageRequest = TestUTCTools.generatePageRequest(1L);
			pageRequest.setPath(invalidPath);
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
		Page        page        = TestUTCTools.generatePage(1L);
		PageRequest pageRequest = TestUTCTools.generatePageRequestFromPage(page);

		doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, VempainMessages.INVALID_USER_SESSION)).when(pageService).saveFromPageRequest(pageRequest);

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
		Page        page        = TestUTCTools.generatePage(1L);
		PageRequest pageRequest = TestUTCTools.generatePageRequestFromPage(page);

		doThrow(new ResponseStatusException(HttpStatus.CONFLICT, VempainMessages.OBJECT_NAME_ALREADY_EXISTS)).when(pageService).saveFromPageRequest(pageRequest);

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
		var page        = TestUTCTools.generatePage(1L);
		var pageRequest = TestUTCTools.generatePageRequestFromPage(page);
		when(pageService.updateFromRequest(pageRequest)).thenReturn(page);

		var acls = TestUTCTools.generateAclResponses(1L, 1L);

		var populatedPageResponse = page.toResponse();
		populatedPageResponse.setPublished(Instant.now().minus(20, ChronoUnit.MINUTES));
		populatedPageResponse.setAcls(acls);
		when(pageService.populateResponse(any())).thenReturn(populatedPageResponse);

		try {
			var response = pageController.updatePage(pageRequest);
			assertNotNull(response);
			var pageResponse = response.getBody();
			assertNotNull(pageResponse);
			assertNotNull(pageResponse.getAcls());
			assertFalse(pageResponse.getAcls().isEmpty());
		} catch (Exception e) {
			fail("Should not have received an exception: " + e);
		}
	}

	@Test
	void updatePageMalformedIdFail() {
		Page        page        = TestUTCTools.generatePage(1L);
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
	void removePageByIdOk() throws ProcessingFailedException, VempainEntityNotFoundException {
		doNothing().when(pageService).
				   deleteById(1L);

		try {
			pageController.deletePage(1L);
		} catch (Exception e) {
			fail("Should not have received an exception: " + e);
		}
	}

	@Test
	void deletePageMalformedIdFail() {
		Page        page        = TestUTCTools.generatePage(1L);
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
}
